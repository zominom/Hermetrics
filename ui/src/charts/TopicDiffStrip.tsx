import { Card, Center, Group, RingProgress, SimpleGrid, Text } from "@mantine/core";
import { Finding } from "../types";
import { topicDiffRates } from "../metrics";

export function TopicDiffStrip({ rollups }: { rollups: Finding[] }) {
  const rates = topicDiffRates(rollups);
  if (rates.length === 0) {
    return (
      <Center h={240}>
        <Text c="dimmed" size="sm">
          no per-topic data yet
        </Text>
      </Center>
    );
  }
  return (
    <SimpleGrid cols={{ base: 2, sm: 3 }} spacing="xs">
      {rates.map((t) => (
        <Card key={t.topic} withBorder padding="xs" radius="md">
          <Group justify="space-between" wrap="nowrap" gap="xs">
            <div style={{ minWidth: 0 }}>
              <Text size="sm" fw={600} truncate title={t.topic}>
                {t.topic}
              </Text>
              <Text size="xs" c="dimmed">
                {t.diff} / {t.total} diff
              </Text>
            </div>
            <RingProgress
              size={52}
              thickness={6}
              roundCaps
              sections={[{ value: t.diffPct, color: ringColor(t.diffPct) }]}
              label={
                <Text size="xs" ta="center" fw={700}>
                  {t.diffPct}%
                </Text>
              }
            />
          </Group>
        </Card>
      ))}
    </SimpleGrid>
  );
}

function ringColor(pct: number): string {
  if (pct >= 25) return "red";
  if (pct >= 5) return "yellow";
  return "green";
}
