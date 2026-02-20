import { createTheme } from "@mui/material/styles";

export type ThemeKey = "agro" | "forest" | "sunset";

const THEMES: Record<ThemeKey, { primary: string; secondary: string }> = {
  agro: { primary: "#3E7580", secondary: "#1f4e8fff" },
  forest: { primary: "#1B5E20", secondary: "#2E7D32" },
  sunset: { primary: "#6A1B9A", secondary: "#EF6C00" },
};

export function getTheme(themeKey: ThemeKey): any {
  const colors = THEMES[themeKey] ?? THEMES.agro;
  return createTheme({
    palette: {
      primary: { main: colors.primary },
      secondary: { main: colors.secondary },
    },
    typography: {
      fontFamily: "Roboto, Arial, sans-serif",
    },
  });
};

export const theme: any = createTheme({
  palette: {
    primary: {
      main: "#3E7580",
    },
    secondary: {
      main: "#1f4e8fff",
    },
  },
  typography: {
    fontFamily: "Roboto, Arial, sans-serif",
  },
});