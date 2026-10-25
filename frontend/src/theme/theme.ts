import { createTheme } from "@mui/material/styles";

/**
 * Tema unico da marca RSOFT.
 *
 * Paleta oficial:
 *  - #007BFF  Azul tecnologia / confianca   (primary)
 *  - #0052CC  Azul inovacao / crescimento   (secondary)
 *  - #0B1F3B  Azul-marinho profissionalismo (dark / superficies escuras)
 *  Neutros: branco e grafite.
 *
 * Portal Partners e um produto da RSOFT.
 */
export const RSOFT_COLORS = {
  primary: "#007BFF",
  primaryDark: "#0052CC",
  navy: "#0B1F3B",
  graphite: "#2B3442",
  bgLight: "#F4F7FB",
  white: "#FFFFFF",
} as const;

export const RSOFT_TAGLINE = "Mais que código. Soluções para o seu futuro.";

export const theme = createTheme({
  palette: {
    primary: { main: RSOFT_COLORS.primary, dark: RSOFT_COLORS.primaryDark, contrastText: "#fff" },
    secondary: { main: RSOFT_COLORS.primaryDark, contrastText: "#fff" },
    background: { default: RSOFT_COLORS.bgLight, paper: RSOFT_COLORS.white },
    text: { primary: RSOFT_COLORS.navy, secondary: RSOFT_COLORS.graphite },
  },
  typography: {
    fontFamily: "'Inter', 'Segoe UI', Roboto, Arial, sans-serif",
    h4: { fontWeight: 700, letterSpacing: "-0.5px" },
    h5: { fontWeight: 700 },
    h6: { fontWeight: 700 },
    button: { fontWeight: 600, textTransform: "none" },
  },
  shape: { borderRadius: 10 },
  components: {
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundImage: `linear-gradient(90deg, ${RSOFT_COLORS.navy} 0%, ${RSOFT_COLORS.primaryDark} 100%)`,
        },
      },
    },
    MuiButton: {
      styleOverrides: { root: { borderRadius: 10, paddingInline: 20 } },
    },
    MuiCard: {
      styleOverrides: {
        root: { borderRadius: 14, boxShadow: "0 4px 20px rgba(11,31,59,0.08)" },
      },
    },
  },
});

export default theme;
