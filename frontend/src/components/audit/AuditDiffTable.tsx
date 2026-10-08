import { parseValueMap } from "@/types/auditLog";

interface AuditDiffTableProps {
  changedFields: string | null;
  oldValues: string | null;
  newValues: string | null;
}

/** Small "Previous -> Updated" table, one row per changed field. Renders
 *  nothing if there's no diff (e.g. a plain CREATE with no logChange data). */
export function AuditDiffTable({ changedFields, oldValues, newValues }: AuditDiffTableProps) {
  const fields = (changedFields ?? "").split(",").map((f) => f.trim()).filter(Boolean);
  if (fields.length === 0) return null;

  const before = parseValueMap(oldValues);
  const after = parseValueMap(newValues);

  return (
    <table className="w-full text-xs">
      <thead>
        <tr className="text-left text-muted-foreground">
          <th className="py-1 pr-3 font-medium">Field</th>
          <th className="py-1 pr-3 font-medium">Previous</th>
          <th className="py-1 font-medium">Updated</th>
        </tr>
      </thead>
      <tbody>
        {fields.map((field) => (
          <tr key={field} className="border-t border-border/60">
            <td className="py-1 pr-3 font-medium text-foreground">{field}</td>
            <td className="py-1 pr-3 text-muted-foreground">{before[field] ?? <em>empty</em>}</td>
            <td className="py-1 text-foreground">{after[field] ?? <em>empty</em>}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}