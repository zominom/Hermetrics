export type Role = "ENTRY" | "OUTPUT" | "BOTH";
export type CohortMode = "ENTRY_TOPICS" | "ASSUME_ALL";

export interface Rule {
  type: string;
  path: string;
  [key: string]: unknown;
}

export interface RuleSet {
  nullsEqualAbsent?: boolean;
  emptyEqualsAbsent?: boolean;
  rules: Rule[];
}

export interface Topic {
  name: string;
  role: Role;
  format: string;
  guidPath: string;
  sequencePath?: string | string[];
  ruleSet?: string;
}

export interface Policy {
  quietMillis: number;
  maxWaitMillis: number;
  strictIntermediates: boolean;
  cohortMode: CohortMode;
}

export interface CompareConfig {
  policy: Policy;
  ruleSets: Record<string, RuleSet>;
  topics: Topic[];
}

export interface RuleParam {
  name: string;
  kind: string;
  required: boolean;
}

export interface RuleType {
  type: string;
  params: RuleParam[];
}

export interface ActiveConfig {
  active: CompareConfig | null;
  source: string;
  flinkUiUrl: string | null;
}

export interface ValidateResult {
  valid: boolean;
  error?: string;
}

export interface Summary {
  total: number;
  counts: Record<string, number>;
}

export type Finding = Record<string, any>;

export function defaultConfig(): CompareConfig {
  return {
    policy: {
      quietMillis: 300000,
      maxWaitMillis: 3600000,
      strictIntermediates: false,
      cohortMode: "ENTRY_TOPICS",
    },
    ruleSets: { default: { nullsEqualAbsent: true, emptyEqualsAbsent: false, rules: [] } },
    topics: [],
  };
}
