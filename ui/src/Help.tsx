import { Tooltip } from "@mantine/core";

export function Help({ text }: { text: string }) {
  return (
    <Tooltip
      label={text}
      multiline
      w={300}
      withArrow
      events={{ hover: true, focus: true, touch: true }}
    >
      <span className="help-dot" tabIndex={0} role="note" aria-label={text}>
        ?
      </span>
    </Tooltip>
  );
}

export const HELP = {
  quietMillis:
    "Judge a document this many seconds after its last message in either environment. Keep it above your main→load mirror lag, or healthy documents briefly look MISSING_IN_LOAD. (Stored as quietMillis in the config.)",
  maxWaitMillis:
    "Upper bound: judge no later than this many seconds after the document's first message, even if updates keep arriving. (Stored as maxWaitMillis in the config.)",
  cohortMode:
    "ENTRY_TOPICS: only judge GUIDs seen entering both pipelines (main-only → NOT_MIRRORED, load-only → TEST_TRAFFIC). ASSUME_ALL: judge every GUID — use when you don't tap entry topics.",
  strictIntermediates:
    "When the final states match but the document passed through different intermediate states (EQUAL_DIVERGED), report it as WARN instead of INFO.",
  topicName:
    "Logical topic name, matched across environments. Physical names per environment (prefixes/overrides) are set in the deployment config.",
  role:
    "ENTRY: only marks which GUIDs entered the pipeline (content not compared). OUTPUT: the content is compared. BOTH: a mirrored input — anchors the cohort AND is compared, so the mirror itself is verified.",
  format: "Decoder for the payload bytes: json or xml (more formats can be plugged in).",
  guidPath:
    "Path to the document id used to match messages across environments. Syntax: dotted fields (doc.guid), array index (items[0]), XML attribute (export.@guid). Must resolve to a single string or number.",
  sequencePath:
    "Optional. Field(s) identifying each update of a document — one path, or comma-separated for a composite key. When set, every message is paired by sequence and compared, and the final state is the highest sequence. Blank = dedupe identical messages by content.",
  ruleSet:
    "Which named rule set applies to this topic. Blank uses 'default'. A named set replaces default entirely — rules are not merged.",
  nullsEqualAbsent: "Treat a field explicitly set to null as equal to the field being absent.",
  emptyEqualsAbsent: "Treat an empty object {} or empty array [] as equal to the field being absent.",
  ruleType:
    "ignore: drop the field before comparing. mask: replace its value with *** (presence still compared). unordered: sort the array before comparing. cast: convert a string field to a number or boolean (so XML fields can use numberTolerance). numberTolerance: allow a small numeric difference. timeTolerance: allow a small time difference.",
  rulePath:
    "Path pattern the rule matches. Supports dotted fields (a.b), array index (items[2]) or any index (items[]), * for any one field, ** for any depth (e.g. **.traceId), and \\. for a literal dot.",
  ruleParams:
    "Parameters for the selected rule type — e.g. epsilon for numberTolerance; toleranceMillis and epochUnit for timeTolerance.",
  paritySummary:
    "Counts of recent verdicts by status — a live sample of the latest ones, not a full total. EQUAL = match; DIFF = mismatch; EQUAL_DIVERGED = same final state via a different path; MISSING_IN_LOAD / EXTRA_IN_LOAD / NOT_MIRRORED / TEST_TRAFFIC / UNANCHORED describe coverage. For authoritative totals at scale, use the rollups below.",
  rollups:
    "A rollup groups every document that differs the same way: one row per (topic, status, diff signature) within a time window, with how many documents matched and a few sample GUIDs. This is the scalable view — thousands of identical diffs collapse to one row. The 'ignore' button turns a noise signature into an ignore rule.",
  verdicts:
    "The most recent individual verdicts (one per document × topic). This is a live tail of the latest results, not the full history — for volume, read the rollups above and open a sample GUID for detail.",
  deadLetters:
    "Messages that could not become observations — unparseable payload, missing GUID, or a missing declared sequence. A steady stream here is itself a finding (e.g. the new version changed a payload so it no longer parses).",
} as const;

export const RULE_DOC: Record<string, string> = {
  ignore:
    "Removes the matching field (and its whole subtree) before anything else — it can never cause a diff and isn't even hashed. Use for noise: trace ids, timestamps, processing metadata. No parameters.",
  mask:
    "Replaces the matching value with *** on both sides before comparing, so the value is never compared or shown in a verdict — only its presence/absence is. Use for PII you must not surface. No parameters.",
  unordered:
    "Sorts the matching array before comparing, so element order can't cause diffs. Use when a list's order isn't meaningful. Sorts after child rules run, so ignored sub-fields don't affect the order. No parameters.",
  cast:
    "Converts the matching string value to a real number or boolean before hashing and comparing. Mainly for XML, where every leaf decodes as a string — cast a numeric field so numberTolerance can apply, or so \"100\" and \"100.0\" compare equal. Values that don't parse are left unchanged. Parameter: to (NUMBER or BOOLEAN).",
  numberTolerance:
    "Treats two numbers at the matching path as equal when they differ by at most epsilon — absorbs floating-point drift and rounding. Applies only to numeric values. Parameter: epsilon.",
  timeTolerance:
    "Treats two timestamps at the matching path as equal when they differ by at most toleranceMillis. Accepts ISO-8601 strings or epoch numbers, so it absorbs clock skew between environments. Parameters: toleranceMillis, epochUnit.",
};

export const PARAM_DOC: Record<string, string> = {
  to: "Target type to convert the string into: NUMBER or BOOLEAN. Values that don't parse as that type are left as-is.",
  epsilon: "Maximum absolute numeric difference still treated as equal — e.g. 0.001 ignores sub-thousandth drift.",
  toleranceMillis:
    "Maximum time difference still treated as equal, in milliseconds (e.g. 5000 = 5 seconds). The field may be an ISO-8601 string or an epoch number.",
  epochUnit:
    "Only used when the timestamp is a number — is it epoch MILLIS or SECONDS? Ignored for ISO-8601 string values. Defaults to MILLIS.",
};
