import { useCallback, useEffect, useState } from "react";
import { api } from "./api";
import { Finding, Summary } from "./types";

export interface Findings {
  summary: Summary | null;
  rollups: Finding[];
  verdicts: Finding[];
  deadLetters: Finding[];
  error: string;
  auto: boolean;
  setAuto: (v: boolean) => void;
  refresh: () => void;
}

export function useFindings(enabled: boolean): Findings {
  const [summary, setSummary] = useState<Summary | null>(null);
  const [rollups, setRollups] = useState<Finding[]>([]);
  const [verdicts, setVerdicts] = useState<Finding[]>([]);
  const [deadLetters, setDeadLetters] = useState<Finding[]>([]);
  const [error, setError] = useState("");
  const [auto, setAuto] = useState(true);

  const refresh = useCallback(async () => {
    try {
      const [s, r, v, d] = await Promise.all([api.summary(), api.rollups(500), api.verdicts(200), api.deadLetters(100)]);
      setSummary(s);
      setRollups(r);
      setVerdicts(v);
      setDeadLetters(d);
      setError("");
    } catch (e: any) {
      setError(String(e.message ?? e));
    }
  }, []);

  useEffect(() => {
    if (enabled) refresh();
  }, [enabled, refresh]);

  useEffect(() => {
    if (!enabled || !auto) return;
    const id = setInterval(refresh, 5000);
    return () => clearInterval(id);
  }, [enabled, auto, refresh]);

  return { summary, rollups, verdicts, deadLetters, error, auto, setAuto, refresh };
}
