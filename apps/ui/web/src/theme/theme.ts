import { createTheme } from "@mui/material/styles";

export const getTheme = (mode: "light" | "dark") =>
    createTheme({
        palette: {
            mode,
            primary: {
                main: "#8819d2ff",
            },
        },
        typography: {
            fontFamily: "Inter, Roboto, sans-serif",
        },
        breakpoints: {
            values: {
                xs: 0,
                sm: 600,
                md: 900,
                lg: 1200,
                xl: 1536,
            },
        },
    });

// JsonViewerConfig.ts
export const JSON_VIEWER_CONFIG = {
    indentPx: 2, // juster denne for mer/mindre indent
    colors: {
        bracket: "primary.main",
        key: "warning.main",
        string: "success.main",
        number: "info.main",
        boolean: "info.main",
        null: "info.main",
        unknown: "text.secondary"
    }
} as const
