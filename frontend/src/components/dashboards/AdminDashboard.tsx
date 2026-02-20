import { useState, useEffect } from "react";
import { Box, Card, CardContent, Typography, Grid, Paper } from "@mui/material";
import {
  Description as DocumentIcon,
  Business as BusinessIcon,
  People as PeopleIcon,
  CheckCircle as ApprovedIcon,
  Warning as PendingIcon,
  Cancel as RejectedIcon,
} from "@mui/icons-material";
import api from "../../services/api";
import {
  SevenDaysDocumentBarChart,
  type DailyDocumentStats,
} from "../common/SevenDaysDocumentBarChart";

type DashboardStats = {
  totalDocumentos: number;
  documentosAprovados: number;
  documentosPendentes: number;
  documentosReprovados: number;
  totalContratadas: number;
  totalFuncionarios: number;
};

export function AdminDashboard() {
  const [stats, setStats] = useState<DashboardStats>({
    totalDocumentos: 0,
    documentosAprovados: 0,
    documentosPendentes: 0,
    documentosReprovados: 0,
    totalContratadas: 0,
    totalFuncionarios: 0,
  });

  const [chartData, setChartData] = useState<DailyDocumentStats[]>([]);

  useEffect(() => {
    loadStats();
    loadChart();
  }, []);

  const loadStats = async () => {
    try {
      const [docsResponse] = await Promise.all([
        api.get("/api/documentos?size=1000"),
      ]);

      const docs = docsResponse.data.content || docsResponse.data;

      setStats({
        totalDocumentos: docs.length,
        documentosAprovados: docs.filter((d: any) => d.statusDocumento === "APROVADO").length,
        documentosPendentes: docs.filter((d: any) => d.statusDocumento === "PENDENTE").length,
        documentosReprovados: docs.filter((d: any) => d.statusDocumento === "REPROVADO").length,
        totalContratadas: 0,
        totalFuncionarios: 0,
      });
    } catch (err) {
      console.error("Erro ao carregar estatísticas:", err);
    }
  };

  const loadChart = async () => {
    try {
      const resp = await api.get<DailyDocumentStats[]>("/api/report/documentos/ultimos-7-dias");
      setChartData(resp.data ?? []);
    } catch (err) {
      console.error("Erro ao carregar gráfico:", err);
    }
  };

  const StatCard = ({ title, value, icon, color }: any) => (
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <Box>
            <Typography color="textSecondary" gutterBottom variant="overline">
              {title}
            </Typography>
            <Typography variant="h4" component="div">
              {value}
            </Typography>
          </Box>
          <Box
            sx={{
              backgroundColor: color,
              borderRadius: "50%",
              width: 56,
              height: 56,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            {icon}
          </Box>
        </Box>
      </CardContent>
    </Card>
  );

  return (
    <Box>
      <Typography variant="h4" fontWeight="bold" sx={{ mb: 3 }}>
        Dashboard Administrativo
      </Typography>

      <Box sx={{ display: "flex", gap: 2, flexWrap: "wrap", mb: 3 }}>
        <Box sx={{ flex: "1 1 250px" }}>
          <StatCard
            title="Total de Documentos"
            value={stats.totalDocumentos}
            icon={<DocumentIcon sx={{ color: "#fff", fontSize: 30 }} />}
            color="#1976d2"
          />
        </Box>
        <Box sx={{ flex: "1 1 250px" }}>
          <StatCard
            title="Aprovados"
            value={stats.documentosAprovados}
            icon={<ApprovedIcon sx={{ color: "#fff", fontSize: 30 }} />}
            color="#2e7d32"
          />
        </Box>
        <Box sx={{ flex: "1 1 250px" }}>
          <StatCard
            title="Pendentes"
            value={stats.documentosPendentes}
            icon={<PendingIcon sx={{ color: "#fff", fontSize: 30 }} />}
            color="#ed6c02"
          />
        </Box>
        <Box sx={{ flex: "1 1 250px" }}>
          <StatCard
            title="Reprovados"
            value={stats.documentosReprovados}
            icon={<RejectedIcon sx={{ color: "#fff", fontSize: 30 }} />}
            color="#d32f2f"
          />
        </Box>
      </Box>

      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 2 }}>
          Visão Geral do Sistema
        </Typography>
        <Typography variant="body1" color="textSecondary">
          Bem-vindo ao painel administrativo. Aqui você tem acesso completo a todos os recursos do sistema.
        </Typography>
      </Paper>

      <Paper sx={{ p: 3, mt: 3 }}>
        <SevenDaysDocumentBarChart
          title="Documentos (últimos 7 dias)"
          data={chartData}
        />
      </Paper>
    </Box>
  );
}
