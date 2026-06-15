import { Alert, Badge, Button, Group, Paper, Stack, Switch, Text, Title } from "@mantine/core";
import { Findings } from "./useFindings";
import { Finding } from "./types";
import { statusColor } from "./theme";
import { DiffVolumeChart } from "./charts/DiffVolumeChart";
import { DeadLettersTable, RollupsTable, VerdictsTable } from "./tables";

export function TopicPage({
  topic,
  findings,
  onIgnorePath,
}: {
  topic: string;
  findings: Findings;
  onIgnorePath: (path: string) => void;
}) {
  const { rollups, verdicts, deadLetters, error, auto, setAuto, refresh } = findings;
  const topicRollups = rollups.filter((r) => r.topic === topic);
  const topicVerdicts = verdicts.filter((v) => v.topic === topic);
  const topicDeadLetters = deadLetters.filter((d) => d.topic === topic);
  const counts = statusCounts(topicRollups);
  const total = Object.values(counts).reduce((a, b) => a + b, 0);

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
            Topic: <Text span ff="monospace" inherit>{topic}</Text>
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
            {total} recent
          </Badge>
          {Object.entries(counts)
            .sort()
            .map(([k, n]) => (
              <Badge key={k} size="lg" variant="light" color={statusColor(k)}>
                {k}: {n}
              </Badge>
            ))}
          {total === 0 && (
            <Text c="dimmed" size="sm">
              no recent findings for this topic
            </Text>
          )}
        </Group>
      </Paper>

      <Paper withBorder p="md" radius="md">
        <Title order={5} mb="xs">
          Diff volume over time
        </Title>
        <DiffVolumeChart rollups={topicRollups} />
      </Paper>

      <Paper withBorder p="md" radius="md">
        <Title order={5} mb="sm">
          Diff signatures
        </Title>
        <RollupsTable rollups={topicRollups} onIgnorePath={onIgnorePath} />
      </Paper>

      <Paper withBorder p="md" radius="md">
        <Title order={5} mb="sm">
          Recent verdicts
        </Title>
        <VerdictsTable verdicts={topicVerdicts} />
      </Paper>

      <Paper withBorder p="md" radius="md">
        <Title order={5} mb="sm">
          Dead letters
        </Title>
        <DeadLettersTable deadLetters={topicDeadLetters} />
      </Paper>
    </Stack>
  );
}

function statusCounts(rollups: Finding[]): Record<string, number> {
  const counts: Record<string, number> = {};
  for (const r of rollups) {
    counts[r.status] = (counts[r.status] ?? 0) + (Number(r.count) || 0);
  }
  return counts;
}
