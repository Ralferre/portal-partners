import { useEffect, useRef, useState } from "react";
import { Box, LinearProgress, Typography, Button } from "@mui/material";
import axios from "axios";
import { BrandLogo } from "./BrandLogo";
import { RSOFT_COLORS } from "../../theme/theme";

type BootState = "checking" | "ready" | "failed";

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "";
const HEALTH_URL = `${API_BASE}/actuator/health/liveness`;
const MAX_TENTATIVAS = 12; // cobre o cold start (~1min) do plano gratuito do Render.

/**
 * Portao de inicializacao: antes de liberar a aplicacao, confirma que a API
 * respondeu ao health check. Enquanto a instancia "acorda" (cold start do
 * Render free), mostra um estado visual tranquilizador em vez de deixar o
 * usuario achar que travou ou que a autenticacao falhou.
 */
export function BootGate({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<BootState>("checking");
  const [tentativa, setTentativa] = useState(0);
  const cancelado = useRef(false);

  async function verificar() {
    cancelado.current = false;
    setState("checking");

    for (let i = 1; i <= MAX_TENTATIVAS; i++) {
      if (cancelado.current) return;
      setTentativa(i);
      try {
        const resp = await axios.get(HEALTH_URL, { timeout: 8000 });
        if (resp.status === 200) {
          setState("ready");
          return;
        }
      } catch {
        // instancia ainda acordando — espera com backoff e tenta de novo.
      }
      const esperaMs = Math.min(2000 * i, 8000);
      await new Promise((r) => setTimeout(r, esperaMs));
    }
    if (!cancelado.current) setState("failed");
  }

  useEffect(() => {
    // Sem API configurada (ex: dev sem backend), nao bloqueia a UI.
    if (!API_BASE) {
      setState("ready");
      return;
    }
    verificar();
    return () => {
      cancelado.current = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (state === "ready") return <>{children}</>;

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        gap: 3,
        p: 3,
        textAlign: "center",
        background: `linear-gradient(135deg, ${RSOFT_COLORS.navy} 0%, ${RSOFT_COLORS.primaryDark} 100%)`,
        color: "#fff",
      }}
    >
      <BrandLogo size={64} textColor="#fff" />

      {state === "checking" && (
        <>
          <Typography variant="h6" sx={{ fontWeight: 600 }}>
            Iniciando a aplicação com segurança, aguarde...
          </Typography>
          <Typography variant="body2" sx={{ opacity: 0.85, maxWidth: 420 }}>
            Estabelecendo conexão criptografada com o servidor. Na primeira vez
            isso pode levar até um minuto.
          </Typography>
          <Box sx={{ width: 280 }}>
            <LinearProgress
              variant="determinate"
              value={Math.min((tentativa / MAX_TENTATIVAS) * 100, 95)}
              sx={{ borderRadius: 2, height: 6 }}
            />
          </Box>
        </>
      )}

      {state === "failed" && (
        <>
          <Typography variant="h6" sx={{ fontWeight: 600 }}>
            Não foi possível conectar ao servidor
          </Typography>
          <Typography variant="body2" sx={{ opacity: 0.85, maxWidth: 420 }}>
            O servidor pode estar iniciando. Tente novamente em instantes.
          </Typography>
          <Button variant="contained" color="primary" onClick={verificar}>
            Tentar novamente
          </Button>
        </>
      )}
    </Box>
  );
}

export default BootGate;
