import { AppLayout } from "./AppLayout";
import { AppTable } from "../components/common/AppTable";
import { AppCard } from "../components/common/AppCard";

export function Dashboard() {
  return (
    <AppLayout
      children={
        <>
          <AppCard />
          <AppTable />
        </>
      }
    />
  );
}
