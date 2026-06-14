import { useEffect, useState } from "react";
import {
  ActionIcon,
  Anchor,
  AppShell,
  Badge,
  Box,
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

type Tab = "dashboard" | "editor";

export function App() {
  const [tab, setTab] = useState<Tab>("dashboard");
  const [config, setConfig] = useState<CompareConfig | null>(null);
  const [ruleTypes, setRuleTypes] = useState<RuleType[]>([]);
  const [source, setSource] = useState("");
  const [flinkUrl, setFlinkUrl] = useState<string | null>(null);
  const [online, setOnline] = useState<boolean | null>(null);
  const { colorScheme, toggleColorScheme } = useMantineColorScheme();

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
    setTab("editor");
  }

  return (
    <AppShell header={{ height: 56 }} navbar={{ width: 200, breakpoint: "xs" }} padding="md">
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
        <NavLink label="Dashboard" active={tab === "dashboard"} onClick={() => setTab("dashboard")} />
        <NavLink label="Config" active={tab === "editor"} onClick={() => setTab("editor")} />
      </AppShell.Navbar>

      <AppShell.Main>
        {config && tab === "editor" && <Editor config={config} ruleTypes={ruleTypes} onChange={setConfig} />}
        {tab === "dashboard" && <Dashboard onIgnorePath={addIgnoreRule} />}
      </AppShell.Main>
    </AppShell>
  );
}
