import { ActiveConfig, CompareConfig, Finding, RuleType, Summary, ValidateResult } from "./types";

async function get<T>(url: string): Promise<T> {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`${res.status}: ${await res.text()}`);
  return res.json();
}

async function post<T>(url: string, body: unknown): Promise<T> {
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`${res.status}: ${await res.text()}`);
  return res.json();
}

export const api = {
  ruleTypes: () => get<RuleType[]>("/api/rule-types"),
  activeConfig: () => get<ActiveConfig>("/api/config/active"),
  validate: (config: CompareConfig) => post<ValidateResult>("/api/config/validate", config),
  apply: (config: CompareConfig) => post<{ applied: boolean }>("/api/config/apply", config),
  verdicts: (limit = 100) => get<Finding[]>(`/api/verdicts?limit=${limit}`),
  rollups: (limit = 100) => get<Finding[]>(`/api/rollups?limit=${limit}`),
  deadLetters: (limit = 100) => get<Finding[]>(`/api/dead-letters?limit=${limit}`),
  summary: () => get<Summary>("/api/summary"),
};
