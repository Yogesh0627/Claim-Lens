/**
 * Renders the backend validation message for a single field, inline under the input. Pass the
 * `fieldErrors` map from a `useMutation` result; the `name` must match the backend DTO field (which is
 * also the form field name). Renders nothing when there's no error for that field.
 */
export function FieldError({
  name,
  errors,
}: {
  name: string;
  errors?: Record<string, string>;
}) {
  const message = errors?.[name];
  if (!message) return null;
  return <p className="text-destructive text-xs">{message}</p>;
}
