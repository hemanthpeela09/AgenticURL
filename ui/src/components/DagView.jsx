import { Box, Chip, Paper, Stack, Typography, Tooltip, LinearProgress } from '@mui/material';
import { useTranslation } from 'react-i18next';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import PendingIcon from '@mui/icons-material/Pending';
import ErrorIcon from '@mui/icons-material/Error';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';

// Static SDLC graph layout mirroring specs/03-orchestration.spec.md.
const LAYOUT = [
    [{ id: 'requirements', label: 'Requirements', agent: 'Analyst' }],
    [{ id: 'design', label: 'Design *', agent: 'Architect' }],
    [
        { id: 'implementation', label: 'Implementation', agent: 'Implementer' },
        { id: 'test_authoring', label: 'Test Authoring', agent: 'TestAuthor' },
    ],
    [{ id: 'testing', label: 'Testing (join)', agent: 'Tester' }],
    [{ id: 'documentation', label: 'Documentation', agent: 'DocWriter' }],
    [{ id: 'release', label: 'Release *', agent: 'ReleaseMgr' }],
];

const STATUS_COLOR = {
    SUCCEEDED: 'success',
    RUNNING: 'info',
    WAITING_APPROVAL: 'warning',
    FAILED: 'error',
    SAFE_STOPPED: 'error',
    ROLLED_BACK: 'warning',
    SKIPPED: 'default',
    PENDING: 'default',
};

const STATUS_ICON = {
    SUCCEEDED: <CheckCircleIcon fontSize="small" />,
    RUNNING: <PlayArrowIcon fontSize="small" />,
    FAILED: <ErrorIcon fontSize="small" />,
    SAFE_STOPPED: <ErrorIcon fontSize="small" />,
    PENDING: <PendingIcon fontSize="small" />,
};

export default function DagView({ nodeStatus, aiMode = false, agentSteps = [] }) {
    const { t } = useTranslation();
    const statusOf = (id) => (nodeStatus && nodeStatus[id]) || 'PENDING';

    // Calculate completion percentage for AI mode
    const completedNodes = LAYOUT.flat().filter(n => statusOf(n.id) === 'SUCCEEDED').length;
    const totalNodes = LAYOUT.flat().length;
    const completionPercent = Math.round((completedNodes / totalNodes) * 100);

    return (
        <Paper sx={{ p: 2 }}>
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
                <Stack direction="row" spacing={1} alignItems="center">
                    {aiMode ? <SmartToyIcon color="primary" /> : <AccountTreeIcon />}
                    <Typography variant="h6">{t('dag.title')}</Typography>
                    {aiMode && (
                        <Chip
                            label="AI Powered"
                            color="primary"
                            size="small"
                            variant="outlined"
                            icon={<SmartToyIcon />}
                        />
                    )}
                </Stack>
                {aiMode && (
                    <Chip
                        label={`${completionPercent}% Complete`}
                        color={completionPercent === 100 ? 'success' : 'primary'}
                        size="small"
                    />
                )}
            </Stack>

            <Typography variant="caption" color="text.secondary">
                {t('dag.legend')}{aiMode && ' • AI Agent processes each node using LangChain'}
            </Typography>

            {/* Progress bar for AI mode */}
            {aiMode && (
                <Box sx={{ mt: 1, mb: 2 }}>
                    <LinearProgress
                        variant="determinate"
                        value={completionPercent}
                        color={completionPercent === 100 ? 'success' : 'primary'}
                        sx={{ height: 8, borderRadius: 4 }}
                    />
                </Box>
            )}

            <Stack spacing={1.5} sx={{ mt: 2 }} alignItems="center">
                {LAYOUT.map((row, i) => (
                    <Box key={i} sx={{ display: 'flex', gap: 2, justifyContent: 'center', flexWrap: 'wrap' }}>
                        {row.map((n) => {
                            const status = statusOf(n.id);
                            const isRunning = status === 'RUNNING';

                            return (
                                <Tooltip
                                    key={n.id}
                                    title={
                                        <Box>
                                            <Typography variant="body2"><strong>{n.id}</strong></Typography>
                                            <Typography variant="caption">Status: {status}</Typography>
                                            {aiMode && (
                                                <Typography variant="caption" display="block">
                                                    Agent: {n.agent}
                                                </Typography>
                                            )}
                                        </Box>
                                    }
                                >
                                    <Box sx={{ position: 'relative' }}>
                                        <Chip
                                            icon={aiMode ? STATUS_ICON[status] : undefined}
                                            label={
                                                <Stack direction="row" spacing={0.5} alignItems="center">
                                                    <span>{n.label}</span>
                                                    {aiMode && status !== 'PENDING' && (
                                                        <Typography variant="caption" sx={{ opacity: 0.8 }}>
                                                            ({n.agent})
                                                        </Typography>
                                                    )}
                                                </Stack>
                                            }
                                            color={STATUS_COLOR[status] || 'default'}
                                            variant={status === 'PENDING' ? 'outlined' : 'filled'}
                                            sx={{
                                                animation: isRunning ? 'pulse 1.5s infinite' : 'none',
                                                '@keyframes pulse': {
                                                    '0%': { opacity: 1 },
                                                    '50%': { opacity: 0.6 },
                                                    '100%': { opacity: 1 },
                                                },
                                            }}
                                        />
                                        {isRunning && (
                                            <LinearProgress
                                                sx={{
                                                    position: 'absolute',
                                                    bottom: 0,
                                                    left: 0,
                                                    right: 0,
                                                    height: 2,
                                                    borderRadius: '0 0 16px 16px',
                                                }}
                                            />
                                        )}
                                    </Box>
                                </Tooltip>
                            );
                        })}
                    </Box>
                ))}

                {/* Connection arrows */}
                <Box sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    position: 'absolute',
                    pointerEvents: 'none',
                    opacity: 0.3,
                }}>
                    {/* Visual arrows would go here - using CSS borders */}
                </Box>
            </Stack>

            {/* AI Mode Legend */}
            {aiMode && (
                <Box sx={{ mt: 3, pt: 2, borderTop: '1px solid', borderColor: 'divider' }}>
                    <Typography variant="caption" color="text.secondary" gutterBottom>
                        AI Agent Legend:
                    </Typography>
                    <Stack direction="row" spacing={2} flexWrap="wrap" sx={{ mt: 1 }}>
                        <Stack direction="row" spacing={0.5} alignItems="center">
                            <CheckCircleIcon fontSize="small" color="success" />
                            <Typography variant="caption">Completed by AI</Typography>
                        </Stack>
                        <Stack direction="row" spacing={0.5} alignItems="center">
                            <PlayArrowIcon fontSize="small" color="info" />
                            <Typography variant="caption">AI Processing</Typography>
                        </Stack>
                        <Stack direction="row" spacing={0.5} alignItems="center">
                            <PendingIcon fontSize="small" color="disabled" />
                            <Typography variant="caption">Pending</Typography>
                        </Stack>
                        <Stack direction="row" spacing={0.5} alignItems="center">
                            <ErrorIcon fontSize="small" color="error" />
                            <Typography variant="caption">Failed/Stopped</Typography>
                        </Stack>
                    </Stack>
                </Box>
            )}
        </Paper>
    );
}