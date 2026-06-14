import { Finding } from "./types";

export interface VolumePoint {
  time: string;
  diff: number;
  other: number;
}

export function diffVolumeSeries(rollups: Finding[]): VolumePoint[] {
  const byWindow = new Map<number, { diff: number; other: number }>();
  for (const r of rollups) {
    const window = Number(r.windowStartMillis);
    if (!Number.isFinite(window)) continue;
    const bucket = byWindow.get(window) ?? { diff: 0, other: 0 };
    const count = Number(r.count) || 0;
    if (r.status === "DIFF") bucket.diff += count;
    else bucket.other += count;
    byWindow.set(window, bucket);
  }
  return [...byWindow.entries()]
    .sort((a, b) => a[0] - b[0])
    .map(([window, b]) => ({
      time: new Date(window).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
      diff: b.diff,
      other: b.other,
    }));
}

export interface TopicRate {
  topic: string;
  total: number;
  diff: number;
  diffPct: number;
}

export function topicDiffRates(rollups: Finding[]): TopicRate[] {
  const byTopic = new Map<string, { total: number; diff: number }>();
  for (const r of rollups) {
    const topic = String(r.topic ?? "");
    if (!topic) continue;
    const bucket = byTopic.get(topic) ?? { total: 0, diff: 0 };
    const count = Number(r.count) || 0;
    bucket.total += count;
    if (r.status === "DIFF") bucket.diff += count;
    byTopic.set(topic, bucket);
  }
  return [...byTopic.entries()]
    .map(([topic, b]) => ({
      topic,
      total: b.total,
      diff: b.diff,
      diffPct: b.total > 0 ? Math.round((b.diff / b.total) * 100) : 0,
    }))
    .sort((a, b) => b.diffPct - a.diffPct || b.total - a.total);
}
