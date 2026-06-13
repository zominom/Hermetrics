import { useEffect, useState } from "react";
import { api } from "./api";
import { CohortMode, CompareConfig, Role, RuleType } from "./types";
import { Help, HELP } from "./Help";

export function Editor({
  config,
  ruleTypes,
  onChange,
}: {
  config: CompareConfig;
  ruleTypes: RuleType[];
  onChange: (config: CompareConfig) => void;
}) {
  const [validateMsg, setValidateMsg] = useState<{ ok: boolean; text: string } | null>(null);
  const [applyMsg, setApplyMsg] = useState<{ ok: boolean; text: string } | null>(null);

  function update(mut: (config: CompareConfig) => void) {
    const next = structuredClone(config);
    mut(next);
    onChange(next);
  }

  async function validate() {
    setApplyMsg(null);
    try {
      const r = await api.validate(config);
      setValidateMsg({ ok: r.valid, text: r.valid ? "valid" : r.error ?? "invalid" });
    } catch (e: any) {
      setValidateMsg({ ok: false, text: String(e.message ?? e) });
    }
  }

  async function apply() {
    setApplyMsg(null);
    try {
      await api.apply(config);
      setApplyMsg({ ok: true, text: "applied to control topic" });
    } catch (e: any) {
      setApplyMsg({ ok: false, text: String(e.message ?? e) });
    }
  }

  const ruleSetNames = Object.keys(config.ruleSets);
  const paramsFor = (type: string) => ruleTypes.find((t) => t.type === type)?.params ?? [];

  return (
    <>
      <div className="section">
        <h2>Policy</h2>
        <div className="row">
          <div className="field">
            <label>quiet (seconds) <Help text={HELP.quietMillis} /></label>
            <input
              type="number"
              value={config.policy.quietMillis / 1000}
              onChange={(e) => update((c) => (c.policy.quietMillis = Math.round(+e.target.value * 1000)))}
            />
          </div>
          <div className="field">
            <label>maxWait (seconds) <Help text={HELP.maxWaitMillis} /></label>
            <input
              type="number"
              value={config.policy.maxWaitMillis / 1000}
              onChange={(e) => update((c) => (c.policy.maxWaitMillis = Math.round(+e.target.value * 1000)))}
            />
          </div>
          <div className="field">
            <label>cohortMode <Help text={HELP.cohortMode} /></label>
            <select
              value={config.policy.cohortMode}
              onChange={(e) => update((c) => (c.policy.cohortMode = e.target.value as CohortMode))}
            >
              <option>ENTRY_TOPICS</option>
              <option>ASSUME_ALL</option>
            </select>
          </div>
          <div className="field">
            <label>strictIntermediates <Help text={HELP.strictIntermediates} /></label>
            <select
              value={String(config.policy.strictIntermediates)}
              onChange={(e) => update((c) => (c.policy.strictIntermediates = e.target.value === "true"))}
            >
              <option value="false">false</option>
              <option value="true">true</option>
            </select>
          </div>
        </div>
      </div>

      <div className="section">
        <h2>Topics</h2>
        <p className="hint">
          Editing an existing topic (role, paths, rule set) applies live on Apply. Adding or removing a topic
          changes which Kafka topics the job consumes — that set is fixed when the job starts, so it needs a
          redeploy. When main and load use different physical topic names, the mapping lives in the deployment
          config (per-cluster topicPrefix / topicOverrides), not here — these are the shared logical names.
        </p>
        <table>
          <thead>
            <tr>
              <th>name <Help text={HELP.topicName} /></th>
              <th>role <Help text={HELP.role} /></th>
              <th>format <Help text={HELP.format} /></th>
              <th>guidPath <Help text={HELP.guidPath} /></th>
              <th>sequencePath <Help text={HELP.sequencePath} /></th>
              <th>ruleSet <Help text={HELP.ruleSet} /></th>
              <th />
            </tr>
          </thead>
          <tbody>
            {config.topics.map((t, i) => (
              <tr key={i}>
                <td>
                  <input className="mono" value={t.name} onChange={(e) => update((c) => (c.topics[i].name = e.target.value))} />
                </td>
                <td>
                  <select value={t.role} onChange={(e) => update((c) => (c.topics[i].role = e.target.value as Role))}>
                    <option>ENTRY</option>
                    <option>OUTPUT</option>
                    <option>BOTH</option>
                  </select>
                </td>
                <td>
                  <input value={t.format} onChange={(e) => update((c) => (c.topics[i].format = e.target.value))} />
                </td>
                <td>
                  <input className="mono" value={t.guidPath} onChange={(e) => update((c) => (c.topics[i].guidPath = e.target.value))} />
                </td>
                <td>
                  <input
                    className="mono"
                    value={seqToString(t.sequencePath)}
                    onChange={(e) => update((c) => (c.topics[i].sequencePath = stringToSeq(e.target.value)))}
                  />
                </td>
                <td>
                  <select
                    value={t.ruleSet ?? ""}
                    onChange={(e) => update((c) => (c.topics[i].ruleSet = e.target.value || undefined))}
                  >
                    <option value="">(default)</option>
                    {ruleSetNames.map((n) => (
                      <option key={n}>{n}</option>
                    ))}
                  </select>
                </td>
                <td>
                  <button className="ghost tiny" onClick={() => update((c) => c.topics.splice(i, 1))}>
                    ✕
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="row" style={{ marginTop: 8 }}>
          <button
            className="ghost"
            onClick={() => update((c) => c.topics.push({ name: "", role: "OUTPUT", format: "json", guidPath: "guid" }))}
          >
            + topic
          </button>
        </div>
      </div>

      <div className="section">
        <h2>Rule sets</h2>
        {ruleSetNames.map((name) => {
          const rs = config.ruleSets[name];
          return (
            <div key={name} style={{ marginBottom: 16 }}>
              <div className="row">
                <strong>{name}</strong>
                <label className="muted">
                  <input
                    type="checkbox"
                    style={{ width: "auto" }}
                    checked={rs.nullsEqualAbsent !== false}
                    onChange={(e) => update((c) => (c.ruleSets[name].nullsEqualAbsent = e.target.checked))}
                  />{" "}
                  nullsEqualAbsent <Help text={HELP.nullsEqualAbsent} />
                </label>
                <label className="muted">
                  <input
                    type="checkbox"
                    style={{ width: "auto" }}
                    checked={!!rs.emptyEqualsAbsent}
                    onChange={(e) => update((c) => (c.ruleSets[name].emptyEqualsAbsent = e.target.checked))}
                  />{" "}
                  emptyEqualsAbsent <Help text={HELP.emptyEqualsAbsent} />
                </label>
                {name !== "default" && (
                  <button className="ghost tiny" onClick={() => update((c) => delete c.ruleSets[name])}>
                    remove set
                  </button>
                )}
              </div>
              <table>
                <thead>
                  <tr>
                    <th style={{ width: 150 }}>type <Help text={HELP.ruleType} /></th>
                    <th>path <Help text={HELP.rulePath} /></th>
                    <th>params <Help text={HELP.ruleParams} /></th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {rs.rules.map((rule, ri) => (
                    <tr key={ri}>
                      <td>
                        <select
                          value={rule.type}
                          onChange={(e) => update((c) => (c.ruleSets[name].rules[ri] = { type: e.target.value, path: rule.path }))}
                        >
                          {ruleTypes.map((rt) => (
                            <option key={rt.type}>{rt.type}</option>
                          ))}
                        </select>
                      </td>
                      <td>
                        <input
                          className="mono"
                          value={rule.path}
                          onChange={(e) => update((c) => (c.ruleSets[name].rules[ri].path = e.target.value))}
                        />
                      </td>
                      <td>
                        <div className="row">
                          {paramsFor(rule.type).map((p) => (
                            <input
                              key={p.name}
                              placeholder={p.name}
                              style={{ width: 130 }}
                              value={String(rule[p.name] ?? "")}
                              onChange={(e) =>
                                update((c) => ((c.ruleSets[name].rules[ri] as any)[p.name] = coerce(p.kind, e.target.value)))
                              }
                            />
                          ))}
                        </div>
                      </td>
                      <td>
                        <button className="ghost tiny" onClick={() => update((c) => c.ruleSets[name].rules.splice(ri, 1))}>
                          ✕
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <button
                className="ghost tiny"
                style={{ marginTop: 6 }}
                onClick={() => update((c) => c.ruleSets[name].rules.push({ type: "ignore", path: "" }))}
              >
                + rule
              </button>
            </div>
          );
        })}
        <button
          className="ghost"
          onClick={() => {
            const n = prompt("rule set name");
            if (n) update((c) => (c.ruleSets[n] = { rules: [] }));
          }}
        >
          + rule set
        </button>
      </div>

      <RawJson config={config} onChange={onChange} />

      <div className="section">
        <div className="row end">
          {validateMsg && <span className={`badge ${validateMsg.ok ? "OK" : "ERROR"}`}>{validateMsg.text}</span>}
          {applyMsg && <span className={`badge ${applyMsg.ok ? "OK" : "ERROR"}`}>{applyMsg.text}</span>}
          <button className="ghost" onClick={validate}>
            Validate
          </button>
          <button className="action" onClick={apply}>
            Apply to control topic
          </button>
        </div>
      </div>
    </>
  );
}

function RawJson({ config, onChange }: { config: CompareConfig; onChange: (c: CompareConfig) => void }) {
  const [text, setText] = useState(() => JSON.stringify(config, null, 2));
  const [error, setError] = useState("");

  useEffect(() => {
    setText(JSON.stringify(config, null, 2));
  }, [config]);

  return (
    <div className="section">
      <h2>Raw JSON</h2>
      <textarea
        rows={14}
        value={text}
        onChange={(e) => setText(e.target.value)}
        onBlur={() => {
          try {
            onChange(JSON.parse(text));
            setError("");
          } catch (err: any) {
            setError(String(err.message ?? err));
          }
        }}
      />
      {error && <div style={{ color: "var(--error)", marginTop: 6 }}>{error}</div>}
    </div>
  );
}

function seqToString(s?: string | string[]) {
  return Array.isArray(s) ? s.join(",") : s ?? "";
}

function stringToSeq(v: string): string | string[] | undefined {
  const parts = v.split(",").map((s) => s.trim()).filter(Boolean);
  if (parts.length === 0) return undefined;
  return parts.length === 1 ? parts[0] : parts;
}

function coerce(kind: string, v: string): unknown {
  return kind === "number" && v !== "" ? Number(v) : v;
}
