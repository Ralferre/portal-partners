import { Box, Typography } from "@mui/material";

export type DailyDocumentStats = {
  date: string; // YYYY-MM-DD
  postados: number;
  aprovados: number;
  pendentes: number;
};

type Props = {
  title: string;
  data: DailyDocumentStats[];
};

const COLORS = {
  postados: "#1b6c72ff",
  aprovados: "#2e7d32",
  pendentes: "#ed6c02",
};

function formatDayLabel(dateStr: string) {
  const [y, m, d] = dateStr.split("-").map((x) => Number(x));
  if (!y || !m || !d) return dateStr;
  const dt = new Date(Date.UTC(y, m - 1, d));
  return dt.toLocaleDateString("pt-BR", { weekday: "short", day: "2-digit" });
}

export function SevenDaysDocumentBarChart({ title, data }: Props) {
  const max = Math.max(
    1,
    ...data.map((x) => Math.max(x.postados ?? 0, x.aprovados ?? 0, x.pendentes ?? 0))
  );

  const barHeight = (value: number) => `${Math.round(((value ?? 0) / max) * 110) + 6}px`;

  return (
    <Box>
      <Typography variant="h6" sx={{ mb: 2 }}>
        {title}
      </Typography>

      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: `repeat(${Math.max(data.length, 1)}, 1fr)`,
          gap: 2,
          alignItems: "end",
        }}
      >
        {data.map((d) => (
          <Box key={d.date} sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
            <Box
              sx={{
                display: "grid",
                gridTemplateColumns: "repeat(3, 1fr)",
                gap: 0.75,
                alignItems: "end",
              }}
            >
              <Box
                sx={{
                  height: barHeight(d.postados),
                  backgroundColor: COLORS.postados,
                  borderRadius: 1,
                }}
                title={`Postados: ${d.postados}`}
              />
              <Box
                sx={{
                  height: barHeight(d.pendentes),
                  backgroundColor: COLORS.pendentes,
                  borderRadius: 1,
                }}
                title={`Pendentes: ${d.pendentes}`}
              />
              <Box
                sx={{
                  height: barHeight(d.aprovados),
                  backgroundColor: COLORS.aprovados,
                  borderRadius: 1,
                }}
                title={`Aprovados: ${d.aprovados}`}
              />
            </Box>

            <Typography variant="caption" sx={{ textAlign: "center" }}>
              {formatDayLabel(d.date)}
            </Typography>
          </Box>
        ))}
      </Box>

      <Box sx={{ display: "flex", gap: 2, mt: 2, flexWrap: "wrap" }}>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          <Box sx={{ width: 12, height: 12, backgroundColor: COLORS.postados, borderRadius: 0.5 }} />
          <Typography variant="caption">Postados</Typography>
        </Box>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          <Box sx={{ width: 12, height: 12, backgroundColor: COLORS.pendentes, borderRadius: 0.5 }} />
          <Typography variant="caption">Pendentes</Typography>
        </Box>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          <Box sx={{ width: 12, height: 12, backgroundColor: COLORS.aprovados, borderRadius: 0.5 }} />
          <Typography variant="caption">Aprovados</Typography>
        </Box>
      </Box>
    </Box>
  );
}
