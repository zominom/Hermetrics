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

function topicParam(topic?: string): string {
  return topic ? `&topic=${encodeURIComponent(topic)}` : "";
}

export const api = {
  ruleTypes: () => get<RuleType[]>("/api/rule-types"),
  activeConfig: () => get<ActiveConfig>("/api/config/active"),
  validate: (config: CompareConfig) => post<ValidateResult>("/api/config/validate", config),
  apply: (config: CompareConfig) => post<{ applied: boolean }>("/api/config/apply", config),
  verdicts: (limit = 100, topic?: string) => get<Finding[]>(`/api/verdicts?limit=${limit}${topicParam(topic)}`),
  rollups: (limit = 100, topic?: string) => get<Finding[]>(`/api/rollups?limit=${limit}${topicParam(topic)}`),
  deadLetters: (limit = 100, topic?: string) => get<Finding[]>(`/api/dead-letters?limit=${limit}${topicParam(topic)}`),
  summary: (topic?: string) => get<Summary>(`/api/summary${topic ? `?topic=${encodeURIComponent(topic)}` : ""}`),
};
