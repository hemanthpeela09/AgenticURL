import { createTheme } from '@mui/material/styles';

// Create a theme instance.
const theme = createTheme({
    palette: {
        primary: { main: '#2F749A' },
        secondary: { main: '#092e5d' },
        background: { default: '#F4f6f9', paper: '#fffff' },
        success: { main: '#28743e' },
        warning: { main: '#ff9800' },
        error: { main: '#b91224' },
        text: { primary: '#000000', secondary: '#666666' },
    },
    typography: {
        fontFamily: 'Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif',
        h5: { fontWeight: 700 },
        h6: { fontWeight: 600 },
        subtitle2: { fontWeight: 600 },
    },
    shape: {
        borderRadius: 10,
    },
    components: {
        MuiButton: {
            styleOverrides: {
                root: {
                    border: '1px solid #e3e8ef',
                },
            },
        },
        MuiChip: {
            styleOverrides: {
                root: {
                    fontWeight: 500,
                },
            },
        },
        MuiPaper: {
            styleOverrides: {
                root: {
                    border: '1px solid #e3e8ef',
                },
            },
        },
    },
});

export default theme;  