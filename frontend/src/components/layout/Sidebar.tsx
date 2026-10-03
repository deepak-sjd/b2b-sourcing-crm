import { NavLink } from "react-router-dom";
import {
  LayoutDashboard,
  Users2,
  Archive,
  Contact2,
  Building2,
  Truck,
  ClipboardList,
  CheckSquare,
  Activity,
  MessageCircle,
  BarChart3,
  UserCog,
  Users,
  Workflow,
  ScrollText,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/store/authStore";

interface NavItem {
  to: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
}

const MAIN_NAV: NavItem[] = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/leads", label: "Leads", icon: Users2 },
  { to: "/leads/scrap", label: "Scrap Records", icon: Archive },
  { to: "/contacts", label: "Contacts", icon: Contact2 },
  { to: "/customers", label: "Customers", icon: Building2 },
  { to: "/requirements", label: "Requirements", icon: ClipboardList },
  { to: "/suppliers", label: "Suppliers", icon: Truck },
  { to: "/tasks", label: "Tasks", icon: CheckSquare },
  { to: "/activities", label: "Activities", icon: Activity },
  { to: "/whatsapp", label: "WhatsApp", icon: MessageCircle },
  { to: "/reports", label: "Reports", icon: BarChart3 },
];

const ADMIN_NAV: NavItem[] = [
  { to: "/users", label: "Users", icon: UserCog },
  { to: "/teams", label: "Teams", icon: Users },
  { to: "/automation", label: "Automation", icon: Workflow },
  { to: "/audit-log", label: "Audit Log", icon: ScrollText },
];

function NavLinkItem({ item }: { item: NavItem }) {
  const Icon = item.icon;
  return (
    <NavLink
      to={item.to}
      end={item.to === "/leads"}
      className={({ isActive }) =>
        cn(
          "flex items-center gap-2.5 rounded-md px-3 py-2 text-sm font-medium transition-colors",
          isActive
            ? "bg-accent text-accent-foreground"
            : "text-sidebar-foreground hover:bg-accent/60 hover:text-accent-foreground",
        )
      }
    >
      <Icon className="h-4 w-4 shrink-0" />
      {item.label}
    </NavLink>
  );
}

export function Sidebar() {
  const role = useAuthStore((s) => s.user?.role);
  const canSeeAdmin = role === "SUPER_ADMIN" || role === "ADMIN";

  return (
    <aside className="flex h-full w-60 shrink-0 flex-col border-r border-sidebar-border bg-sidebar">
      <div className="flex h-14 items-center gap-2 border-b border-sidebar-border px-4">
        <div className="flex h-7 w-7 items-center justify-center rounded-md bg-primary text-sm font-bold text-primary-foreground">
          D
        </div>
        <span className="text-[15px] font-semibold tracking-tight">
          DocTutorials <span className="text-muted-foreground font-normal">CRM</span>
        </span>
      </div>

      <nav className="flex-1 space-y-0.5 overflow-y-auto p-3 thin-scrollbar">
        {MAIN_NAV.map((item) => (
          <NavLinkItem key={item.to} item={item} />
        ))}

        {canSeeAdmin && (
          <>
            <p className="mb-1 mt-5 px-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Admin
            </p>
            {ADMIN_NAV.map((item) => (
              <NavLinkItem key={item.to} item={item} />
            ))}
          </>
        )}
      </nav>
    </aside>
  );
}