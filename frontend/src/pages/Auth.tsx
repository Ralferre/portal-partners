import { Box, Grid, Paper, Typography, Stack, Chip } from "@mui/material";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import GppGoodOutlinedIcon from "@mui/icons-material/GppGoodOutlined";
import FactCheckOutlinedIcon from "@mui/icons-material/FactCheckOutlined";
import { ReactNode } from "react";
import { BrandLogo } from "../components/common/BrandLogo";
import { RSOFT_COLORS, RSOFT_TAGLINE } from "../theme/theme";

type AuthProps = {
  left?: ReactNode;
  right?: ReactNode;
};

export function Auth({ left }: AuthProps) {
  return (
    <Box minHeight="100vh" sx={{ background: RSOFT_COLORS.bgLight }}>
      <Grid container spacing={0}>
        {/* Formulario */}
        <Grid
          minHeight="100vh"
          size={{ xs: 12, md: 5 }}
          justifyContent="center"
          alignItems="center"
          display="flex"
          sx={{ background: RSOFT_COLORS.white, p: 3 }}
        >
          <Paper elevation={0} sx={{ maxWidth: 400, width: "100%", p: 3, background: "transparent" }}>
            {left}
          </Paper>
        </Grid>

        {/* Painel de marca + segurança (vitrine comercial) */}
        <Grid
          minHeight="100vh"
          size={{ xs: 0, md: 7 }}
          sx={{
            display: { xs: "none", md: "flex" },
            flexDirection: "column",
            justifyContent: "center",
            gap: 4,
            p: 8,
            color: "#fff",
            position: "relative",
            overflow: "hidden",
            background: `linear-gradient(135deg, ${RSOFT_COLORS.navy} 0%, ${RSOFT_COLORS.primaryDark} 100%)`,
          }}
        >
          {/* Textura industrial sutil (grade de pontos) */}
          <Box
            aria-hidden
            sx={{
              position: "absolute",
              inset: 0,
              opacity: 0.12,
              backgroundImage:
                "radial-gradient(circle at 1px 1px, #fff 1px, transparent 0)",
              backgroundSize: "28px 28px",
            }}
          />
          <Box sx={{ position: "relative" }}>
            <BrandLogo size={56} textColor="#fff" />
            <Typography variant="h4" sx={{ mt: 4, fontWeight: 800, maxWidth: 520 }}>
              Controle total de conformidade documental em relações B2B
            </Typography>
            <Typography variant="body1" sx={{ mt: 2, opacity: 0.9, maxWidth: 480 }}>
              {RSOFT_TAGLINE}
            </Typography>

            <Stack direction="row" spacing={1} sx={{ mt: 5, flexWrap: "wrap", gap: 1 }}>
              <Chip icon={<LockOutlinedIcon sx={{ color: "#fff !important" }} />} label="Criptografia AES-256"
                    sx={{ color: "#fff", bgcolor: "rgba(255,255,255,0.12)" }} />
              <Chip icon={<GppGoodOutlinedIcon sx={{ color: "#fff !important" }} />} label="Conformidade LGPD"
                    sx={{ color: "#fff", bgcolor: "rgba(255,255,255,0.12)" }} />
              <Chip icon={<FactCheckOutlinedIcon sx={{ color: "#fff !important" }} />} label="Trilha de auditoria"
                    sx={{ color: "#fff", bgcolor: "rgba(255,255,255,0.12)" }} />
            </Stack>
          </Box>
        </Grid>
      </Grid>
    </Box>
  );
}
