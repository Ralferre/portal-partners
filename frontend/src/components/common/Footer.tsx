import { Box, Typography, Link } from "@mui/material";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import { RSOFT_COLORS, RSOFT_TAGLINE } from "../../theme/theme";

/**
 * Rodape institucional com a marca RSOFT, a tagline e um selo de seguranca.
 * Exibido nas telas internas (ver AppLayout).
 */
export function Footer() {
  const ano = new Date().getFullYear();
  return (
    <Box
      component="footer"
      sx={{
        mt: "auto",
        px: 3,
        py: 2,
        display: "flex",
        flexWrap: "wrap",
        gap: 1,
        alignItems: "center",
        justifyContent: "space-between",
        borderTop: "1px solid rgba(11,31,59,0.08)",
        color: RSOFT_COLORS.graphite,
      }}
    >
      <Typography variant="caption">
        © {ano} <strong>RSOFT</strong> — Portal Partners. {RSOFT_TAGLINE}
      </Typography>
      <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, opacity: 0.9 }}>
        <LockOutlinedIcon sx={{ fontSize: 14, color: RSOFT_COLORS.primary }} />
        <Typography variant="caption">
          Dados criptografados · LGPD · Trilha de auditoria
        </Typography>
      </Box>
    </Box>
  );
}

export default Footer;
