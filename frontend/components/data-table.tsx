"use client";

import { cn } from "@/lib/utils";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

export interface Column<T> {
  header: string;
  cell: (row: T) => React.ReactNode;
  className?: string;
  headerClassName?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  rows: T[];
  getKey: (row: T) => string | number;
  onRowClick?: (row: T) => void;
}

/** A thin, typed wrapper over the shadcn table primitives for list views. */
export function DataTable<T>({ columns, rows, getKey, onRowClick }: DataTableProps<T>) {
  return (
    <div className="w-full overflow-x-auto rounded-lg border">
      <Table>
        <TableHeader>
          <TableRow>
            {columns.map((col, i) => (
              <TableHead key={i} className={col.headerClassName}>
                {col.header}
              </TableHead>
            ))}
          </TableRow>
        </TableHeader>
        <TableBody>
          {rows.map((row) => (
            <TableRow
              key={getKey(row)}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              className={cn(onRowClick && "cursor-pointer")}
            >
              {columns.map((col, i) => (
                <TableCell key={i} className={col.className}>
                  {col.cell(row)}
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
