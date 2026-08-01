import { useState } from "react";
import { Box, TextField, Button, Stack, Card, CardContent, Typography, Alert, Divider, Table,
    TableBody, TableCell, TableContainer, TableHead, TableRow, Chip, 
 } from "@mui/material";
import { useTranslation } from "react-i18next";
import { stats } from "../api.js";

function CountTable({ title, data }) {
    const { t } = useTranslation();
    const entries = Object.entries(data || {});
    if(entries.length === 0) {
        return null;
    }
    return (
        <Box sx={{ mt: 2 }}>
            <Typography variant="subtitle2" gutterBottom>
                {t(title)}
            </Typography>
            <Table size="small">
                <TableHead>
                    <TableRow>
                        <TableCell>{t('stats.key')}</TableCell>
                        <TableCell align="right">{t('stats.clicksCol')}</TableCell>
                    </TableRow>
                    </TableHead>
                <TableBody>
                    {entries.map(([key, count]) => (
                        <TableRow key={key}>
                            <TableCell>{key}</TableCell>
                            <TableCell align="right">{count}</TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </Box>
    );
}

export default function StatsPanel({ code: initialCode}) {
    const { t } = useTranslation();
    const [code, setCode] = useState(initialCode || '');
    const [error, setError] = useState(null);
    const [data, setData] = useState(null);

    const load = async () => {
        setError(null);
        setData(null);
        try {
         setData(await stats(code.trim()));
        } catch (err) {
            setError(err.message);
        }
    };

    return (
        <Card>
            <CardContent>
                <Typography variant="h6">{t('stats.title')}</Typography>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="flex-start" sx={{ mt: 2 }}>
                    <TextField label={t('stats.code')} value={code}  size="small" onChange={(e) => setCode(e.target.value)} placeholder="abc123" />
                    <Button variant="contained" onClick={load} disabled={!code.trim()}>
                        {t('stats.load')}
                    </Button>
                </Stack>
                {error && (
                    <Alert severity="error" sx={{ mt: 2 }}>
                        {error}
                    </Alert>
                )}
                {data && (
                    <Box sx={{ mt: 2 }}>
                        <Stack direction="row" spacing={1} alignItems="center">
                            <Chip label={t('stats.clicks', {count: data.totalClicks})} color="primary" />
                            {data.lastAccessed && (
                                <Typography variant="body2" color="text.secondary">
                                    {t('stats.lastAccessed', { when: new Date(data.lastAccessed).toLocaleString() })}
                                    </Typography>
                            )}
                        </Stack>
                        <Typography variant="body2" sx={{ wordBreak: 'break-all'}}>
                         {data.longUrl}</Typography>
                        <Divider sx={{ my: 1 }} />
                        <CountTable title="stats.byDay" data={data.clicksByDay} />
                        <CountTable title="stats.referrers" data={data.referrers} />
                    </Box>
                )}
            </CardContent>
        </Card>
    );
}