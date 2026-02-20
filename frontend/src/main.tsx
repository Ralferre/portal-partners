import React from "react";
import App from "./App";
import { getTheme, type ThemeKey } from "./theme/theme";
import { CssBaseline, ThemeProvider } from "@mui/material";
import { createRoot } from "react-dom/client";
import { AuthProvider } from "./contexts/AuthContext";

const STORAGE_THEME_KEY = "@PortalPartners:theme";

function readThemeKey(): ThemeKey {
  const raw = localStorage.getItem(STORAGE_THEME_KEY);
  if (raw === "forest" || raw === "sunset" || raw === "agro") return raw;
  return "agro";
}

function Root() {
  const [themeKey, setThemeKey] = React.useState<ThemeKey>(() => readThemeKey());

  React.useEffect(() => {
    const handler = () => setThemeKey(readThemeKey());
    window.addEventListener("portalpartners:theme", handler);
    return () => window.removeEventListener("portalpartners:theme", handler);
  }, []);

  const theme = React.useMemo(() => getTheme(themeKey), [themeKey]);

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AuthProvider>
        <App />
      </AuthProvider>
    </ThemeProvider>
  );
}

createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <Root />
  </React.StrictMode>
);
