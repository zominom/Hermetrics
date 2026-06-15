import { ActionIcon, Badge, Code, Group, Table, Text } from "@mantine/core";
import { Finding } from "./types";
import { statusColor } from "./theme";

export function RollupsTable({ rollups, onIgnorePath }: { rollups: Finding[]; onIgnorePath: (path: string) => void }) {
  return (
    <Table striped highlightOnHover>
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
  );
}

export function VerdictsTable({ verdicts }: { verdicts: Finding[] }) {
  return (
    <Table striped highlightOnHover>
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
  );
}

export function DeadLettersTable({ deadLetters }: { deadLetters: Finding[] }) {
  return (
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
  );
}

export function pathOf(signatureEntry: string): string {
  const idx = signatureEntry.lastIndexOf(":");
  return idx > 0 ? signatureEntry.slice(0, idx).trim() : signatureEntry.trim();
}
