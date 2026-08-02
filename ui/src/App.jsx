import { useState, useEffect, useRef } from 'react';
import {
    AppBar, Toolbar, Typography, Container, Tabs, Tab, Box, Stack, Badge,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import ShortenForm from './components/ShortenForm.jsx';
import StatsPanel from './components/StatsPanel.jsx';
import ScenarioRunner from './components/ScenarioRunner.jsx';
import LanguageSelector from './components/LanguageSelector.jsx';

function TabPanel({ value, index, children }) {
    return value === index ? <Box sx={{ py: 3 }}>{children}</Box> : null;
}

// Helper to load pending requests from localStorage
const loadPendingRequests = () => {
    try {
        const saved = localStorage.getItem('pendingRequests');
        if (saved) {
            const parsed = JSON.parse(saved);
            if (Array.isArray(parsed) && parsed.length > 0) {
                // Convert timestamp strings back to Date objects
                const restored = parsed.map(r => ({
                    ...r,
                    timestamp: r.timestamp ? new Date(r.timestamp) : new Date()
                }));
                console.log('Restored pending requests from localStorage:', restored.length);
                return restored;
            }
        }
    } catch (e) {
        console.error('Failed to load pending requests from localStorage:', e);
    }
    return [];
};

// Helper to save pending requests to localStorage
const savePendingRequests = (requests) => {
    try {
        localStorage.setItem('pendingRequests', JSON.stringify(requests));
        console.log('Saved pending requests to localStorage:', requests.length);
    } catch (e) {
        console.error('Failed to save pending requests to localStorage:', e);
    }
};

export default function App() {
    const { t } = useTranslation();
    const [tab, setTab] = useState(0);
    const [lastCode, setLastCode] = useState('');

    // Lift pending requests state to App level to persist across tab changes
    // Initialize from localStorage to survive page refresh
    const [pendingRequests, setPendingRequests] = useState(loadPendingRequests);

    // Track if this is the first render to avoid overwriting localStorage on mount
    const isFirstRender = useRef(true);

    // Save pending requests to localStorage whenever they change (skip first render)
    useEffect(() => {
        if (isFirstRender.current) {
            isFirstRender.current = false;
            return;
        }
        savePendingRequests(pendingRequests);
    }, [pendingRequests]);

    return (
        <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
            <AppBar position="static" sx={{ bgcolor: 'secondary.main' }}>
                <Toolbar>
                    <Box sx={{ flexGrow: 1 }}>
                        <Typography variant="h6">{t('app.title')}</Typography>
                        <Typography variant="caption">{t('app.subtitle')}</Typography>
                    </Box>
                    <LanguageSelector />
                </Toolbar>
            </AppBar>

            <Container maxWidth="lg">
                <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mt: 2 }}>
                    <Tab label={t('tabs.shortener')} />
                    <Tab label={t('tabs.analytics')} />
                    <Tab label={
                        <Badge badgeContent={pendingRequests.length} color="warning" max={99}>
                            {t('tabs.orchestrator')}
                        </Badge>
                    } />
                </Tabs>

                <TabPanel value={tab} index={0}>
                    <Stack spacing={2}>
                        <ShortenForm onCreated={setLastCode} />
                    </Stack>
                </TabPanel>
                <TabPanel value={tab} index={1}>
                    <StatsPanel code={lastCode} />
                </TabPanel>
                <TabPanel value={tab} index={2}>
                    <ScenarioRunner
                        pendingRequests={pendingRequests}
                        setPendingRequests={setPendingRequests}
                    />
                </TabPanel>
            </Container>
        </Box>
    );
}