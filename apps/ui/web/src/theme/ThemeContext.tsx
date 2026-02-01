import { ThemeProvider as MuiThemeProvider } from "@mui/material/styles";
import { createContext, useContext, useMemo, useState } from "react";
import { getTheme } from "./theme";

type Mode = "light" | "dark";

const ThemeContext = createContext({
    mode: "light" as Mode,
    toggleMode: () => { },
});

export const ThemeProvider = ({ children }: { children: React.ReactNode }) => {
    const [mode, setMode] = useState<Mode>("dark");

    const toggleMode = () =>
        setMode((prev) => (prev === "light" ? "dark" : "light"));

    const theme = useMemo(() => getTheme(mode), [mode]);

    return (
        <ThemeContext.Provider value={{ mode, toggleMode }}>
            <MuiThemeProvider theme={theme}>{children}</MuiThemeProvider>
        </ThemeContext.Provider>
    );
};

export const useThemeMode = () => useContext(ThemeContext);
