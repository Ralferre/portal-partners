import { AppCard } from "../components/common/AppCard";
import { AppTable } from "../components/common/AppTable";
import { AppLayout } from "./AppLayout";

export function ReportFiles() {
  return (
    <AppLayout
      children={
        <>
          <AppTable />
        </>
      }
    />
  );
}
