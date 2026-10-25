import * as Sentry from "@sentry/react";

/**
 * Inicializa o Sentry no frontend.
 *
 * - DSN vem de VITE_SENTRY_DSN (gravado no bundle em build). Sem DSN, nao inicializa.
 * - Sampling de performance e replay em 0: plano Developer (free) nao e consumido;
 *   capturamos apenas erros.
 * - sendDefaultPii=false e um beforeSend que remove CPF, e-mail e tokens antes
 *   de qualquer envio, alinhado a LGPD.
 */
const CPF = /\b\d{3}\.?\d{3}\.?\d{3}-?\d{2}\b/g;
const EMAIL = /[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g;
const BEARER = /bearer\s+[A-Za-z0-9._-]+/gi;

export function maskPii(input: string): string {
  return input
    .replace(CPF, "[CPF_REMOVIDO]")
    .replace(EMAIL, "[EMAIL_REMOVIDO]")
    .replace(BEARER, "Bearer [TOKEN_REMOVIDO]");
}

export function initObservability(): void {
  const dsn = import.meta.env.VITE_SENTRY_DSN as string | undefined;
  if (!dsn) return;

  Sentry.init({
    dsn,
    environment: (import.meta.env.VITE_SENTRY_ENVIRONMENT as string) ?? "production",
    tracesSampleRate: 0,
    replaysSessionSampleRate: 0,
    replaysOnErrorSampleRate: 0,
    sendDefaultPii: false,
    beforeSend(event) {
      // Nunca enviar dados do usuario.
      if (event.user) event.user = {};
      if (event.message) event.message = maskPii(event.message);
      if (event.exception?.values) {
        event.exception.values = event.exception.values.map((v) => ({
          ...v,
          value: v.value ? maskPii(v.value) : v.value,
        }));
      }
      if (event.request) {
        delete event.request.cookies;
        delete event.request.headers;
      }
      return event;
    },
  });
}
