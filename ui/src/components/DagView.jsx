import { Box, Chip, Paper, Stack, Typography, Tooltip } from "@mui/material";
import { useTranslation } from "react-i18next";

// Static SDLC graph layout for mirroring specs/03-orchestration-spec.md
const LAYOUT = [
    [{ id: 'requirements', label: 'Requirements' }],
    [{ id: 'design', label: 'Design' }],
    [   
        { id: 'implementation', label: 'Implementation' },
        {id: 'test_authoring', label: 'Test Authoring'},
    ],
    [{ id: 'testing', label: 'Testing (join)' }],
    [{ id: 'documentation', label: 'Documentation' }],
    [{ id: 'release', label: 'Release *' }],
];

const STATUS_COLORS = {
    SUCCEEDED: 'success',
    RUNNING: 'info',
    WAITING_APPROVAL: 'warning',
    FAILED: 'error',
    SAFE_STOPPED: 'error',
    ROLLED_BACK: 'warning',
    SKIPPED: 'default',
    PENDING: 'default',
};

export default function DagView({ nodeStatus }) {
    const { t } = useTranslation();
    const statusOf = (id) => (nodeStatus && nodeStatus[id]) || 'PENDING';
    return (
        <Paper sx={{ p: 2, mt: 2 }}>
            <Typography variant="h6" gutterBottom>
                {t('dag.title')}
            </Typography>
            <Typography variant="caption" color="text.secondary">
                {t('dag.legend')}
            </Typography>
            <Stack spacing={1.5} sx={{ mt: 2}} alignItems="center">
                {LAYOUT.map((row, rowIndex) => (
                    <Box key={rowIndex} direction="row" spacing={2} sx={{ display: 'flex', justifyContent: 'center', flexWrap: 'wrap' }}>
                        {row.map((node) => (
                            <Tooltip key={node.id} title={`${node.id} - ${statusOf(node.id)}`} arrow>
                                <Chip
                                    label={`${node.label} - ${statusOf(node.id)}`}
                                    color={STATUS_COLORS[statusOf(node.id)] || 'default'}
                                    variant={statusOf(node.id) === 'PENDING' ? 'outline' : 'filled'}
                                />
                            </Tooltip>
                        ))}
                    </Box>
                ))}
            </Stack>
        </Paper>
    );
}