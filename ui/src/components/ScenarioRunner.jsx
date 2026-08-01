import { useEffect, useState } from 'react';
import {
    Box, Button, Card, CardContent, Typography, Alert, Stack, MenuItem, TextField,
    Chip, Accordion, AccordionSummary, AccordionDetails, Table, TableBody, TableCell,
    TableHead, TableRow,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { useTranslation } from 'react-i18next';
import { listScenarios, runScenario } from '../api.js';
import DagView from './DagView.jsx';
import MetricsPanel from "./MetricsPanel.jsx";

const KIND_COLOR = {
    policy: 'secondary', approval: 'warning', error: 'error', rollback: 'warning',
    safe_stop: 'error', replan: 'info', clarification: 'info', synchronize: 'success',
    parallel: 'info', artifact: 'default',
};

export default function ScenarioRunner() {
    const { t } = useTranslation();
    const [scenarios, setScenarios] = useState([]);
    const [scenario, setScenario] = useState('greenfield');
    const [approval, setApproval] = useState('auto');
    const [result, setResult] = useState(null);
    const [error, setError] = useState(null);
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        listScenarios().then(setScenarios).catch((e) => setError(e.message));
    }, []);

    const run = async () => {
        setError(null);
        setResult(null);
        setBusy(true);
        try {
            setResult(await runScenario(scenario, approval));
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
                    <Typography variant="h6" gutterBottom>{t('orch.title')}</Typography>
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="flex-start">
                        <TextField select label={t('orch.scenario')} size="small" value={scenario}
                                   onChange={(e) => setScenario(e.target.value)} sx={{ minWidth: 220 }}>
                            {scenarios.map((s) => (
                                <MenuItem key={s.key} value={s.key}>{s.key}</MenuItem>
                            ))}
                        </TextField>
                        <TextField select label={t('orch.approval')} size="small" value={approval}
                                   onChange={(e) => setApproval(e.target.value)} sx={{ minWidth: 160 }}>
                            <MenuItem value="auto">{t('orch.approvalAuto')}</MenuItem>
                            <MenuItem value="deny">{t('orch.approvalDeny')}</MenuItem>
                        </TextField>
                        <Button variant="contained" onClick={run} disabled={busy}>
                            {busy ? t('orch.running') : t('orch.run')}
                        </Button>
                    </Stack>
                    {scenarios.find((s) => s.key === scenario) && (
                        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                            "{scenarios.find((s) => s.key === scenario).requirement}"
                        </Typography>
                    )}
                    {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
                </CardContent>
            </Card>

            {result && (
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
                            {t('orch.openQuestions')} {result.openQuestions.join(', ')}
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
        </Stack>
    );
}