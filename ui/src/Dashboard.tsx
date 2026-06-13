import { useEffect, useState } from "react";
import { api } from "./api";
import { Finding, Summary } from "./types";

export function Dashboard({ onIgnorePath }: { onIgnorePath: (path: string) => void }) {
  const [summary, setSummary] = useState<Summary | null>(null);
  const [rollups, setRollups] = useState<Finding[]>([]);
  const [verdicts, setVerdicts] = useState<Finding[]>([]);
  const [deadLetters, setDeadLetters] = useState<Finding[]>([]);
  const [error, setError] = useState("");
  const [auto, setAuto] = useState(true);

  async function refresh() {
    try {
      const [s, r, v, d] = await Promise.all([
        api.summary(),
        api.rollups(100),
        api.verdicts(100),
        api.deadLetters(50),
      ]);
      setSummary(s);
      setRollups(r);
      setVerdicts(v);
      setDeadLetters(d);
      setError("");
    } catch (e: any) {
      setError(String(e.message ?? e));
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  useEffect(() => {
    if (!auto) return;
    const id = setInterval(refresh, 5000);
    return () => clearInterval(id);
  }, [auto]);

  return (
    <>
      {error && <div className="banner err">API: {error}</div>}

      <div className="section">
        <div className="row">
          <h2 style={{ margin: 0, flex: 1 }}>Parity summary</h2>
          <label className="muted">
            <input type="checkbox" style={{ width: "auto" }} checked={auto} onChange={(e) => setAuto(e.target.checked)} /> auto-refresh
          </label>
          <button className="ghost" onClick={refresh}>
            Refresh
          </button>
        </div>
        <div className="summary-grid" style={{ marginTop: 12 }}>
          <div className="stat">
            <div className="n">{summary?.total ?? 0}</div>
            <div className="k">verdicts (recent)</div>
          </div>
          {summary &&
            Object.entries(summary.counts)
              .sort()
              .map(([k, n]) => (
                <div className="stat" key={k}>
                  <div className="n">
                    <span className={`badge ${k}`}>{n}</span>
                  </div>
                  <div className="k">{k}</div>
                </div>
              ))}
        </div>
      </div>

      <div className="section">
        <h2>Diff signatures (rollups)</h2>
        <table>
          <thead>
            <tr>
              <th>topic</th>
              <th>status</th>
              <th>signature</th>
              <th>count</th>
              <th>samples</th>
            </tr>
          </thead>
          <tbody>
            {rollups.length === 0 && (
              <tr>
                <td colSpan={5} className="muted">
                  no rollups yet
                </td>
              </tr>
            )}
            {rollups.map((r, i) => (
              <tr key={i}>
                <td className="mono">{r.topic}</td>
                <td>
                  <span className={`badge ${r.status}`}>{r.status}</span>
                </td>
                <td>
                  {(r.signature ?? []).map((s: string, si: number) => (
                    <div key={si} className="row" style={{ gap: 6 }}>
                      <span className="mono">{s}</span>
                      {r.status === "DIFF" && (
                        <button className="ghost tiny" title="add ignore rule for this path" onClick={() => onIgnorePath(pathOf(s))}>
                          ignore
                        </button>
                      )}
                    </div>
                  ))}
                </td>
                <td>
                  <strong>{r.count}</strong>
                </td>
                <td>
                  {(r.sampleGuids ?? []).map((g: string) => (
                    <span key={g} className="pill mono">
                      {g}
                    </span>
                  ))}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="section">
        <h2>Recent verdicts</h2>
        <table>
          <thead>
            <tr>
              <th>guid</th>
              <th>topic</th>
              <th>status</th>
              <th>rev</th>
              <th>signature</th>
            </tr>
          </thead>
          <tbody>
            {verdicts.length === 0 && (
              <tr>
                <td colSpan={5} className="muted">
                  no verdicts yet
                </td>
              </tr>
            )}
            {verdicts.map((v, i) => (
              <tr key={i}>
                <td className="mono">{v.guid}</td>
                <td className="mono">{v.topic}</td>
                <td>
                  <span className={`badge ${v.status}`}>{v.status}</span>
                </td>
                <td>{v.revision}</td>
                <td className="mono muted">{v.signatureId ?? ""}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="section">
        <h2>Dead letters</h2>
        <table>
          <thead>
            <tr>
              <th>env</th>
              <th>topic</th>
              <th>error</th>
            </tr>
          </thead>
          <tbody>
            {deadLetters.length === 0 && (
              <tr>
                <td colSpan={3} className="muted">
                  none
                </td>
              </tr>
            )}
            {deadLetters.map((d, i) => (
              <tr key={i}>
                <td>{d.env}</td>
                <td className="mono">{d.topic}</td>
                <td className="mono">{d.error}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

function pathOf(signatureEntry: string): string {
  const idx = signatureEntry.lastIndexOf(":");
  return idx > 0 ? signatureEntry.slice(0, idx).trim() : signatureEntry.trim();
}
