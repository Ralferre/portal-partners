import { Typography, Link } from "@mui/material";
import { Button } from "../components/common/Button";
import { InputField } from "../components/common/InputField";
import EmailOutlinedIcon from "@mui/icons-material/EmailOutlined";
import { JSX, useState } from "react";
import { isValidEmail } from "../utils/validators";
import { Auth } from "./Auth";
import api from "../services/api";

export function ForgotPassword(): JSX.Element {
  const [email, setEmail] = useState("");
  const [emailError, setEmailError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitSuccess, setSubmitSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setSubmitError(null);
    setSubmitSuccess(null);

    const res = isValidEmail(email);
    if (!res) {
      setEmailError("Digite um e-mail válido.");
      return;
    }

    try {
      setLoading(true);
      const response = await api.post("/api/auth/forgot-password", { email });
      const msg = typeof response.data === "string" ? response.data : "Email enviado.";
      setSubmitSuccess(msg);
    } catch (err: any) {
      const message = err?.response?.data?.message;
      setSubmitError(message || "Falha ao enviar email");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Auth
      left={
        <>
          <form onSubmit={handleSubmit}>
            <Typography
              variant="h4"
              fontWeight="bold"
              gutterBottom
              align="center"
            >
              Sistema de Gestão Documentação
            </Typography>

            <Typography variant="body1" color="text.secondary" mt={3} mb={3}>
              Insira seu e-mail abaixo para redefini-la.
            </Typography>

            <Typography
              fontSize={12}
              variant="body1"
              color="text.secondary"
              mb={0}
            >
              E-mail
            </Typography>
            <InputField
              id="email"
              value={email}
              required
              onChange={(e) => {
                const val = (e.target as HTMLInputElement).value;
                setEmail(val);
                const res = isValidEmail(val);
                setEmailError(res ? null : "Digite um e-mail válido.");
              }}
              label="Enter your e-mail"
              startIcon={<EmailOutlinedIcon />}
              error={!!emailError}
              helperText={emailError ?? undefined}
            />

            {submitError ? (
              <Typography color="error" mt={1}>
                {submitError}
              </Typography>
            ) : null}

            {submitSuccess ? (
              <Typography color="success.main" mt={1}>
                {submitSuccess}
              </Typography>
            ) : null}

            <Button type="submit" disabled={loading}>
              Enviar
            </Button>

            <Typography textAlign="right" mt={2}>
              <Link href="/login" underline="hover">
                Retornar ao login
              </Link>
            </Typography>
          </form>
        </>
      }
    ></Auth>
  );
}
