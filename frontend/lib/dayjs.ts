/**
 * Single dayjs instance for the whole app. Import from here, never from "dayjs" directly, so plugins
 * are registered exactly once and date handling stays consistent.
 */
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import localizedFormat from "dayjs/plugin/localizedFormat";
import utc from "dayjs/plugin/utc";

dayjs.extend(relativeTime);
dayjs.extend(localizedFormat);
dayjs.extend(utc);

export default dayjs;

/** e.g. "19 Jul 2026" */
export function formatDate(value?: string | null): string {
  if (!value) return "—";
  return dayjs(value).format("DD MMM YYYY");
}

/** e.g. "19 Jul 2026, 2:30 PM" */
export function formatDateTime(value?: string | null): string {
  if (!value) return "—";
  return dayjs(value).format("DD MMM YYYY, h:mm A");
}

/** e.g. "3 hours ago" */
export function fromNow(value?: string | null): string {
  if (!value) return "—";
  return dayjs(value).fromNow();
}

/** ISO date (YYYY-MM-DD) for LocalDate request fields. */
export function toIsoDate(value?: string | null): string | undefined {
  if (!value) return undefined;
  return dayjs(value).format("YYYY-MM-DD");
}
