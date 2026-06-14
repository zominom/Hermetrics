import { useEffect, useState } from "react";
import {
  ActionIcon,
  Button,
  Group,
  NumberInput,
  Paper,
  Select,
  Stack,
  Switch,
  Table,
  Text,
  TextInput,
  Textarea,
  Title,
} from "@mantine/core";
import { api } from "./api";
import { CohortMode, CompareConfig, Role, RuleType } from "./types";
import { Help, HELP, RULE_DOC, PARAM_DOC } from "./Help";

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
  const ruleTypeNames = ruleTypes.map((t) => t.type);
  const paramsFor = (type: string) => ruleTypes.find((t) => t.type === type)?.params ?? [];

  return (
    <Stack gap="md">
      <Paper withBorder p="md" radius="md">
        <Title order={5} mb="sm">
          Policy
        </Title>
        <Group align="flex-end" gap="md">
          <NumberInput
            label={<>quiet (seconds) <Help text={HELP.quietMillis} /></>}
            w={150}
            value={config.policy.quietMillis / 1000}
            onChange={(v) => update((c) => (c.policy.quietMillis = Math.round(Number(v) * 1000)))}
          />
          <NumberInput
            label={<>maxWait (seconds) <Help text={HELP.maxWaitMillis} /></>}
            w={150}
            value={config.policy.maxWaitMillis / 1000}
            onChange={(v) => update((c) => (c.policy.maxWaitMillis = Math.round(Number(v) * 1000)))}
          />
          <Select
            label={<>cohortMode <Help text={HELP.cohortMode} /></>}
            w={180}
            data={["ENTRY_TOPICS", "ASSUME_ALL"]}
            value={config.policy.cohortMode}
            onChange={(v) => update((c) => (c.policy.cohortMode = (v as CohortMode) ?? "ENTRY_TOPICS"))}
          />
          <Switch
            label={<>strictIntermediates <Help text={HELP.strictIntermediates} /></>}
            checked={config.policy.strictIntermediates}
            onChange={(e) => update((c) => (c.policy.strictIntermediates = e.currentTarget.checked))}
          />
        </Group>
      </Paper>

      <Paper withBorder p="md" radius="md">
        <Title order={5} mb="xs">
          Topics
        </Title>
        <Text c="dimmed" size="xs" mb="sm" maw={900}>
          Editing an existing topic (role, paths, rule set) applies live on Apply. Adding or removing a topic changes which
          Kafka topics the job consumes — that set is fixed when the job starts, so it needs a redeploy. When main and load
          use different physical topic names, the mapping lives in the deployment config (per-cluster topicPrefix /
          topicOverrides), not here — these are the shared logical names.
        </Text>
        <Table verticalSpacing="xs">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>name <Help text={HELP.topicName} /></Table.Th>
              <Table.Th>role <Help text={HELP.role} /></Table.Th>
              <Table.Th>format <Help text={HELP.format} /></Table.Th>
              <Table.Th>guidPath <Help text={HELP.guidPath} /></Table.Th>
              <Table.Th>sequencePath <Help text={HELP.sequencePath} /></Table.Th>
              <Table.Th>ruleSet <Help text={HELP.ruleSet} /></Table.Th>
              <Table.Th />
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {config.topics.map((t, i) => (
              <Table.Tr key={i}>
                <Table.Td>
                  <TextInput value={t.name} onChange={(e) => update((c) => (c.topics[i].name = e.currentTarget.value))} />
                </Table.Td>
                <Table.Td>
                  <Select
                    w={110}
                    data={["ENTRY", "OUTPUT", "BOTH"]}
                    value={t.role}
                    onChange={(v) => update((c) => (c.topics[i].role = (v as Role) ?? "OUTPUT"))}
                  />
                </Table.Td>
                <Table.Td>
                  <Select
                    w={90}
                    data={["json", "xml"]}
                    value={t.format}
                    onChange={(v) => update((c) => (c.topics[i].format = v ?? "json"))}
                  />
                </Table.Td>
                <Table.Td>
                  <TextInput value={t.guidPath} onChange={(e) => update((c) => (c.topics[i].guidPath = e.currentTarget.value))} />
                </Table.Td>
                <Table.Td>
                  <TextInput
                    value={seqToString(t.sequencePath)}
                    onChange={(e) => update((c) => (c.topics[i].sequencePath = stringToSeq(e.currentTarget.value)))}
                  />
                </Table.Td>
                <Table.Td>
                  <Select
                    w={130}
                    data={[{ value: "", label: "(default)" }, ...ruleSetNames.map((n) => ({ value: n, label: n }))]}
                    value={t.ruleSet ?? ""}
                    onChange={(v) => update((c) => (c.topics[i].ruleSet = v || undefined))}
                  />
                </Table.Td>
                <Table.Td>
                  <ActionIcon variant="subtle" color="red" onClick={() => update((c) => c.topics.splice(i, 1))}>
                    ✕
                  </ActionIcon>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
        <Button
          variant="default"
          size="xs"
          mt="sm"
          onClick={() => update((c) => c.topics.push({ name: "", role: "OUTPUT", format: "json", guidPath: "guid" }))}
        >
          + topic
        </Button>
      </Paper>

      <Paper withBorder p="md" radius="md">
        <Title order={5} mb="sm">
          Rule sets
        </Title>
        <Stack gap="lg">
          {ruleSetNames.map((name) => {
            const rs = config.ruleSets[name];
            return (
              <div key={name}>
                <Group mb="xs">
                  <Text fw={700}>{name}</Text>
                  <Switch
                    size="xs"
                    label={<>nullsEqualAbsent <Help text={HELP.nullsEqualAbsent} /></>}
                    checked={rs.nullsEqualAbsent !== false}
                    onChange={(e) => update((c) => (c.ruleSets[name].nullsEqualAbsent = e.currentTarget.checked))}
                  />
                  <Switch
                    size="xs"
                    label={<>emptyEqualsAbsent <Help text={HELP.emptyEqualsAbsent} /></>}
                    checked={!!rs.emptyEqualsAbsent}
                    onChange={(e) => update((c) => (c.ruleSets[name].emptyEqualsAbsent = e.currentTarget.checked))}
                  />
                  {name !== "default" && (
                    <Button variant="subtle" color="red" size="compact-xs" onClick={() => update((c) => delete c.ruleSets[name])}>
                      remove set
                    </Button>
                  )}
                </Group>
                <Table verticalSpacing="xs">
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th w={170}>type <Help text={HELP.ruleType} /></Table.Th>
                      <Table.Th>path <Help text={HELP.rulePath} /></Table.Th>
                      <Table.Th>params <Help text={HELP.ruleParams} /></Table.Th>
                      <Table.Th />
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {rs.rules.map((rule, ri) => (
                      <Table.Tr key={ri}>
                        <Table.Td>
                          <Group gap={4} wrap="nowrap">
                            <Select
                              w={120}
                              data={ruleTypeNames}
                              value={rule.type}
                              onChange={(v) => update((c) => (c.ruleSets[name].rules[ri] = { type: v ?? "ignore", path: rule.path }))}
                            />
                            <Help text={RULE_DOC[rule.type] ?? "Custom rule type registered in RuleTypeRegistry."} />
                          </Group>
                        </Table.Td>
                        <Table.Td>
                          <TextInput
                            ff="monospace"
                            value={rule.path}
                            onChange={(e) => update((c) => (c.ruleSets[name].rules[ri].path = e.currentTarget.value))}
                          />
                        </Table.Td>
                        <Table.Td>
                          <Group gap="xs">
                            {paramsFor(rule.type).map((p) => (
                              <Group key={p.name} gap={4} wrap="nowrap">
                                {renderParam(p, rule[p.name], (val) =>
                                  update((c) => ((c.ruleSets[name].rules[ri] as any)[p.name] = val)),
                                )}
                                {PARAM_DOC[p.name] && <Help text={PARAM_DOC[p.name]} />}
                              </Group>
                            ))}
                            {paramsFor(rule.type).length === 0 && (
                              <Text c="dimmed" size="xs">
                                no parameters
                              </Text>
                            )}
                          </Group>
                        </Table.Td>
                        <Table.Td>
                          <ActionIcon variant="subtle" color="red" onClick={() => update((c) => c.ruleSets[name].rules.splice(ri, 1))}>
                            ✕
                          </ActionIcon>
                        </Table.Td>
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
                <Button
                  variant="default"
                  size="compact-xs"
                  mt={6}
                  onClick={() => update((c) => c.ruleSets[name].rules.push({ type: "ignore", path: "" }))}
                >
                  + rule
                </Button>
              </div>
            );
          })}
        </Stack>
        <Button
          variant="default"
          size="xs"
          mt="md"
          onClick={() => {
            const n = prompt("rule set name");
            if (n) update((c) => (c.ruleSets[n] = { rules: [] }));
          }}
        >
          + rule set
        </Button>
      </Paper>

      <RawJson config={config} onChange={onChange} />

      <Paper withBorder p="md" radius="md">
        <Group justify="flex-end">
          {validateMsg && (
            <Text c={validateMsg.ok ? "green" : "red"} size="sm">
              {validateMsg.text}
            </Text>
          )}
          {applyMsg && (
            <Text c={applyMsg.ok ? "green" : "red"} size="sm">
              {applyMsg.text}
            </Text>
          )}
          <Button variant="default" onClick={validate}>
            Validate
          </Button>
          <Button onClick={apply}>Apply to control topic</Button>
        </Group>
      </Paper>
    </Stack>
  );
}

function renderParam(
  p: { name: string; kind: string },
  value: unknown,
  set: (val: unknown) => void,
) {
  if (p.kind === "number") {
    return (
      <NumberInput w={140} placeholder={p.name} value={value === undefined || value === null ? "" : (value as number)} onChange={(v) => set(v === "" ? "" : Number(v))} />
    );
  }
  if (p.kind.startsWith("enum:")) {
    return (
      <Select w={140} placeholder={p.name} data={p.kind.slice(5).split(",")} value={(value as string) ?? null} onChange={(v) => set(v ?? "")} />
    );
  }
  return <TextInput w={140} placeholder={p.name} value={String(value ?? "")} onChange={(e) => set(e.currentTarget.value)} />;
}

function RawJson({ config, onChange }: { config: CompareConfig; onChange: (c: CompareConfig) => void }) {
  const [text, setText] = useState(() => JSON.stringify(config, null, 2));
  const [error, setError] = useState("");

  useEffect(() => {
    setText(JSON.stringify(config, null, 2));
  }, [config]);

  return (
    <Paper withBorder p="md" radius="md">
      <Title order={5} mb="xs">
        Raw JSON
      </Title>
      <Textarea
        autosize
        minRows={10}
        maxRows={24}
        styles={{ input: { fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace", fontSize: 12 } }}
        value={text}
        onChange={(e) => setText(e.currentTarget.value)}
        onBlur={() => {
          try {
            onChange(JSON.parse(text));
            setError("");
          } catch (err: any) {
            setError(String(err.message ?? err));
          }
        }}
        error={error || undefined}
      />
    </Paper>
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
