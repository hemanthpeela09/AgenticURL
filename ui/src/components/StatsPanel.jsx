import { useState, useEffect } from 'react';
import {
    Box, Button, Card, CardContent, TextField, Typography, Alert, Stack, Divider,
    Table, TableBody, TableCell, TableHead, TableRow, Chip, Dialog, DialogTitle,
    DialogContent, DialogActions, IconButton, Link as MuiLink,
} from '@mui/material';
import ListAltIcon from '@mui/icons-material/ListAlt';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import CloseIcon from '@mui/icons-material/Close';
import RefreshIcon from '@mui/icons-material/Refresh';
import { useTranslation } from 'react-i18next';
import { stats, listAllUrls } from '../api.js';

function CountTable({ title, data }) {
    const { t } = useTranslation();
    const entries = Object.entries(data || {});
    if (entries.length === 0) return null;
    return (
        <Box sx={{ mt: 2 }}>
            <Typography variant="subtitle2" gutterBottom>{title}</Typography>
            <Table size="small">
                <TableHead>
                    <TableRow>
                        <TableCell>{t('stats.key')}</TableCell>
                        <TableCell align="right">{t('stats.clicksCol')}</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {entries.map(([k, v]) => (
                        <TableRow key={k}>
                            <TableCell>{k}</TableCell>
                            <TableCell align="right">{v}</TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </Box>
    );
}

export default function StatsPanel({ code: initialCode }) {
    const { t } = useTranslation();
    const [code, setCode] = useState(initialCode || '');
    const [data, setData] = useState(null);
    const [error, setError] = useState(null);

    // All URLs state
    const [allUrls, setAllUrls] = useState([]);
    const [showAllUrlsDialog, setShowAllUrlsDialog] = useState(false);
    const [loadingUrls, setLoadingUrls] = useState(false);
    const [urlsError, setUrlsError] = useState(null);

    // Auto-load all URLs on component mount
    useEffect(() => {
        loadAllUrls(false); // Load silently without opening dialog
    }, []);

    const load = async () => {
        setError(null);
        setData(null);
        try {
            setData(await stats(code.trim()));
        } catch (err) {
            setError(err.message);
        }
    };

    const loadAllUrls = async (openDialog = true) => {
        setLoadingUrls(true);
        setUrlsError(null);
        try {
            const urls = await listAllUrls();
            setAllUrls(urls);
            if (openDialog) {
                setShowAllUrlsDialog(true);
            }
        } catch (err) {
            setUrlsError(err.message);
        } finally {
            setLoadingUrls(false);
        }
    };

    const copyToClipboard = (text) => {
        navigator.clipboard.writeText(text);
    };

    const selectUrlForStats = (urlCode) => {
        setCode(urlCode);
        setShowAllUrlsDialog(false);
    };

    return (
        <Card>
            <CardContent>
                <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
                    <Typography variant="h6">{t('stats.title')}</Typography>
                    <Stack direction="row" spacing={1} alignItems="center">
                        {allUrls.length > 0 && (
                            <Chip label={`${allUrls.length} URLs`} size="small" color="primary" variant="outlined" />
                        )}
                        <IconButton
                            size="small"
                            onClick={() => loadAllUrls(false)}
                            disabled={loadingUrls}
                            title="Refresh URLs"
                        >
                            <RefreshIcon fontSize="small" />
                        </IconButton>
                        <Button
                            variant="outlined"
                            onClick={() => loadAllUrls(true)}
                            disabled={loadingUrls}
                            startIcon={<ListAltIcon />}
                            size="small"
                        >
                            {loadingUrls ? 'Loading...' : 'List All URLs'}
                        </Button>
                    </Stack>
                </Stack>

                <Stack direction="row" spacing={2}>
                    <TextField
                        label={t('stats.code')}
                        size="small"
                        value={code}
                        onChange={(e) => setCode(e.target.value)}
                        placeholder="abc123"
                    />
                    <Button variant="outlined" onClick={load} disabled={!code.trim()}>{t('stats.load')}</Button>
                </Stack>

                {urlsError && <Alert severity="error" sx={{ mt: 2 }}>{urlsError}</Alert>}
                {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
                {data && (
                    <Box sx={{ mt: 2 }}>
                        <Stack direction="row" spacing={1} sx={{ mb: 1 }} alignItems="center">
                            <Chip color="primary" label={t('stats.clicks', { count: data.totalClicks })} />
                            {data.lastAccessed && (
                                <Typography variant="caption" color="text.secondary">
                                    {t('stats.lastAccessed', { when: new Date(data.lastAccessed).toLocaleString() })}
                                </Typography>
                            )}
                        </Stack>
                        <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>
                            &rarr; {data.longUrl}
                        </Typography>
                        <Divider sx={{ my: 1 }} />
                        <CountTable title={t('stats.byDay')} data={data.clicksByDay} />
                        <CountTable title={t('stats.referrers')} data={data.referrers} />
                    </Box>
                )}
            </CardContent>

            {/* All URLs Dialog */}
            <Dialog
                open={showAllUrlsDialog}
                onClose={() => setShowAllUrlsDialog(false)}
                maxWidth="lg"
                fullWidth
            >
                <DialogTitle>
                    <Stack direction="row" justifyContent="space-between" alignItems="center">
                        <Stack direction="row" spacing={1} alignItems="center">
                            <ListAltIcon color="primary" />
                            <Typography variant="h6">All Shortened URLs</Typography>
                            <Chip label={allUrls.length} color="primary" size="small" />
                        </Stack>
                        <IconButton onClick={() => setShowAllUrlsDialog(false)} size="small">
                            <CloseIcon />
                        </IconButton>
                    </Stack>
                </DialogTitle>
                <DialogContent>
                    {allUrls.length === 0 ? (
                        <Alert severity="info">No URLs have been shortened yet.</Alert>
                    ) : (
                        <Table size="small">
                            <TableHead>
                                <TableRow>
                                    <TableCell>Code</TableCell>
                                    <TableCell>Short URL</TableCell>
                                    <TableCell>Original URL</TableCell>
                                    <TableCell>Created</TableCell>
                                    <TableCell>Expires</TableCell>
                                    <TableCell>Actions</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {allUrls.map((url) => (
                                    <TableRow key={url.code} hover>
                                        <TableCell>
                                            <Chip
                                                label={url.code}
                                                size="small"
                                                color="primary"
                                                variant="outlined"
                                                onClick={() => selectUrlForStats(url.code)}
                                                sx={{ cursor: 'pointer' }}
                                            />
                                        </TableCell>
                                        <TableCell>
                                            <Stack direction="row" spacing={0.5} alignItems="center">
                                                <MuiLink href={url.shortUrl} target="_blank" rel="noreferrer" sx={{ maxWidth: 200 }} noWrap>
                                                    {url.shortUrl}
                                                </MuiLink>
                                                <IconButton size="small" onClick={() => copyToClipboard(url.shortUrl)} title="Copy short URL">
                                                    <ContentCopyIcon fontSize="small" />
                                                </IconButton>
                                            </Stack>
                                        </TableCell>
                                        <TableCell sx={{ maxWidth: 300 }}>
                                            <Typography variant="body2" noWrap title={url.longUrl}>
                                                {url.longUrl}
                                            </Typography>
                                        </TableCell>
                                        <TableCell>
                                            <Typography variant="caption">
                                                {new Date(url.createdAt).toLocaleString()}
                                            </Typography>
                                        </TableCell>
                                        <TableCell>
                                            {url.expiresAt ? (
                                                <Chip
                                                    label={new Date(url.expiresAt).toLocaleString()}
                                                    size="small"
                                                    color={new Date(url.expiresAt) < new Date() ? 'error' : 'warning'}
                                                />
                                            ) : (
                                                <Chip label="Never" size="small" color="success" />
                                            )}
                                        </TableCell>
                                        <TableCell>
                                            <Stack direction="row" spacing={0.5}>
                                                <IconButton
                                                    size="small"
                                                    onClick={() => selectUrlForStats(url.code)}
                                                    title="View Stats"
                                                    color="primary"
                                                >
                                                    <ListAltIcon fontSize="small" />
                                                </IconButton>
                                                <IconButton
                                                    size="small"
                                                    href={url.shortUrl}
                                                    target="_blank"
                                                    title="Open URL"
                                                    color="success"
                                                >
                                                    <OpenInNewIcon fontSize="small" />
                                                </IconButton>
                                            </Stack>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setShowAllUrlsDialog(false)}>Close</Button>
                </DialogActions>
            </Dialog>
        </Card>
    );
}