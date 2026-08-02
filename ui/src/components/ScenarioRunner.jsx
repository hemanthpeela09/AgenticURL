import { useEffect, useState } from 'react';
import {
    Box, Button, Card, CardContent, Typography, Alert, Stack, MenuItem, TextField,
    Chip, Accordion, AccordionSummary, AccordionDetails, Table, TableBody, TableCell,
    TableHead, TableRow, Switch, FormControlLabel, LinearProgress, Paper, Divider,
    IconButton, Collapse, Tabs, Tab, Checkbox, Badge, Dialog, DialogTitle, DialogContent, DialogActions,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import PsychologyIcon from '@mui/icons-material/Psychology';
import BuildIcon from '@mui/icons-material/Build';
import RefreshIcon from '@mui/icons-material/Refresh';
import HourglassEmptyIcon from '@mui/icons-material/HourglassEmpty';
import ThumbUpIcon from '@mui/icons-material/ThumbUp';
import ThumbDownIcon from '@mui/icons-material/ThumbDown';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import ListAltIcon from '@mui/icons-material/ListAlt';
import DeleteIcon from '@mui/icons-material/Delete';
import { useTranslation } from 'react-i18next';
import { listScenarios, runScenario, getLlmInfo, runSdlcChain, runSdlcAgent, shortenWithAi } from '../api.js';
import DagView from './DagView.jsx';
import MetricsPanel from './MetricsPanel.jsx';
import LinkIcon from '@mui/icons-material/Link';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';

const KIND_COLOR = {
    policy: 'secondary', approval: 'warning', error: 'error', rollback: 'warning',
    safe_stop: 'error', replan: 'info', clarification: 'info', synchronize: 'success',
    parallel: 'info', artifact: 'default',
};

// Tab panel component
function TabPanel({ children, value, index, ...other }) {
    return (
        <div role="tabpanel" hidden={value !== index} {...other}>
            {value === index && <Box sx={{ pt: 2 }}>{children}</Box>}
        </div>
    );
}

export default function ScenarioRunner({ pendingRequests, setPendingRequests }) {
    const { t } = useTranslation();
    const [scenarios, setScenarios] = useState([]);
    const [scenario, setScenario] = useState('greenfield');
    const [approval, setApproval] = useState('auto');
    const [result, setResult] = useState(null);
    const [error, setError] = useState(null);
    const [busy, setBusy] = useState(false);

    // AI/LangChain state
    const [aiMode, setAiMode] = useState(false);
    const [llmInfo, setLlmInfo] = useState(null);
    const [customRequirement, setCustomRequirement] = useState('');
    const [chainResult, setChainResult] = useState(null);
    const [agentResult, setAgentResult] = useState(null);
    const [tabValue, setTabValue] = useState(0);

    // URL Shortening AI state
    const [shortenResult, setShortenResult] = useState(null);

    // Approval workflow state - pendingRequests is now passed as prop from App
    // Selected requests for batch approval
    const [selectedRequests, setSelectedRequests] = useState([]);
    // Show pending requests dialog
    const [showPendingDialog, setShowPendingDialog] = useState(false);
    // approvalStatus: null | 'approved' | 'denied'
    const [approvalStatus, setApprovalStatus] = useState(null);
    // Processing results for batch operations
    const [processingResults, setProcessingResults] = useState([]);

    useEffect(() => {
        listScenarios().then(setScenarios).catch((e) => setError(e.message));
        // Fetch LLM info on mount
        getLlmInfo().then(setLlmInfo).catch(() => setLlmInfo({ backend: 'unavailable', supportsStreaming: false }));
    }, []);

    const refreshLlmInfo = async () => {
        try {
            const info = await getLlmInfo();
            setLlmInfo(info);
        } catch (e) {
            setLlmInfo({ backend: 'unavailable', supportsStreaming: false });
        }
    };

    const run = async () => {
        setError(null);
        setResult(null);
        setChainResult(null);
        setAgentResult(null);
        setShortenResult(null);
        setBusy(true);

        // Normal scenario run (non-AI mode)
        try {
            setResult(await runScenario(scenario, approval));
        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    };

    // Process selected pending requests (batch approval/denial)
    const processSelectedRequests = async (action) => {
        const results = [];
        setProcessingResults([]);

        if (action === 'approve') {
            // Approve and execute each selected request
            setApprovalStatus('approved');
            setBusy(true);
            for (const requestId of selectedRequests) {
                const request = pendingRequests.find(r => r.id === requestId);
                if (!request) continue;

                try {
                    await executeRequest(request);
                    const shortId = requestId.substring(0, 12);
                    results.push({ id: requestId, status: 'success', message: `[${shortId}] ${request.type} executed successfully` });
                } catch (err) {
                    const shortId = requestId.substring(0, 12);
                    results.push({ id: requestId, status: 'error', message: `[${shortId}] ${err.message}` });
                }
            }
            setBusy(false);
        } else if (action === 'deny') {
            // Deny - just remove from queue, don't call any API, show denial in result section
            setApprovalStatus('denied');
            for (const requestId of selectedRequests) {
                const request = pendingRequests.find(r => r.id === requestId);
                if (!request) continue;
                const shortId = requestId.substring(0, 12);

                // Update the corresponding result state to show denial
                if (request.type === 'shorten') {
                    setShortenResult({
                        success: false,
                        error: `Request [${shortId}] denied by user (safe-stop). Original prompt: "${request.instruction}"`,
                        denied: true,
                        requestId: shortId,
                        instruction: request.instruction,
                    });
                } else if (request.type === 'chain') {
                    setChainResult({
                        denied: true,
                        error: `Request [${shortId}] denied by user (safe-stop). Original requirement: "${request.instruction}"`,
                        requestId: shortId,
                        instruction: request.instruction,
                    });
                } else if (request.type === 'agent') {
                    setAgentResult({
                        success: false,
                        denied: true,
                        error: `Request [${shortId}] denied by user (safe-stop). Original input: "${request.instruction}"`,
                        requestId: shortId,
                        instruction: request.instruction,
                    });
                }

                results.push({ id: requestId, status: 'denied', message: `[${shortId}] ${request.type} request denied - removed from queue` });
            }
        }

        // Remove processed requests from pending queue
        setPendingRequests(prev => prev.filter(r => !selectedRequests.includes(r.id)));
        setSelectedRequests([]);
        setProcessingResults(results);
    };

    // Execute a single request
    const executeRequest = async (request) => {
        const { type, instruction } = request;

        if (type === 'chain') {
            const result = await runSdlcChain(instruction);
            setChainResult(result);
        } else if (type === 'agent') {
            const result = await runSdlcAgent(instruction);
            setAgentResult(result);
        } else if (type === 'shorten') {
            const result = await shortenWithAi(instruction);
            setShortenResult(result);
        }
    };

    // Generate unique ID for requests
    const generateId = () => `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

    // Submit Chain request for approval
    const submitChainForApproval = () => {
        const requirement = customRequirement || scenarios.find((s) => s.key === scenario)?.requirement || '';
        if (!requirement) {
            setError('Please enter a requirement or select a scenario');
            return;
        }

        setError(null);
        setApprovalStatus(null);
        setPendingRequests(prev => [...prev, {
            id: generateId(),
            type: 'chain',
            scenario: scenario,
            instruction: requirement,
            timestamp: new Date(),
        }]);
        setCustomRequirement('');
    };

    // Submit Agent request for approval
    const submitAgentForApproval = () => {
        const input = customRequirement || scenarios.find((s) => s.key === scenario)?.requirement || '';
        if (!input) {
            setError('Please enter a requirement or select a scenario');
            return;
        }

        setError(null);
        setApprovalStatus(null);
        setPendingRequests(prev => [...prev, {
            id: generateId(),
            type: 'agent',
            scenario: scenario,
            instruction: input,
            timestamp: new Date(),
        }]);
        setCustomRequirement('');
    };

    // Submit URL Shortening request for approval
    const submitShortenForApproval = () => {
        const prompt = customRequirement;
        if (!prompt || !prompt.trim()) {
            setError('Please enter a prompt describing the URL to shorten (e.g., "Shorten https://example.com with alias mylink")');
            return;
        }

        setError(null);
        setApprovalStatus(null);
        setPendingRequests(prev => [...prev, {
            id: generateId(),
            type: 'shorten',
            scenario: scenario,
            instruction: prompt,
            timestamp: new Date(),
        }]);
        setCustomRequirement('');
    };

    // Remove a pending request
    const removePendingRequest = (requestId) => {
        setPendingRequests(prev => prev.filter(r => r.id !== requestId));
        setSelectedRequests(prev => prev.filter(id => id !== requestId));
    };

    // Toggle selection of a request
    const toggleRequestSelection = (requestId) => {
        setSelectedRequests(prev =>
            prev.includes(requestId)
                ? prev.filter(id => id !== requestId)
                : [...prev, requestId]
        );
    };

    // Select/deselect all requests
    const toggleSelectAll = () => {
        if (selectedRequests.length === pendingRequests.length) {
            setSelectedRequests([]);
        } else {
            setSelectedRequests(pendingRequests.map(r => r.id));
        }
    };

    // Filter pending requests by scenario
    const getPendingRequestsByScenario = (scenarioKey) => {
        return pendingRequests.filter(r => r.scenario === scenarioKey);
    };

    // Get icon for request type
    const getRequestTypeIcon = (type) => {
        switch (type) {
            case 'chain': return <AccountTreeIcon fontSize="small" />;
            case 'agent': return <SmartToyIcon fontSize="small" />;
            case 'shorten': return <LinkIcon fontSize="small" />;
            default: return null;
        }
    };

    // Example prompts for URL shortening
    const urlShortenExamples = [
        "Shorten https://github.com/example/repo with alias gh-repo",
        "Create a short URL for https://docs.google.com/document/d/123456",
        "Shorten https://example.com/campaign?utm_source=newsletter and call it newsletter",
    ];

    return (
        <Stack spacing={2}>
            <Card>
                <CardContent>
                    <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
                        <Typography variant="h6">{t('orch.title')}</Typography>
                        <Stack direction="row" spacing={1} alignItems="center">
                            {llmInfo && (
                                <Chip
                                    icon={<SmartToyIcon />}
                                    label={`LLM: ${llmInfo.backend}`}
                                    color={llmInfo.backend === 'mock' ? 'default' : 'primary'}
                                    size="small"
                                    variant="outlined"
                                />
                            )}
                            <IconButton size="small" onClick={refreshLlmInfo} title="Refresh LLM Info">
                                <RefreshIcon fontSize="small" />
                            </IconButton>
                            <FormControlLabel
                                control={
                                    <Switch
                                        checked={aiMode}
                                        onChange={(e) => setAiMode(e.target.checked)}
                                        color="primary"
                                    />
                                }
                                label={
                                    <Stack direction="row" spacing={0.5} alignItems="center">
                                        <PsychologyIcon fontSize="small" />
                                        <Typography variant="body2">AI Agent Mode</Typography>
                                    </Stack>
                                }
                            />
                        </Stack>
                    </Stack>

                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="flex-start">
                        <TextField select label={t('orch.scenario')} size="small" value={scenario}
                                   onChange={(e) => setScenario(e.target.value)} sx={{ minWidth: 220 }}>
                            {scenarios.map((s) => (
                                <MenuItem key={s.key} value={s.key}>{s.key}</MenuItem>
                            ))}
                        </TextField>

                        {/* Approval dropdown - always shown when there are pending requests or in non-AI mode */}
                        <TextField select label={t('orch.approval')} size="small" value={approval}
                                   onChange={(e) => setApproval(e.target.value)} sx={{ minWidth: 160 }}>
                            <MenuItem value="auto">{t('orch.approvalAuto')}</MenuItem>
                            <MenuItem value="deny">{t('orch.approvalDeny')}</MenuItem>
                        </TextField>

                        {/* Run Scenario button */}
                        <Button
                            variant="contained"
                            onClick={run}
                            disabled={busy}
                            startIcon={selectedRequests.length > 0 ? <PlayArrowIcon /> : <AccountTreeIcon />}
                            color={selectedRequests.length > 0 ? (approval === 'auto' ? 'success' : 'error') : 'primary'}
                        >
                            {busy ? t('orch.running') : (
                                selectedRequests.length > 0
                                    ? (approval === 'auto' ? `Approve (${selectedRequests.length})` : `Deny (${selectedRequests.length})`)
                                    : t('orch.run')
                            )}
                        </Button>

                        {/* Pending Requests button - always visible when there are pending requests */}
                        <Badge badgeContent={pendingRequests.length} color="warning">
                            <Button
                                variant="outlined"
                                onClick={() => setShowPendingDialog(true)}
                                startIcon={<ListAltIcon />}
                                color="warning"
                                disabled={pendingRequests.length === 0}
                            >
                                Pending Requests
                            </Button>
                        </Badge>
                    </Stack>

                    {/* AI Mode buttons for submitting requests - show specific button based on scenario */}
                    {aiMode && (
                        <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ mt: 2 }} alignItems="center">
                            {scenario === 'ambiguous' && (
                                <>
                                    <Button
                                        variant="contained"
                                        onClick={submitChainForApproval}
                                        disabled={busy}
                                        startIcon={<AccountTreeIcon />}
                                        color="primary"
                                        size="small"
                                    >
                                        Submit Chain
                                    </Button>
                                    <Chip label="Experimental" size="small" color="warning" variant="outlined" />
                                </>
                            )}
                            {scenario === 'brownfield' && (
                                <>
                                    <Button
                                        variant="contained"
                                        onClick={submitAgentForApproval}
                                        disabled={busy}
                                        startIcon={<SmartToyIcon />}
                                        color="secondary"
                                        size="small"
                                    >
                                        Submit Agent
                                    </Button>
                                    <Chip label="Beta" size="small" color="info" variant="outlined" />
                                </>
                            )}
                            {scenario === 'greenfield' && (
                                <>
                                    <Button
                                        variant="contained"
                                        onClick={submitShortenForApproval}
                                        disabled={busy}
                                        startIcon={<LinkIcon />}
                                        color="success"
                                        size="small"
                                    >
                                        Submit URL Shorten
                                    </Button>
                                    <Chip label="Stable" size="small" color="success" variant="outlined" />
                                </>
                            )}
                        </Stack>
                    )}

                    {/* Custom requirement input for AI mode */}
                    {aiMode && (
                        <Box sx={{ mt: 2 }}>
                            <TextField
                                fullWidth
                                multiline
                                rows={2}
                                label={
                                    scenario === 'greenfield' ? 'URL Shortening Prompt' :
                                        scenario === 'brownfield' ? 'Agent Input' :
                                            'Chain Requirement'
                                }
                                placeholder={
                                    scenario === 'greenfield' ? "Enter a URL shortening request like 'Shorten https://example.com with alias mylink'" :
                                        scenario === 'brownfield' ? 'Enter an input for the AI agent to process' :
                                            'Enter a requirement for the SDLC chain'
                                }
                                value={customRequirement}
                                onChange={(e) => setCustomRequirement(e.target.value)}
                                variant="outlined"
                            />

                            {scenario === 'greenfield' && (
                                <>
                                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1, mb: 1 }}>
                                        URL Shortening Examples (click to use):
                                    </Typography>
                                    <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ gap: 1 }}>
                                        {urlShortenExamples.map((example, idx) => (
                                            <Chip
                                                key={idx}
                                                label={example.length > 45 ? example.substring(0, 45) + '...' : example}
                                                size="small"
                                                onClick={() => setCustomRequirement(example)}
                                                sx={{ cursor: 'pointer' }}
                                                variant="outlined"
                                                color="success"
                                                icon={<LinkIcon />}
                                            />
                                        ))}
                                    </Stack>
                                </>
                            )}
                        </Box>
                    )}

                    {scenarios.find((s) => s.key === scenario) && !customRequirement && (
                        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                            "{scenarios.find((s) => s.key === scenario).requirement}"
                        </Typography>
                    )}

                    {busy && <LinearProgress sx={{ mt: 2 }} />}
                    {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
                </CardContent>
            </Card>

            {/* Traditional Orchestration Result */}
            {result && !aiMode && (
                <>
                    <Stack direction="row" spacing={1} alignItems="center">
                        <Chip color={result.completed ? 'success' : 'error'}
                              label={result.completed ? t('orch.completed') : t('orch.safeStopped')} />
                        <Typography variant="body2" color="text.secondary">{t('orch.runId', { id: result.runId })}</Typography>
                    </Stack>

                    {result.safeStopReason && (
                        <Alert severity="error">
                            {t('orch.safeStopReason')} {result.safeStopReason}
                        </Alert>
                    )}

                    <DagView nodeStatus={result.nodeStatus} />

                    {result.openQuestions?.length > 0 && (
                        <Alert severity="warning">
                            {t('orch.openQuestions')} {result.openQuestions.join(' · ')}
                        </Alert>
                    )}

                    <MetricsPanel metrics={result.metrics} />

                    <Accordion>
                        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                            <Typography>{t('orch.lineage', { count: result.lineage?.length || 0 })}</Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <Table size="small">
                                <TableHead>
                                    <TableRow>
                                        <TableCell>#</TableCell>
                                        <TableCell>{t('orch.node')}</TableCell>
                                        <TableCell>{t('orch.kind')}</TableCell>
                                        <TableCell>{t('orch.detail')}</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {(result.lineage || []).map((e) => (
                                        <TableRow key={e.seq}>
                                            <TableCell>{e.seq}</TableCell>
                                            <TableCell>{e.node}</TableCell>
                                            <TableCell>
                                                <Chip size="small" label={e.kind}
                                                      color={KIND_COLOR[e.kind] || 'default'} />
                                            </TableCell>
                                            <TableCell>{e.detail}</TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </AccordionDetails>
                    </Accordion>
                </>
            )}

            {/* AI Chain Result */}
            {chainResult && aiMode && (
                <Card>
                    <CardContent>
                        <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
                            <AccountTreeIcon color={chainResult.denied ? 'warning' : 'primary'} />
                            <Typography variant="h6">LangChain SDLC Result</Typography>
                            <Chip
                                label={chainResult.denied ? 'Denied' : 'Chain Execution'}
                                color={chainResult.denied ? 'warning' : 'primary'}
                                size="small"
                            />
                        </Stack>

                        {chainResult.denied ? (
                            <Paper sx={{ p: 2, bgcolor: 'warning.light' }}>
                                <Stack spacing={2}>
                                    <Stack direction="row" spacing={1} alignItems="center">
                                        <ThumbDownIcon color="warning" />
                                        <Typography variant="subtitle1" fontWeight="bold" color="warning.dark">
                                            🚫 Request Denied (Safe-Stop)
                                        </Typography>
                                    </Stack>
                                    <Divider />
                                    <Box>
                                        <Typography variant="body2" color="text.secondary">Request ID:</Typography>
                                        <Typography variant="body1" fontFamily="monospace">{chainResult.requestId}</Typography>
                                    </Box>
                                    <Box>
                                        <Typography variant="body2" color="text.secondary">Original Requirement:</Typography>
                                        <Typography variant="body1" sx={{ fontStyle: 'italic' }}>
                                            "{chainResult.instruction}"
                                        </Typography>
                                    </Box>
                                    <Alert severity="warning" sx={{ mt: 1 }}>
                                        This chain request was denied by the user and was not executed. No LLM call was made.
                                    </Alert>
                                </Stack>
                            </Paper>
                        ) : (
                            <>
                                <Tabs value={tabValue} onChange={(e, v) => setTabValue(v)} sx={{ mb: 2 }}>
                                    <Tab label="Requirements" />
                                    <Tab label="Design" />
                                    <Tab label="Implementation" />
                                    <Tab label="Tests" />
                                </Tabs>

                                <TabPanel value={tabValue} index={0}>
                                    <Paper sx={{ p: 2, bgcolor: 'grey.50' }}>
                                        <Typography variant="subtitle2" color="primary" gutterBottom>Requirement Analysis</Typography>
                                        <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                                            {chainResult.requirement_spec || chainResult.requirement || 'No requirement analysis generated'}
                                        </Typography>
                                    </Paper>
                                </TabPanel>

                                <TabPanel value={tabValue} index={1}>
                                    <Paper sx={{ p: 2, bgcolor: 'grey.50' }}>
                                        <Typography variant="subtitle2" color="primary" gutterBottom>Design Specification</Typography>
                                        <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                                            {chainResult.design_spec || 'No design specification generated'}
                                        </Typography>
                                    </Paper>
                                </TabPanel>

                                <TabPanel value={tabValue} index={2}>
                                    <Paper sx={{ p: 2, bgcolor: 'grey.50' }}>
                                        <Typography variant="subtitle2" color="primary" gutterBottom>Code Manifest</Typography>
                                        <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                                            {chainResult.code_manifest || 'No code manifest generated'}
                                        </Typography>
                                    </Paper>
                                </TabPanel>

                                <TabPanel value={tabValue} index={3}>
                                    <Paper sx={{ p: 2, bgcolor: 'grey.50' }}>
                                        <Typography variant="subtitle2" color="primary" gutterBottom>Test Plan</Typography>
                                        <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                                            {chainResult.test_plan || 'No test plan generated'}
                                        </Typography>
                                    </Paper>
                                </TabPanel>

                                {/* Show DAG view for chain execution */}
                                <Box sx={{ mt: 2 }}>
                                    <DagView
                                        nodeStatus={{
                                            requirements: chainResult.requirement_spec ? 'SUCCEEDED' : 'PENDING',
                                            design: chainResult.design_spec ? 'SUCCEEDED' : 'PENDING',
                                            implementation: chainResult.code_manifest ? 'SUCCEEDED' : 'PENDING',
                                            test_authoring: chainResult.test_plan ? 'SUCCEEDED' : 'PENDING',
                                            testing: 'PENDING',
                                            documentation: 'PENDING',
                                            release: 'PENDING',
                                        }}
                                        aiMode={true}
                                    />
                                </Box>
                            </>
                        )}
                    </CardContent>
                </Card>
            )}

            {/* AI Agent Result */}
            {agentResult && aiMode && (
                <Card>
                    <CardContent>
                        <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
                            <SmartToyIcon color={agentResult.denied ? 'warning' : 'secondary'} />
                            <Typography variant="h6">AI Agent Result</Typography>
                            <Chip
                                label={agentResult.denied ? 'Denied' : agentResult.success ? 'Success' : 'Incomplete'}
                                color={agentResult.denied ? 'warning' : agentResult.success ? 'success' : 'warning'}
                                size="small"
                            />
                        </Stack>

                        {agentResult.denied ? (
                            <Paper sx={{ p: 2, bgcolor: 'warning.light' }}>
                                <Stack spacing={2}>
                                    <Stack direction="row" spacing={1} alignItems="center">
                                        <ThumbDownIcon color="warning" />
                                        <Typography variant="subtitle1" fontWeight="bold" color="warning.dark">
                                            🚫 Request Denied (Safe-Stop)
                                        </Typography>
                                    </Stack>
                                    <Divider />
                                    <Box>
                                        <Typography variant="body2" color="text.secondary">Request ID:</Typography>
                                        <Typography variant="body1" fontFamily="monospace">{agentResult.requestId}</Typography>
                                    </Box>
                                    <Box>
                                        <Typography variant="body2" color="text.secondary">Original Input:</Typography>
                                        <Typography variant="body1" sx={{ fontStyle: 'italic' }}>
                                            "{agentResult.instruction}"
                                        </Typography>
                                    </Box>
                                    <Alert severity="warning" sx={{ mt: 1 }}>
                                        This agent request was denied by the user and was not executed. No LLM call was made.
                                    </Alert>
                                </Stack>
                            </Paper>
                        ) : (
                            <>
                                {/* Agent Output */}
                                <Paper sx={{ p: 2, bgcolor: 'grey.50', mb: 2 }}>
                                    <Typography variant="subtitle2" color="primary" gutterBottom>Final Output</Typography>
                                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                                        {agentResult.output || 'No output generated'}
                                    </Typography>
                                </Paper>

                                {/* Agent Steps (ReAct trace) */}
                                {agentResult.steps && agentResult.steps.length > 0 && (
                                    <Accordion defaultExpanded>
                                        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                                            <Stack direction="row" spacing={1} alignItems="center">
                                                <PsychologyIcon fontSize="small" />
                                                <Typography>Agent Reasoning Steps ({agentResult.steps.length})</Typography>
                                            </Stack>
                                        </AccordionSummary>
                                        <AccordionDetails>
                                            <Stack spacing={2}>
                                                {agentResult.steps.map((step, idx) => (
                                                    <Paper key={idx} sx={{ p: 2, border: '1px solid', borderColor: 'divider' }}>
                                                        <Stack spacing={1}>
                                                            <Stack direction="row" spacing={1} alignItems="center">
                                                                <Chip label={`Step ${idx + 1}`} size="small" color="primary" />
                                                                {step.action && (
                                                                    <Chip
                                                                        icon={<BuildIcon />}
                                                                        label={step.action}
                                                                        size="small"
                                                                        color="secondary"
                                                                        variant="outlined"
                                                                    />
                                                                )}
                                                            </Stack>

                                                            {step.thought && (
                                                                <Box>
                                                                    <Typography variant="caption" color="text.secondary">Thought:</Typography>
                                                                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', pl: 1 }}>
                                                                        {step.thought}
                                                                    </Typography>
                                                                </Box>
                                                            )}

                                                            {step.actionInput && (
                                                                <Box>
                                                                    <Typography variant="caption" color="text.secondary">Action Input:</Typography>
                                                                    <Paper sx={{ p: 1, bgcolor: 'action.hover', ml: 1 }}>
                                                                        <Typography variant="body2" fontFamily="monospace">
                                                                            {step.actionInput}
                                                                        </Typography>
                                                                    </Paper>
                                                                </Box>
                                                            )}

                                                            {step.observation && (
                                                                <Box>
                                                                    <Typography variant="caption" color="text.secondary">Observation:</Typography>
                                                                    <Paper sx={{ p: 1, bgcolor: 'success.light', ml: 1 }}>
                                                                        <Typography variant="body2">
                                                                            {step.observation}
                                                                        </Typography>
                                                                    </Paper>
                                                                </Box>
                                                            )}
                                                        </Stack>
                                                    </Paper>
                                                ))}
                                            </Stack>
                                        </AccordionDetails>
                                    </Accordion>
                                )}
                            </>
                        )}
                    </CardContent>
                </Card>
            )}

            {/* URL Shortening Result */}
            {shortenResult && aiMode && (
                <Card>
                    <CardContent>
                        <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
                            <LinkIcon color={shortenResult.success ? 'success' : shortenResult.denied ? 'warning' : 'error'} />
                            <Typography variant="h6">AI URL Shortening Result</Typography>
                            <Chip
                                label={shortenResult.success ? 'Success' : shortenResult.denied ? 'Denied' : 'Failed'}
                                color={shortenResult.success ? 'success' : shortenResult.denied ? 'warning' : 'error'}
                                size="small"
                            />
                        </Stack>

                        {shortenResult.success ? (
                            <Paper sx={{ p: 2, bgcolor: 'grey.light' }}>
                                <Stack spacing={2}>
                                    <Stack direction="row" spacing={1} alignItems="center">
                                        <CheckCircleIcon color="success" />
                                        <Typography variant="subtitle1" fontWeight="bold">
                                            URL Shortened Successfully!
                                        </Typography>
                                    </Stack>
                                    <Divider />
                                    <Box>
                                        <Typography variant="body2" color="text.secondary">Short URL:</Typography>
                                        <Typography variant="h6" color="primary">
                                            <a href={shortenResult.shortUrl} target="_blank" rel="noreferrer" style={{ color: 'inherit' }}>
                                                {shortenResult.shortUrl}
                                            </a>
                                        </Typography>
                                    </Box>
                                    <Stack direction="row" spacing={4} flexWrap="wrap">
                                        <Box>
                                            <Typography variant="caption" color="text.secondary">Code</Typography>
                                            <Typography variant="body1" fontWeight="bold">{shortenResult.code}</Typography>
                                        </Box>
                                        {shortenResult.customAlias && (
                                            <Box>
                                                <Typography variant="caption" color="text.secondary">Custom Alias</Typography>
                                                <Typography variant="body1" fontWeight="bold">{shortenResult.customAlias}</Typography>
                                            </Box>
                                        )}
                                        {shortenResult.ttlSeconds && (
                                            <Box>
                                                <Typography variant="caption" color="text.secondary">Expires In</Typography>
                                                <Typography variant="body1" fontWeight="bold">{shortenResult.ttlSeconds}s</Typography>
                                            </Box>
                                        )}
                                    </Stack>
                                    <Box>
                                        <Typography variant="caption" color="text.secondary">Original URL</Typography>
                                        <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>
                                            {shortenResult.originalUrl}
                                        </Typography>
                                    </Box>
                                </Stack>
                            </Paper>
                        ) : shortenResult.denied ? (
                            <Paper sx={{ p: 2, bgcolor: 'warning.light' }}>
                                <Stack spacing={2}>
                                    <Stack direction="row" spacing={1} alignItems="center">
                                        <ThumbDownIcon color="warning" />
                                        <Typography variant="subtitle1" fontWeight="bold" color="warning.dark">
                                            🚫 Request Denied (Safe-Stop)
                                        </Typography>
                                    </Stack>
                                    <Divider />
                                    <Box>
                                        <Typography variant="body2" color="text.secondary">Request ID:</Typography>
                                        <Typography variant="body1" fontFamily="monospace">{shortenResult.requestId}</Typography>
                                    </Box>
                                    <Box>
                                        <Typography variant="body2" color="text.secondary">Original Prompt:</Typography>
                                        <Typography variant="body1" sx={{ fontStyle: 'italic' }}>
                                            "{shortenResult.instruction}"
                                        </Typography>
                                    </Box>
                                    <Alert severity="warning" sx={{ mt: 1 }}>
                                        This request was denied by the user and was not executed. No API call was made.
                                    </Alert>
                                </Stack>
                            </Paper>
                        ) : (
                            <Paper sx={{ p: 2, bgcolor: 'yellow.light' }}>
                                <Stack spacing={1}>
                                    <Typography variant="subtitle1" fontWeight="bold" color="error">
                                        ❌ Could not process request
                                    </Typography>
                                    <Typography variant="body2">
                                        {shortenResult.error}
                                    </Typography>
                                </Stack>
                            </Paper>
                        )}

                        {/* Show processing flow */}
                        <Box sx={{ mt: 2 }}>
                            <Typography variant="subtitle2" gutterBottom>AI Processing Flow:</Typography>
                            <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" sx={{ gap: 1 }}>
                                <Chip label="1. Parse Prompt" color="primary" size="small" />
                                <Typography>→</Typography>
                                <Chip label="2. Extract URL & Alias" color="primary" size="small" />
                                <Typography>→</Typography>
                                <Chip label="3. Validate URL" color="primary" size="small" />
                                <Typography>→</Typography>
                                <Chip
                                    label="4. Request Pending"
                                    color={shortenResult.denied ? 'warning' : 'success'}
                                    size="small"
                                    icon={<HourglassEmptyIcon />}
                                />
                                <Typography>→</Typography>
                                {shortenResult.denied ? (
                                    <Chip
                                        label="Request Denied"
                                        color="error"
                                        size="small"
                                        icon={<ThumbDownIcon />}
                                    />
                                ) : (
                                    <>
                                        <Chip
                                            label="Request Approved"
                                            color="success"
                                            size="small"
                                            icon={<ThumbUpIcon />}
                                        />
                                        <Typography>→</Typography>
                                        <Chip label="5. Call /api/shorten" color="primary" size="small" />
                                        <Typography>→</Typography>
                                        <Chip label="6. Return Result" color="success" size="small" icon={<CheckCircleIcon />} />
                                    </>
                                )}
                            </Stack>
                        </Box>
                    </CardContent>
                </Card>
            )}

            {/* Processing Results Alert */}
            {processingResults.length > 0 && (
                <Alert
                    severity={processingResults.every(r => r.status === 'success') ? 'success' : 'warning'}
                    onClose={() => setProcessingResults([])}
                    sx={{ mt: 2 }}
                >
                    <Typography variant="subtitle2" gutterBottom>
                        Batch Processing Results ({processingResults.length} request(s)):
                    </Typography>
                    <Stack spacing={0.5}>
                        {processingResults.map((r, idx) => (
                            <Typography key={idx} variant="body2">
                                • {r.status === 'success' ? '✅' : r.status === 'denied' ? '🚫' : '❌'} {r.message}
                            </Typography>
                        ))}
                    </Stack>
                </Alert>
            )}

            {/* Pending Requests Dialog */}
            <Dialog
                open={showPendingDialog}
                onClose={() => setShowPendingDialog(false)}
                maxWidth="md"
                fullWidth
                PaperProps={{ sx: { backgroundColor: '#ffffff' } }}
            >
                <DialogTitle>
                    <Stack direction="row" spacing={1} alignItems="center">
                        <HourglassEmptyIcon color="warning" />
                        <Typography variant="h6">Pending Approval Requests</Typography>
                        <Chip label={pendingRequests.length} color="warning" size="small" />
                    </Stack>
                </DialogTitle>
                <DialogContent>
                    {pendingRequests.length === 0 ? (
                        <Alert severity="info">No pending requests.</Alert>
                    ) : (
                        <>
                            <Alert severity="info" sx={{ mb: 2 }}>
                                Select requests to approve or deny, then click the action button below.
                            </Alert>

                            {/* Group by scenario */}
                            {['greenfield', 'brownfield', 'ambiguous'].map(scenarioKey => {
                                const requests = getPendingRequestsByScenario(scenarioKey);
                                if (requests.length === 0) return null;

                                return (
                                    <Accordion key={scenarioKey} defaultExpanded>
                                        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                                            <Stack direction="row" spacing={1} alignItems="center">
                                                <Typography variant="subtitle1" sx={{ textTransform: 'capitalize' }}>
                                                    {scenarioKey}
                                                </Typography>
                                                <Chip label={requests.length} size="small" color="warning" />
                                            </Stack>
                                        </AccordionSummary>
                                        <AccordionDetails>
                                            <Table size="small">
                                                <TableHead>
                                                    <TableRow>
                                                        <TableCell padding="checkbox">
                                                            <Checkbox
                                                                indeterminate={
                                                                    requests.some(r => selectedRequests.includes(r.id)) &&
                                                                    !requests.every(r => selectedRequests.includes(r.id))
                                                                }
                                                                checked={requests.every(r => selectedRequests.includes(r.id))}
                                                                onChange={() => {
                                                                    const allSelected = requests.every(r => selectedRequests.includes(r.id));
                                                                    if (allSelected) {
                                                                        setSelectedRequests(prev => prev.filter(id => !requests.map(r => r.id).includes(id)));
                                                                    } else {
                                                                        setSelectedRequests(prev => [...new Set([...prev, ...requests.map(r => r.id)])]);
                                                                    }
                                                                }}
                                                            />
                                                        </TableCell>
                                                        <TableCell>Type</TableCell>
                                                        <TableCell>Instruction</TableCell>
                                                        <TableCell>Submitted</TableCell>
                                                        <TableCell>Actions</TableCell>
                                                    </TableRow>
                                                </TableHead>
                                                <TableBody>
                                                    {requests.map((request) => (
                                                        <TableRow key={request.id} hover selected={selectedRequests.includes(request.id)}>
                                                            <TableCell padding="checkbox">
                                                                <Checkbox
                                                                    checked={selectedRequests.includes(request.id)}
                                                                    onChange={() => toggleRequestSelection(request.id)}
                                                                />
                                                            </TableCell>
                                                            <TableCell>
                                                                <Chip
                                                                    icon={getRequestTypeIcon(request.type)}
                                                                    label={request.type}
                                                                    size="small"
                                                                    color={request.type === 'chain' ? 'primary' : request.type === 'agent' ? 'secondary' : 'success'}
                                                                />
                                                            </TableCell>
                                                            <TableCell sx={{ maxWidth: 300 }}>
                                                                <Typography variant="body2" noWrap title={request.instruction}>
                                                                    {request.instruction.length > 60
                                                                        ? request.instruction.substring(0, 60) + '...'
                                                                        : request.instruction}
                                                                </Typography>
                                                            </TableCell>
                                                            <TableCell>
                                                                <Typography variant="caption">
                                                                    {request.timestamp.toLocaleTimeString()}
                                                                </Typography>
                                                            </TableCell>
                                                            <TableCell>
                                                                <IconButton
                                                                    size="small"
                                                                    color="error"
                                                                    onClick={() => removePendingRequest(request.id)}
                                                                    title="Remove request"
                                                                >
                                                                    <DeleteIcon fontSize="small" />
                                                                </IconButton>
                                                            </TableCell>
                                                        </TableRow>
                                                    ))}
                                                </TableBody>
                                            </Table>
                                        </AccordionDetails>
                                    </Accordion>
                                );
                            })}
                        </>
                    )}
                </DialogContent>
                <DialogActions>
                    <Stack direction="row" spacing={2} sx={{ width: '100%', justifyContent: 'space-between', px: 2, pb: 1 }}>
                        <Stack direction="row" spacing={1}>
                            <Button onClick={toggleSelectAll} size="small" variant="outlined">
                                {selectedRequests.length === pendingRequests.length ? 'Deselect All' : 'Select All'}
                            </Button>
                            <Typography variant="body2" color="text.secondary" sx={{ alignSelf: 'center' }}>
                                {selectedRequests.length} selected
                            </Typography>
                        </Stack>
                        <Stack direction="row" spacing={1}>
                            <Button onClick={() => setShowPendingDialog(false)}>
                                Close
                            </Button>
                            <Button
                                variant="contained"
                                color="success"
                                disabled={selectedRequests.length === 0 || busy}
                                onClick={() => {
                                    processSelectedRequests('approve');
                                    setShowPendingDialog(false);
                                }}
                                startIcon={<ThumbUpIcon />}
                            >
                                Approve Selected ({selectedRequests.length})
                            </Button>
                            <Button
                                variant="contained"
                                color="error"
                                disabled={selectedRequests.length === 0 || busy}
                                onClick={() => {
                                    processSelectedRequests('deny');
                                    setShowPendingDialog(false);
                                }}
                                startIcon={<ThumbDownIcon />}
                            >
                                Deny Selected ({selectedRequests.length})
                            </Button>
                        </Stack>
                    </Stack>
                </DialogActions>
            </Dialog>
        </Stack>
    );
}