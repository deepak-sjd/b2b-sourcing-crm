import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";

export function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-muted/40 p-4 text-center">
      <p className="text-5xl font-bold text-muted-foreground">404</p>
      <p className="text-lg font-medium">Page not found</p>
      <Button asChild>
        <Link to="/dashboard">Back to dashboard</Link>
      </Button>
    </div>
  );
}
