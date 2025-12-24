import React from "react";
import App from "./App";
import { theme } from "./theme/theme";
import { CssBaseline, ThemeProvider } from "@mui/material";
import { createRoot } from "react-dom/client";

createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <App />
    </ThemeProvider>
  </React.StrictMode>
);
