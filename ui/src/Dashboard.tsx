import { Alert, Badge, Button, Group, Paper, SimpleGrid, Stack, Switch, Title } from "@mantine/core";
import { useFindings } from "./useFindings";
import { Help, HELP } from "./Help";
import { statusColor } from "./theme";
import { DiffVolumeChart } from "./charts/DiffVolumeChart";
import { TopicDiffStrip } from "./charts/TopicDiffStrip";
import { DeadLettersTable, RollupsTable, VerdictsTable } from "./tables";

export function Dashboard({ onIgnorePath }: { onIgnorePath: (path: string) => void }) {
  const { summary, rollups, verdicts, deadLetters, error, auto, setAuto, refresh } = useFindings();

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
        <RollupsTable rollups={rollups} onIgnorePath={onIgnorePath} />
      </Paper>

      <Paper withBorder p="md" radius="md">
        <Title order={5} mb="sm">
          Recent verdicts <Help text={HELP.verdicts} />
        </Title>
        <VerdictsTable verdicts={verdicts} />
      </Paper>

      <Paper withBorder p="md" radius="md">
        <Title order={5} mb="sm">
          Dead letters <Help text={HELP.deadLetters} />
        </Title>
        <DeadLettersTable deadLetters={deadLetters} />
      </Paper>
    </Stack>
  );
}
