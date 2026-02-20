import { AppLayout } from "./AppLayout";
import { useAuth } from "../contexts/AuthContext";
import { AdminDashboard } from "../components/dashboards/AdminDashboard";
import { ContratanteDashboard } from "../components/dashboards/ContratanteDashboard";
import { ContratadaDashboard } from "../components/dashboards/ContratadaDashboard";

export function Dashboard() {
  const { user } = useAuth();

  const renderDashboard = () => {
    switch (user?.role) {
      case "ADMIN":
        return <AdminDashboard />;
      case "CONTRATANTE":
        return <ContratanteDashboard />;
      case "CONTRATADA":
        return <ContratadaDashboard />;
      default:
        return null;
    }
  };

  return <AppLayout>{renderDashboard()}</AppLayout>;
}
