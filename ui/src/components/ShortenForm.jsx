import { useState } from 'react';
import {
    Box, Button, Card, CardContent, TextField, Typography, Alert, Link, Stack, Grid,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { shorten } from '../api.js';

export default function ShortenForm({ onCreated }) {
    const { t } = useTranslation();
    const [url, setUrl] = useState('');
    const [alias, setAlias] = useState('');
    const [ttl, setTtl] = useState('');
    const [result, setResult] = useState(null);
    const [error, setError] = useState(null);
    const [busy, setBusy] = useState(false);

    const submit = async (e) => {
        e.preventDefault();
        setError(null);
        setResult(null);
        setBusy(true);
        try {
            const payload = { url };
            if (alias.trim()) payload.customAlias = alias.trim();
            if (ttl.trim()) payload.ttlSeconds = Number(ttl);
            const res = await shorten(payload);
            setResult(res);
            if (onCreated) onCreated(res.code);
        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    };

    return (
        <Card>
            <CardContent>
                <Typography variant="h6" gutterBottom>{t('shorten.title')}</Typography>
                <Box component="form" onSubmit={submit}>
                    <Grid container spacing={2}>
                        <Grid item xs={12}>
                            <TextField
                                label={t('shorten.longUrl')} fullWidth required value={url}
                                onChange={(e) => setUrl(e.target.value)}
                                placeholder="https://example.com/very/long/path"
                            />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <TextField
                                label={t('shorten.alias')} fullWidth value={alias}
                                onChange={(e) => setAlias(e.target.value)} placeholder="my-link"
                            />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <TextField
                                label={t('shorten.ttl')} fullWidth type="number" value={ttl}
                                onChange={(e) => setTtl(e.target.value)} placeholder="3600"
                            />
                        </Grid>
                    </Grid>
                    <Stack direction="row" spacing={2} sx={{ mt: 2 }}>
                        <Button type="submit" variant="contained" disabled={busy}>
                            {busy ? t('shorten.submitting') : t('shorten.submit')}
                        </Button>
                    </Stack>
                </Box>

                {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
                {result && (
                    <Alert severity="success" sx={{ mt: 2 }}>
                        <Typography variant="body2">
                            {t('shorten.resultPrefix')}{' '}
                            <Link href={result.shortUrl} target="_blank" rel="noreferrer">
                                {result.shortUrl}
                            </Link>{' '}
                            ({t('shorten.code')} <strong>{result.code}</strong>)
                        </Typography>
                    </Alert>
                )}
            </CardContent>
        </Card>
    );
}