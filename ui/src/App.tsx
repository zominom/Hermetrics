import { useEffect, useMemo, useState } from "react";
import {
  ActionIcon,
  Anchor,
  AppShell,
  Badge,
  Box,
  Container,
  Group,
  NavLink,
  Text,
  Tooltip,
  useMantineColorScheme,
} from "@mantine/core";
import { api } from "./api";
import { CompareConfig, RuleType, defaultConfig } from "./types";
import { Editor } from "./Editor";
import { Dashboard } from "./Dashboard";
import { TopicPage } from "./TopicPage";
import { useFindings } from "./useFindings";

type View = { kind: "overview" } | { kind: "topic"; name: string } | { kind: "config" };

export function App() {
  const [view, setView] = useState<View>({ kind: "overview" });
  const [config, setConfig] = useState<CompareConfig | null>(null);
  const [ruleTypes, setRuleTypes] = useState<RuleType[]>([]);
  const [source, setSource] = useState("");
  const [flinkUrl, setFlinkUrl] = useState<string | null>(null);
  const [online, setOnline] = useState<boolean | null>(null);
  const { colorScheme, toggleColorScheme } = useMantineColorScheme();
  const findings = useFindings(view.kind !== "config");

  useEffect(() => {
    api.ruleTypes().then(setRuleTypes).catch(() => {});
    api
      .activeConfig()
      .then((a) => {
        setConfig(a.active ?? defaultConfig());
        setSource(a.source);
        setFlinkUrl(a.flinkUiUrl);
        setOnline(true);
      })
      .catch(() => {
        setConfig(defaultConfig());
        setOnline(false);
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
    setView({ kind: "config" });
  }

  const topicNames = useMemo(() => {
    const fromConfig = (config?.topics ?? []).map((t) => t.name).filter(Boolean);
    if (fromConfig.length) return [...new Set(fromConfig)].sort();
    const seen = new Set<string>();
    for (const r of findings.rollups) if (r.topic && r.topic !== "(cohort)") seen.add(String(r.topic));
    for (const v of findings.verdicts) if (v.topic && v.topic !== "(cohort)") seen.add(String(v.topic));
    return [...seen].sort();
  }, [config, findings.rollups, findings.verdicts]);

  return (
    <AppShell header={{ height: 56 }} navbar={{ width: 220, breakpoint: "xs" }} padding="md">
      <AppShell.Header>
        <Group h="100%" px="md" gap="sm">
          <Text fw={800} size="lg" style={{ letterSpacing: 1.5 }}>
            hermetrics
          </Text>
          <Badge variant="dot" color={online === false ? "red" : "green"} size="sm">
            {online === false ? "API offline" : "API online"}
          </Badge>
          <Box style={{ flex: 1 }} />
          <Text size="xs" c="dimmed">
            config: {source || "?"}
          </Text>
          {flinkUrl && (
            <Anchor href={flinkUrl} target="_blank" size="sm">
              Flink UI ↗
            </Anchor>
          )}
          <Tooltip label="Toggle light / dark">
            <ActionIcon variant="default" onClick={toggleColorScheme} aria-label="toggle color scheme">
              {colorScheme === "dark" ? "☀" : "☾"}
            </ActionIcon>
          </Tooltip>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="sm">
        <NavLink label="Overview" active={view.kind === "overview"} onClick={() => setView({ kind: "overview" })} />
        <NavLink label="Topics" defaultOpened childrenOffset={20}>
          {topicNames.length === 0 && (
            <Text size="xs" c="dimmed" px="sm" py={4}>
              no topics
            </Text>
          )}
          {topicNames.map((t) => (
            <NavLink
              key={t}
              label={t}
              active={view.kind === "topic" && view.name === t}
              onClick={() => setView({ kind: "topic", name: t })}
            />
          ))}
        </NavLink>
        <NavLink label="Config" active={view.kind === "config"} onClick={() => setView({ kind: "config" })} />
      </AppShell.Navbar>

      <AppShell.Main>
        <Container size="xl" px={0}>
          {view.kind === "overview" && <Dashboard findings={findings} onIgnorePath={addIgnoreRule} />}
          {view.kind === "topic" && <TopicPage topic={view.name} findings={findings} onIgnorePath={addIgnoreRule} />}
          {view.kind === "config" && config && <Editor config={config} ruleTypes={ruleTypes} onChange={setConfig} />}
        </Container>
      </AppShell.Main>
    </AppShell>
  );
}
