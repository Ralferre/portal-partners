import { Box, Typography } from "@mui/material";
import { RSOFT_COLORS } from "../../theme/theme";

type BrandLogoProps = {
  /** Altura do icone em px. */
  size?: number;
  /** Mostra o texto "Portal Partners" + assinatura RSOFT ao lado do icone. */
  showText?: boolean;
  /** Cor do texto (default herda). Em fundos escuros use "#fff". */
  textColor?: string;
  /** Assinatura "by RSOFT" abaixo do nome do produto. */
  showSignature?: boolean;
};

/**
 * Marca do Portal Partners (produto RSOFT).
 *
 * Icone: "R" estilizado com corte dinamico, em degrade azul RSOFT
 * (#007BFF -> #0052CC), recriado como SVG limpo (vetor, escala sem perda).
 */
export function BrandLogo({
  size = 36,
  showText = true,
  textColor,
  showSignature = true,
}: BrandLogoProps) {
  return (
    <Box sx={{ display: "flex", alignItems: "center", gap: 1.2 }}>
      <svg
        width={size}
        height={size}
        viewBox="0 0 48 48"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        role="img"
        aria-label="Logo RSOFT"
      >
        <defs>
          <linearGradient id="rsoftGrad" x1="0" y1="0" x2="48" y2="48" gradientUnits="userSpaceOnUse">
            <stop stopColor={RSOFT_COLORS.primary} />
            <stop offset="1" stopColor={RSOFT_COLORS.primaryDark} />
          </linearGradient>
        </defs>
        <rect width="48" height="48" rx="12" fill={RSOFT_COLORS.navy} />
        {/* "R" estilizado com corte dinamico */}
        <path
          d="M14 10h12.5c5 0 8.5 3.1 8.5 7.8 0 3.6-2.1 6.3-5.5 7.4L36 38h-6.6l-5.2-11.3H20V38h-6V10zm6 5.2v6.6h6c2.3 0 3.8-1.3 3.8-3.3S28.3 15.2 26 15.2h-6z"
          fill="url(#rsoftGrad)"
        />
        {/* traco de velocidade */}
        <path d="M12 40l10-6" stroke={RSOFT_COLORS.primary} strokeWidth="2.5" strokeLinecap="round" />
      </svg>

      {showText && (
        <Box sx={{ lineHeight: 1 }}>
          <Typography
            component="span"
            sx={{ fontWeight: 800, fontSize: size * 0.46, color: textColor, display: "block", letterSpacing: "-0.3px" }}
          >
            Portal Partners
          </Typography>
          {showSignature && (
            <Typography
              component="span"
              sx={{ fontSize: size * 0.28, opacity: 0.8, color: textColor, letterSpacing: "1px" }}
            >
              by RSOFT
            </Typography>
          )}
        </Box>
      )}
    </Box>
  );
}

export default BrandLogo;
