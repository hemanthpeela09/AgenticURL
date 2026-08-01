import { Grid, Paper, Typography, Box } from "@mui/material";
import { useTranslation } from "react-i18next";

const LABELS = {
    successRate: 'Success rate',
    nodeAttempts: 'Node attempts',
    nodeSuccesses: 'Node successes',
    nodeFailures: 'Node failures',
    retries: 'Retries',
    retryFrequency: 'Retry frequency',
    rollbacks: 'Rollbacks',
    rollbackFrequency: 'Rollback frequency',
    safeStops: 'Safe stops',
    approvalsRequested: 'Approvals requested',
    replans: 'Replans',
    mttrSeconds: 'MTTR (seconds)',
    e2eDurationSeconds: 'E2E duration (seconds)',
};

function Metric({ label, value }) {
    return (
        <Grid item xs={6} sm={4} md={3}>
            <Paper sx={{ p: 1.5, textAlign: 'center', height: '100%' }}>
                <Typography variant="h6">{value}</Typography>
                <Typography variant="caption" color="text.secondary">
                    {label}
                </Typography>
            </Paper>
        </Grid>
    );
}

export default function MetricsPanel({ metrics }) {
    const { t } = useTranslation();
    if (!metrics) return null;
    return(
        <Box>
            <Typography variant={"h6"} gutterBottem>
                {t('metrics.title')}
            </Typography>
            <Grid container spacing={1.5}>
                {Object.entries(LABELS).map(([k, label]) => (
                    <Metric key={k} label={label}
                    value={metrics[k] !== undefined ? String(metrics[k]) : '-'} />
                ))}
            </Grid>
        </Box>
    );
}