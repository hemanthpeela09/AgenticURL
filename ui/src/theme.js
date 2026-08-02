import { createTheme } from '@mui/material/styles';

// Create a theme instance.
const theme = createTheme({
    palette: {
        primary: { main: '#2F749A' },
        secondary: { main: '#092e5d' },
        background: { default: '#F4f6f9', paper: '#ffffff' },
        success: { main: '#28743e' },
        warning: { main: '#ff9800' },
        error: { main: '#b91224' },
        text: { primary: '#464646', secondary: '#727272' },
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
            defaultProps: {
                disableElevation: true
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
        MuiDialog: {
            styleOverrides: {
                paper: {
                    backgroundColor: '#ffffff',
                },
            },
        },
    },
});

export default theme;  