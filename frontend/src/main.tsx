import React from "react";
import App from "./App";
import { theme } from "./theme/theme";
import { CssBaseline, ThemeProvider } from "@mui/material";
import { createRoot } from "react-dom/client";
import { AuthProvider } from "./contexts/AuthContext";
import { initObservability } from "./observability/sentry";

initObservability();

createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AuthProvider>
        <App />
      </AuthProvider>
    </ThemeProvider>
  </React.StrictMode>
);
