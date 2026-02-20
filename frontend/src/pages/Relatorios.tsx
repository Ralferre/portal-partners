import { useEffect, useState } from "react";
import { Box, Card, CardContent, Typography, Alert } from "@mui/material";
import { AppLayout } from "./AppLayout";
import api from "../services/api";

type ReportDashboard = {
  totalPostados: number;
  totalAnalisados: number;
  totalAprovados: number;
  totalReprovados: number;
  totalPendentes: number;
};

export function Relatorios() {
  const [data, setData] = useState<ReportDashboard | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    load();
  }, []);

  const load = async () => {
    setError("");
    try {
      const response = await api.get<ReportDashboard>("/api/report/dashboard");
      setData(response.data);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao carregar relatório");
    }
  };

  const StatCard = ({ title, value }: { title: string; value: number }) => (
    <Card sx={{ flex: "1 1 220px" }}>
      <CardContent>
        <Typography color="textSecondary" gutterBottom variant="overline">
          {title}
        </Typography>
        <Typography variant="h4" component="div">
          {value}
        </Typography>
      </CardContent>
    </Card>
  );

  return (
    <AppLayout>
      <Box>
        <Typography variant="h4" fontWeight="bold" sx={{ mb: 3 }}>
          Relatórios
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError("")}>
            {error}
          </Alert>
        )}

        <Box sx={{ display: "flex", gap: 2, flexWrap: "wrap" }}>
          <StatCard title="Total Postados" value={data?.totalPostados ?? 0} />
          <StatCard title="Pendentes" value={data?.totalPendentes ?? 0} />
          <StatCard title="Analisados" value={data?.totalAnalisados ?? 0} />
          <StatCard title="Aprovados" value={data?.totalAprovados ?? 0} />
          <StatCard title="Reprovados" value={data?.totalReprovados ?? 0} />
        </Box>
      </Box>
    </AppLayout>
  );
}
