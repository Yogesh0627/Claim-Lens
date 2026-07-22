import { AlertCircle, Inbox } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

export function LoadingRows({ rows = 5 }: { rows?: number }) {
  return (
    <div className="space-y-2">
      {Array.from({ length: rows }).map((_, i) => (
        <Skeleton key={i} className="h-12 w-full" />
      ))}
    </div>
  );
}

export function ErrorState({ message }: { message: string }) {
  return (
    <Alert variant="destructive">
      <AlertCircle className="h-4 w-4" />
      <AlertTitle>Something went wrong</AlertTitle>
      <AlertDescription>{message}</AlertDescription>
    </Alert>
  );
}

export function EmptyState({
  title = "Nothing here yet",
  description,
  action,
}: {
  title?: string;
  description?: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 text-center">
      <Inbox className="text-muted-foreground mb-3 h-10 w-10" />
      <p className="font-medium">{title}</p>
      {description ? (
        <p className="text-muted-foreground mt-1 max-w-sm text-sm">{description}</p>
      ) : null}
      {action ? <div className="mt-4">{action}</div> : null}
    </div>
  );
}

/** Chooses loading / error / empty / content based on the useAsync-style state. */
export function DataState<T>({
  loading,
  error,
  data,
  children,
  emptyWhen,
  empty,
}: {
  loading: boolean;
  error: string | null;
  data: T | null;
  children: (data: T) => React.ReactNode;
  emptyWhen?: (data: T) => boolean;
  empty?: React.ReactNode;
}) {
  if (loading) return <LoadingRows />;
  if (error) return <ErrorState message={error} />;
  if (data == null) return <ErrorState message="No data" />;
  if (emptyWhen?.(data)) return <>{empty ?? <EmptyState />}</>;
  return <>{children(data)}</>;
}
