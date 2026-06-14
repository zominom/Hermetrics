import { createTheme } from "@mantine/core";

export const theme = createTheme({
  primaryColor: "cyan",
  defaultRadius: "md",
  fontFamilyMonospace: "ui-monospace, SFMono-Regular, Menlo, monospace",
});

const STATUS_COLOR: Record<string, string> = {
  EQUAL: "green",
  OK: "green",
  EQUAL_DIVERGED: "blue",
  INFO: "blue",
  NOT_MIRRORED: "blue",
  TEST_TRAFFIC: "grape",
  EXTRA_IN_LOAD: "yellow",
  UNANCHORED: "yellow",
  WARN: "yellow",
  DIFF: "red",
  MISSING_IN_LOAD: "red",
  ERROR: "red",
};

export function statusColor(status: string): string {
  return STATUS_COLOR[status] ?? "gray";
}
