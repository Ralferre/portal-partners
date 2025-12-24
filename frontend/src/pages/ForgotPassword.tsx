import { Typography, Link } from "@mui/material";
import { Button } from "../components/common/Button";
import { InputField } from "../components/common/InputField";
import EmailOutlinedIcon from "@mui/icons-material/EmailOutlined";
import { JSX, useState } from "react";
import { isValidEmail } from "../utils/validators";
import { Auth } from "./Auth";

export function ForgotPassword(): JSX.Element {
  const [email, setEmail] = useState("");
  const [emailError, setEmailError] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const res = isValidEmail(email);
    if (!res) {
      setEmailError("Digite um e-mail válido.");
      return;
    }
    console.log("Email testado:", email);
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

            <Button type="submit">Enviar</Button>

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
