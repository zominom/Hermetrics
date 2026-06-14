import { AreaChart } from "@mantine/charts";
import { Center, Text } from "@mantine/core";
import { Finding } from "../types";
import { diffVolumeSeries } from "../metrics";

export function DiffVolumeChart({ rollups }: { rollups: Finding[] }) {
  const data = diffVolumeSeries(rollups);
  if (data.length === 0) {
    return (
      <Center h={240}>
        <Text c="dimmed" size="sm">
          no rollups yet — diff volume appears once the job emits them
        </Text>
      </Center>
    );
  }
  return (
    <AreaChart
      h={240}
      data={data}
      dataKey="time"
      withLegend
      curveType="monotone"
      withDots={false}
      series={[
        { name: "diff", label: "DIFF", color: "red.6" },
        { name: "other", label: "other", color: "gray.5" },
      ]}
    />
  );
}
