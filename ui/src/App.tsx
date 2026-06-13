import { useEffect, useState } from "react";
import { api } from "./api";
import { CompareConfig, RuleType, defaultConfig } from "./types";
import { Editor } from "./Editor";
import { Dashboard } from "./Dashboard";

type Tab = "dashboard" | "editor";

export function App() {
  const [tab, setTab] = useState<Tab>("dashboard");
  const [config, setConfig] = useState<CompareConfig | null>(null);
  const [ruleTypes, setRuleTypes] = useState<RuleType[]>([]);
  const [source, setSource] = useState("");
  const [flinkUrl, setFlinkUrl] = useState<string | null>(null);
  const [loadError, setLoadError] = useState("");

  useEffect(() => {
    api.ruleTypes().then(setRuleTypes).catch(() => {});
    api
      .activeConfig()
      .then((a) => {
        setConfig(a.active ?? defaultConfig());
        setSource(a.source);
        setFlinkUrl(a.flinkUiUrl);
      })
      .catch((e) => {
        setConfig(defaultConfig());
        setLoadError(String(e.message ?? e));
      });
  }, []);

  function addIgnoreRule(path: string, ruleSet = "default") {
    setConfig((prev) => {
      const next = prev ? structuredClone(prev) : defaultConfig();
      if (!next.ruleSets[ruleSet]) next.ruleSets[ruleSet] = { rules: [] };
      const exists = next.ruleSets[ruleSet].rules.some((r) => r.type === "ignore" && r.path === path);
      if (!exists) next.ruleSets[ruleSet].rules.push({ type: "ignore", path });
      return next;
    });
    setTab("editor");
  }

  return (
    <>
      <div className="topbar">
        <h1>hermetrics</h1>
        <button className={`tab ${tab === "dashboard" ? "active" : ""}`} onClick={() => setTab("dashboard")}>
          Dashboard
        </button>
        <button className={`tab ${tab === "editor" ? "active" : ""}`} onClick={() => setTab("editor")}>
          Config
        </button>
        <div className="spacer" />
        {flinkUrl && (
          <a href={flinkUrl} target="_blank" rel="noreferrer">
            Flink UI ↗
          </a>
        )}
        <span className="muted">config source: {source || "?"}</span>
      </div>
      <div className="content">
        {loadError && <div className="banner err">API unreachable: {loadError}</div>}
        {config && tab === "editor" && <Editor config={config} ruleTypes={ruleTypes} onChange={setConfig} />}
        {tab === "dashboard" && <Dashboard onIgnorePath={addIgnoreRule} />}
      </div>
    </>
  );
}
