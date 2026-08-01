import { useState } from "react";
import { AppBar, Toolbar, Typography, Container, Box, Tab, Tabs, Stack } from "@mui/material";
import { useTranslation } from "react-i18next";
import LanguageSelector from "./components/LanguageSelector.jsx";
import ShortenForm from "./components/ShortenForm.jsx";
import StatsPanel from "./components/StatsPanel.jsx";
import ScenarioRunner from "./components/ScenarioRunner.jsx";

function TabPanel({ value, index, children}) {
    return value === index ? <Box sx={{ py: 3 }}>{children}</Box> : null;
}

export default function App() {
    const { t } = useTranslation();
    const [tab, setTab] = useState(0);
    const [lastCode, setLastCode] = useState('');

    return (
        <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
            <AppBar position="static" sx={{ bgcolor: 'secondary.main' }}>
                <Toolbar>
                    <Box sx={{ flexGrow: 1}}>
                    <Typography variant="h6">
                        {t('app.title')}
                    </Typography>
                    <Typography variant="caption">
                        {t('app.subtitle')}
                    </Typography>
                    </Box>
                    <LanguageSelector />
                </Toolbar>
            </AppBar>
            <Container maxWidth="lg">
                <Tabs value={tab} onChange={(_, newValue) => setTab(newValue)} sx={{ mt: 2 }}>
                    <Tab label={t('tabs.shortener')} />
                    <Tab label={t('tabs.analytics')} />
                    <Tab label={t('tabs.orchestrator')} />
                </Tabs>
                <TabPanel value={tab} index={0}>
                    <ShortenForm onCreated={setLastCode} />
                </TabPanel>
                <TabPanel value={tab} index={1}>
                    <StatsPanel code={lastCode} />
                </TabPanel>
                <TabPanel value={tab} index={2}>
                    <ScenarioRunner />
                </TabPanel>
            </Container>
        </Box>
    );
}