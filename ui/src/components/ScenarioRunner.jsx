import { useEffect, useState } from 'react';
import {
    Box, Button, Card, CardContent, Typography, Alert, Stack, MenuItem, TextField,
    Chip, Accordion, AccordionSummary, AccordionDetails, Table, TableBody, TableCell,
    TableHead, TableRow, Switch, FormControlLabel, LinearProgress, Paper, Divider,
    IconButton, Collapse, Tabs, Tab,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import PsychologyIcon from '@mui/icons-material/Psychology';
import BuildIcon from '@mui/icons-material/Build';
import RefreshIcon from '@mui/icons-material/Refresh';
import { useTranslation } from 'react-i18next';
import { listScenarios, runScenario, getLlmInfo, runSdlcChain, runSdlcAgent } from '../api.js';
import DagView from './DagView.jsx';
import MetricsPanel from './MetricsPanel.jsx';

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

export default function ScenarioRunner() {
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
        setBusy(true);
        try {
            setResult(await runScenario(scenario, approval));
        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    };

    const runAiChain = async () => {
        const requirement = customRequirement || scenarios.find((s) => s.key === scenario)?.requirement || '';
        if (!requirement) {
            setError('Please enter a requirement or select a scenario');
            return;
        }

        setError(null);
        setChainResult(null);
        setAgentResult(null);
        setBusy(true);
        try {
            const result = await runSdlcChain(requirement);
            setChainResult(result);
        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    };

    const runAiAgent = async () => {
        const input = customRequirement || scenarios.find((s) => s.key === scenario)?.requirement || '';
        if (!input) {
            setError('Please enter a requirement or select a scenario');
            return;
        }

        setError(null);
        setChainResult(null);
        setAgentResult(null);
        setBusy(true);
        try {
            const result = await runSdlcAgent(input);
            setAgentResult(result);
        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    };

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
                        <TextField
                            select
                            label={t('orch.scenario')}
                            size="small"
                            value={scenario}
                            onChange={(e) => setScenario(e.target.value)}
                            sx={{ minWidth: 220 }}
                            SelectProps={{
                                MenuProps: {
                                    PaperProps: {
                                        sx: {
                                            backgroundColor: '#ffffff',
                                        },
                                    },
                                },
                            }}
                        >
                            {scenarios.map((s) => (
                                <MenuItem key={s.key} value={s.key}>{s.key}</MenuItem>
                            ))}
                        </TextField>

                        {!aiMode && (
                            <TextField
                                select
                                label={t('orch.approval')}
                                size="small"
                                value={approval}
                                onChange={(e) => setApproval(e.target.value)}
                                sx={{ minWidth: 160 }}
                                SelectProps={{
                                    MenuProps: {
                                        PaperProps: {
                                            sx: {
                                                backgroundColor: '#ffffff',
                                            },
                                        },
                                    },
                                }}
                            >
                                <MenuItem value="auto">{t('orch.approvalAuto')}</MenuItem>
                                <MenuItem value="deny">{t('orch.approvalDeny')}</MenuItem>
                            </TextField>
                        )}

                        {!aiMode ? (
                            <Button variant="contained" onClick={run} disabled={busy} startIcon={<AccountTreeIcon />}>
                                {busy ? t('orch.running') : t('orch.run')}
                            </Button>
                        ) : (
                            <Stack direction="row" spacing={1}>
                                <Button
                                    variant="contained"
                                    onClick={runAiChain}
                                    disabled={busy}
                                    startIcon={<AccountTreeIcon />}
                                    color="primary"
                                >
                                    {busy ? 'Running...' : 'Run Chain'}
                                </Button>
                                <Button
                                    variant="contained"
                                    onClick={runAiAgent}
                                    disabled={busy}
                                    startIcon={<SmartToyIcon />}
                                    color="secondary"
                                >
                                    {busy ? 'Running...' : 'Run Agent'}
                                </Button>
                            </Stack>
                        )}
                    </Stack>

                    {/* Custom requirement input for AI mode */}
                    {aiMode && (
                        <TextField
                            fullWidth
                            multiline
                            rows={2}
                            label="Custom Requirement (or use scenario)"
                            placeholder="Enter a custom requirement for the AI to process..."
                            value={customRequirement}
                            onChange={(e) => setCustomRequirement(e.target.value)}
                            sx={{ mt: 2 }}
                            variant="outlined"
                        />
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
                            <AccountTreeIcon color="primary" />
                            <Typography variant="h6">LangChain SDLC Result</Typography>
                            <Chip label="Chain Execution" color="primary" size="small" />
                        </Stack>

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
                    </CardContent>
                </Card>
            )}

            {/* AI Agent Result */}
            {agentResult && aiMode && (
                <Card>
                    <CardContent>
                        <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
                            <SmartToyIcon color="secondary" />
                            <Typography variant="h6">AI Agent Result</Typography>
                            <Chip
                                label={agentResult.success ? 'Success' : 'Incomplete'}
                                color={agentResult.success ? 'success' : 'warning'}
                                size="small"
                            />
                        </Stack>

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
                    </CardContent>
                </Card>
            )}
        </Stack>
    );
}