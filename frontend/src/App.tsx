import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import { AppLayout } from "@/components/layout/AppLayout";
import { ProtectedRoute } from "@/routes/ProtectedRoute";

import { LoginPage } from "@/pages/LoginPage";
import { RegisterPage } from "@/pages/RegisterPage";
import { DashboardPage } from "@/pages/DashboardPage";
import { LeadsPage } from "@/pages/LeadsPage";
import { LeadDetailPage } from "@/pages/LeadDetailPage";
import { ScrapPage } from "@/pages/ScrapPage";
import { ContactsPage } from "@/pages/ContactsPage";
import { CustomersPage } from "@/pages/CustomersPage";
import { CustomerDetailPage } from "@/pages/CustomerDetailPage";
import { RequirementsPage } from "@/pages/RequirementsPage";
import { SuppliersPage } from "@/pages/SuppliersPage";
import { TasksPage } from "@/pages/TasksPage";
import { ActivitiesPage } from "@/pages/ActivitiesPage";
import { ReportsPage } from "@/pages/ReportsPage";
import { QuotationsPage } from "@/pages/QuotationsPage";
import { QuotationBuilderPage } from "@/pages/QuotationBuilderPage";
import { UsersPage } from "@/pages/UsersPage";
import { TeamsPage } from "@/pages/TeamsPage";
import { AuditLogPage } from "@/pages/AuditLogPage";
import { AutomationPage } from "@/pages/AutomationPage";
import { WhatsAppPage } from "@/pages/WhatsAppPage";
import { NotFoundPage } from "@/pages/NotFoundPage";

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* Authenticated */}
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />

            <Route path="/leads" element={<LeadsPage />} />
            <Route path="/leads/scrap" element={<ScrapPage />} />
            <Route path="/leads/:leadId" element={<LeadDetailPage />} />

            <Route path="/contacts" element={<ContactsPage />} />
            <Route path="/customers" element={<CustomersPage />} />
            <Route path="/customers/:customerId" element={<CustomerDetailPage />} />

            <Route path="/requirements" element={<RequirementsPage />} />
            <Route path="/suppliers" element={<SuppliersPage />} />
            <Route path="/tasks" element={<TasksPage />} />
            <Route path="/activities" element={<ActivitiesPage />} />
            <Route path="/reports" element={<ReportsPage />} />

            <Route path="/quotations" element={<QuotationsPage />} />
            <Route path="/quotations/new" element={<QuotationBuilderPage />} />
            <Route path="/quotations/:quotationId" element={<QuotationBuilderPage />} />

            <Route path="/automation" element={<AutomationPage />} />
            <Route path="/whatsapp" element={<WhatsAppPage />} />

            {/* Admin only */}
            <Route element={<ProtectedRoute allowedRoles={["SUPER_ADMIN", "ADMIN"]} />}>
              <Route path="/users" element={<UsersPage />} />
              <Route path="/teams" element={<TeamsPage />} />
              <Route path="/audit-log" element={<AuditLogPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
