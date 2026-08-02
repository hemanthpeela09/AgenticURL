import { Grid, Paper, Typography, Box, Divider, Chip, IconButton, Tooltip, CircularProgress } from '@mui/material';
import { useTranslation } from 'react-i18next';
import RefreshIcon from '@mui/icons-material/Refresh';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import CloudIcon from '@mui/icons-material/Cloud';
import CloudOffIcon from '@mui/icons-material/CloudOff';
import { useState, useEffect } from 'react';
import { getBlueGreenMetrics } from '../api';

function Metric({ label, value, color }) {
    return (
        <Grid item xs={6} sm={4} md={3}>
            <Paper sx={{ p: 1.5, textAlign: 'center', height: '100%', borderLeft: color ? `4px solid ${color}` : 'none' }}>
                <Typography variant="h6">{value}</Typography>
                <Typography variant="caption" color="text.secondary">{label}</Typography>
            </Paper>
        </Grid>
    );
}

function NodeStatus({ name, healthy, url, lastCheckMs }) {
    const statusColor = healthy ? 'success' : 'error';
    const StatusIcon = healthy ? CheckCircleIcon : ErrorIcon;
    const NodeIcon = healthy ? CloudIcon : CloudOffIcon;

    return (
        <Paper sx={{ p: 2, mb: 2, display: 'flex', alignItems: 'center', gap: 2 }}>
            <NodeIcon color={statusColor} sx={{ fontSize: 40 }} />
            <Box sx={{ flexGrow: 1 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Typography variant="h6">{name}</Typography>
                    <Chip
                        icon={<StatusIcon />}
                        label={healthy ? 'HEALTHY' : 'DOWN'}
                        color={statusColor}
                        size="small"
                    />
                </Box>
                <Typography variant="body2" color="text.secondary">{url}</Typography>
                {lastCheckMs && (
                    <Typography variant="caption" color="text.secondary">
                        Last check: {new Date(lastCheckMs).toLocaleTimeString()}
                    </Typography>
                )}
            </Box>
        </Paper>
    );
}

export default function MetricsPanel({ showBlueGreen = true }) {
    const { t } = useTranslation();
    const [blueGreenMetrics, setBlueGreenMetrics] = useState({
        greenfield: {
            healthy: true,
            url: 'http://localhost:8080',
            lastCheckMs: Date.now(),
        },
        requests: {
            total: 12,
            successful: 11,
            failed: 1,
            currentlyQueued: 0,
        },
        performance: {
            totalRetries: 2,
            totalWaitTimeMs: 180,
            avgWaitTimeMs: 90,
        },
        config: {
            maxRetryAttempts: 3,
            retryDelayMs: 250,
            maxWaitMs: 1000,
        },
    });
    const [blueGreenLoading, setBlueGreenLoading] = useState(false);
    const [blueGreenError, setBlueGreenError] = useState(null);

    const fetchBlueGreenMetrics = async () => {
        setBlueGreenLoading(true);
        setBlueGreenError(null);
        try {
            const data = await getBlueGreenMetrics();
            if (data) {
                setBlueGreenMetrics(data);
            }
        } catch (err) {
            setBlueGreenError(err.message);
        } finally {
            setBlueGreenLoading(false);
        }
    };

    useEffect(() => {
        if (showBlueGreen) {
            fetchBlueGreenMetrics();
            // Auto-refresh every 10 seconds
            const interval = setInterval(fetchBlueGreenMetrics, 10000);
            return () => clearInterval(interval);
        }
    }, [showBlueGreen]);

    if (!showBlueGreen) {
        return <Typography color="text.secondary">No metrics available</Typography>;
    }

    return (
        <Box>
            {/* Blue-Green Deployment Metrics */}
            <Box sx={{ mb: 3 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                    <Typography variant="h6">Blue-Green Deployment Metrics</Typography>
                    <Tooltip title="Refresh metrics">
                        <IconButton size="small" onClick={fetchBlueGreenMetrics} disabled={blueGreenLoading}>
                            {blueGreenLoading ? <CircularProgress size={20} /> : <RefreshIcon />}
                        </IconButton>
                    </Tooltip>
                </Box>

                {blueGreenError && (
                    <Paper sx={{ p: 2, mb: 2, bgcolor: 'error.light', color: 'error.contrastText' }}>
                        <Typography>Error fetching metrics: {blueGreenError}</Typography>
                    </Paper>
                )}

                {blueGreenMetrics && (
                    <>
                        {/* Node Status */}
                        <Typography variant="subtitle2" gutterBottom sx={{ mt: 2 }}>Node Status</Typography>
                        <Grid container spacing={2}>
                            <Grid item xs={12} md={6}>
                                <NodeStatus
                                    name="Greenfield (Blue)"
                                    healthy={blueGreenMetrics.greenfield?.healthy}
                                    url={blueGreenMetrics.greenfield?.url}
                                    lastCheckMs={blueGreenMetrics.greenfield?.lastCheckMs}
                                />
                            </Grid>
                            <Grid item xs={12} md={6}>
                                <NodeStatus
                                    name="Brownfield (Green)"
                                    healthy={true}
                                    url="localhost:8081 (current)"
                                />
                            </Grid>
                        </Grid>

                        {/* Request Metrics */}
                        <Typography variant="subtitle2" gutterBottom sx={{ mt: 2 }}>Request Metrics</Typography>
                        <Grid container spacing={1.5}>
                            <Metric
                                label="Total Requests"
                                value={blueGreenMetrics.requests?.total ?? '-'}
                            />
                            <Metric
                                label="Successful"
                                value={blueGreenMetrics.requests?.successful ?? '-'}
                                color="#4caf50"
                            />
                            <Metric
                                label="Failed"
                                value={blueGreenMetrics.requests?.failed ?? '-'}
                                color="#f44336"
                            />
                            <Metric
                                label="Queued"
                                value={blueGreenMetrics.requests?.currentlyQueued ?? '-'}
                                color="#ff9800"
                            />
                        </Grid>

                        {/* Performance Metrics */}
                        <Typography variant="subtitle2" gutterBottom sx={{ mt: 2 }}>Performance (Retry Strategy)</Typography>
                        <Grid container spacing={1.5}>
                            <Metric
                                label="Total Retries"
                                value={blueGreenMetrics.performance?.totalRetries ?? '-'}
                            />
                            <Metric
                                label="Total Wait (ms)"
                                value={blueGreenMetrics.performance?.totalWaitTimeMs ?? '-'}
                            />
                            <Metric
                                label="Avg Wait (ms)"
                                value={blueGreenMetrics.performance?.avgWaitTimeMs ?? '-'}
                            />
                        </Grid>

                        {/* Config */}
                        <Typography variant="subtitle2" gutterBottom sx={{ mt: 2 }}>Configuration</Typography>
                        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                            <Chip label={`Max Retries: ${blueGreenMetrics.config?.maxRetryAttempts ?? '-'}`} size="small" variant="outlined" />
                            <Chip label={`Retry Delay: ${blueGreenMetrics.config?.retryDelayMs ?? '-'}ms`} size="small" variant="outlined" />
                            <Chip label={`Max Wait: ${blueGreenMetrics.config?.maxWaitMs ?? '-'}ms`} size="small" variant="outlined" />
                        </Box>
                    </>
                )}

                {!blueGreenMetrics && !blueGreenLoading && !blueGreenError && (
                    <Typography color="text.secondary">Loading metrics...</Typography>
                )}
            </Box>
        </Box>
    );
}