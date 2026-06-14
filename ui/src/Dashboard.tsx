import { useEffect, useState } from "react";
import {
  ActionIcon,
  Alert,
  Badge,
  Button,
  Code,
  Group,
  Paper,
  SimpleGrid,
  Stack,
  Switch,
  Table,
  Text,
  Title,
} from "@mantine/core";
import { api } from "./api";
import { Finding, Summary } from "./types";
import { Help, HELP } from "./Help";
import { statusColor } from "./theme";
import { DiffVolumeChart } from "./charts/DiffVolumeChart";
import { TopicDiffStrip } from "./charts/TopicDiffStrip";

export function Dashboard({ onIgnorePath }: { onIgnorePath: (path: string) => void }) {
  const [summary, setSummary] = useState<Summary | null>(null);
  const [rollups, setRollups] = useState<Finding[]>([]);
  const [verdicts, setVerdicts] = useState<Finding[]>([]);
  const [deadLetters, setDeadLetters] = useState<Finding[]>([]);
  const [error, setError] = useState("");
  const [auto, setAuto] = useState(true);

  async function refresh() {
    try {
      const [s, r, v, d] = await Promise.all([api.summary(), api.rollups(500), api.verdicts(100), api.deadLetters(50)]);
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
    <Stack gap="md">
      {error && (
        <Alert color="red" title="API error">
          {error}
        </Alert>
      )}

      <Paper withBorder p="md" radius="md">
        <Group justify="space-between" mb="sm">
          <Title order={4}>
            Parity summary <Help text={HELP.paritySummary} />
          </Title>
          <Group gap="sm">
            <Switch label="auto-refresh" checked={auto} onChange={(e) => setAuto(e.currentTarget.checked)} size="sm" />
            <Button variant="default" size="xs" onClick={refresh}>
              Refresh
            </Button>
          </Group>
        </Group>
        <Group gap="sm">
          <Badge size="lg" variant="light" color="gray">
            {summary?.total ?? 0} recent
          </Badge>
          {summary &&
            Object.entries(summary.counts)
              .sort()
              .map(([k, n]) => (
                <Badge key={k} size="lg" variant="light" color={statusColor(k)}>
                  {k}: {n}
                </Badge>
              ))}
        </Group>
      </Paper>

      <SimpleGrid cols={{ base: 1, md: 2 }}>
        <Paper withBorder p="md" radius="md">
          <Title order={5} mb="xs">
            Diff volume over time <Help text={HELP.rollups} />
          </Title>
          <DiffVolumeChart rollups={rollups} />
        </Paper>
        <Paper withBorder p="md" radius="md">
          <Title order={5} mb="xs">
            Diff rate by topic
          </Title>
          <TopicDiffStrip rollups={rollups} />
        </Paper>
      </SimpleGrid>

      <Paper withBorder p="md" radius="md">
        <Title order={5} mb="sm">
          Diff signatures (rollups) <Help text={HELP.rollups} />
        </Title>
        <Table striped highlightOnHover stickyHeader>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>topic</Table.Th>
              <Table.Th>status</Table.Th>
              <Table.Th>signature</Table.Th>
              <Table.Th>count</Table.Th>
              <Table.Th>samples</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {rollups.length === 0 && (
              <Table.Tr>
                <Table.Td colSpan={5}>
                  <Text c="dimmed">no rollups yet</Text>
                </Table.Td>
              </Table.Tr>
            )}
            {rollups.map((r, i) => (
              <Table.Tr key={i}>
                <Table.Td>
                  <Code>{r.topic}</Code>
                </Table.Td>
                <Table.Td>
                  <Badge color={statusColor(r.status)} variant="light">
                    {r.status}
                  </Badge>
                </Table.Td>
                <Table.Td>
                  {(r.signature ?? []).map((s: string, si: number) => (
                    <Group key={si} gap={6} wrap="nowrap">
                      <Code>{s}</Code>
                      {r.status === "DIFF" && (
                        <ActionIcon size="sm" variant="subtle" title="add ignore rule for this path" onClick={() => onIgnorePath(pathOf(s))}>
                          ⊘
                        </ActionIcon>
                      )}
                    </Group>
                  ))}
                </Table.Td>
                <Table.Td>
                  <Text fw={700}>{r.count}</Text>
                </Table.Td>
                <Table.Td>
                  <Group gap={4}>
                    {(r.sampleGuids ?? []).map((g: string) => (
                      <Code key={g}>{g}</Code>
                    ))}
                  </Group>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Paper>

      <Paper withBorder p="md" radius="md">
        <Title order={5} mb="sm">
          Recent verdicts <Help text={HELP.verdicts} />
        </Title>
        <Table striped highlightOnHover stickyHeader>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>guid</Table.Th>
              <Table.Th>topic</Table.Th>
              <Table.Th>status</Table.Th>
              <Table.Th>rev</Table.Th>
              <Table.Th>signature</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {verdicts.length === 0 && (
              <Table.Tr>
                <Table.Td colSpan={5}>
                  <Text c="dimmed">no verdicts yet</Text>
                </Table.Td>
              </Table.Tr>
            )}
            {verdicts.map((v, i) => (
              <Table.Tr key={i}>
                <Table.Td>
                  <Code>{v.guid}</Code>
                </Table.Td>
                <Table.Td>
                  <Code>{v.topic}</Code>
                </Table.Td>
                <Table.Td>
                  <Badge color={statusColor(v.status)} variant="light">
                    {v.status}
                  </Badge>
                </Table.Td>
                <Table.Td>{v.revision}</Table.Td>
                <Table.Td>
                  <Text c="dimmed" ff="monospace" size="xs">
                    {v.signatureId ?? ""}
                  </Text>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Paper>

      <Paper withBorder p="md" radius="md">
        <Title order={5} mb="sm">
          Dead letters <Help text={HELP.deadLetters} />
        </Title>
        <Table striped highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>env</Table.Th>
              <Table.Th>topic</Table.Th>
              <Table.Th>error</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {deadLetters.length === 0 && (
              <Table.Tr>
                <Table.Td colSpan={3}>
                  <Text c="dimmed">none</Text>
                </Table.Td>
              </Table.Tr>
            )}
            {deadLetters.map((d, i) => (
              <Table.Tr key={i}>
                <Table.Td>{d.env}</Table.Td>
                <Table.Td>
                  <Code>{d.topic}</Code>
                </Table.Td>
                <Table.Td>
                  <Code>{d.error}</Code>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Paper>
    </Stack>
  );
}

function pathOf(signatureEntry: string): string {
  const idx = signatureEntry.lastIndexOf(":");
  return idx > 0 ? signatureEntry.slice(0, idx).trim() : signatureEntry.trim();
}
