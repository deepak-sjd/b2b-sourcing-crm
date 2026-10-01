import { useEffect, useMemo, useRef, useState } from "react";
import { Check, ChevronsUpDown, X } from "lucide-react";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

export interface SearchSelectOption {
  value: string;
  label: string;
  sublabel?: string;
}

interface SearchSelectProps {
  options: SearchSelectOption[];
  value: string | null;
  onChange: (value: string | null) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
}

/**
 * A minimal client-side searchable dropdown - there's no combobox/popover
 * primitive in this project's component set yet, and pulling in a new
 * dependency for one field felt heavier than ~60 lines of plain React.
 * Filters the options it's given locally, so it's meant for lists in the
 * hundreds (customers, suppliers), not tens of thousands - swap to a
 * server-side search endpoint if that ever stops being true.
 */
export function SearchSelect({ options, value, onChange, placeholder, disabled, className }: SearchSelectProps) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const containerRef = useRef<HTMLDivElement>(null);

  const selected = options.find((o) => o.value === value) ?? null;

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
        setQuery("");
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return options.slice(0, 50);
    return options
      .filter((o) => o.label.toLowerCase().includes(q) || o.sublabel?.toLowerCase().includes(q))
      .slice(0, 50);
  }, [options, query]);

  return (
    <div className={cn("relative", className)} ref={containerRef}>
      <div className="relative">
        <Input
          value={open ? query : (selected?.label ?? "")}
          placeholder={placeholder}
          disabled={disabled}
          onFocus={() => {
            setOpen(true);
            setQuery("");
          }}
          onChange={(e) => setQuery(e.target.value)}
          className="pr-14"
        />
        <div className="absolute right-2 top-1/2 flex -translate-y-1/2 items-center gap-1">
          {selected && !disabled && (
            <button
              type="button"
              tabIndex={-1}
              className="rounded p-0.5 text-muted-foreground hover:bg-accent"
              onClick={() => {
                onChange(null);
                setQuery("");
              }}
              aria-label="Clear"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          )}
          <ChevronsUpDown className="h-3.5 w-3.5 text-muted-foreground" />
        </div>
      </div>

      {open && !disabled && (
        <div className="absolute z-50 mt-1 max-h-64 w-full min-w-[220px] overflow-y-auto rounded-md border border-border bg-popover p-1 shadow-md">
          {filtered.length === 0 ? (
            <p className="px-2 py-3 text-center text-xs text-muted-foreground">No matches</p>
          ) : (
            filtered.map((o) => (
              <button
                key={o.value}
                type="button"
                className="flex w-full items-center justify-between gap-2 rounded-sm px-2 py-1.5 text-left text-sm hover:bg-accent"
                onClick={() => {
                  onChange(o.value);
                  setOpen(false);
                  setQuery("");
                }}
              >
                <span>
                  {o.label}
                  {o.sublabel && <span className="text-xs text-muted-foreground"> · {o.sublabel}</span>}
                </span>
                {o.value === value && <Check className="h-3.5 w-3.5 shrink-0 text-primary" />}
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
}
