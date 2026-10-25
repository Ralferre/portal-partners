import { AppRoutes } from "./routes/AppRoutes";
import { BootGate } from "./components/common/BootGate";

export default function App() {
  return (
    <BootGate>
      <AppRoutes />
    </BootGate>
  );
}
