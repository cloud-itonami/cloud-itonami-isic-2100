# cloud-itonami-isic-2100

Open Business Blueprint for **ISIC 2100**: manufacture of
pharmaceuticals, medicinal chemical and botanical products — the last
scaffold gap flagged by ADR-2607121000 (after 食 C10-12 / 衣 C13-14 /
住 4100 landed in ADR-2607122200's batch and the own-sector 6201/6311
actors shipped).

**Maturity: `:blueprint`** — this repository publishes the business
blueprint only. There is **no actor implementation yet**, and none is
claimed. ISIC division 21 sits in **rollout Wave 3
(production/robotics)** of the reverse-toposort plan (ADR-2607121000):
implementation is gated on the robotics premise (ADR-2607011000).
Publishing the blueprint now is ammunition loading for when that gate
opens (ADR-2607122100 Track A).

## What the implemented actor will be

**PharmaOps-LLM ⊣ Pharma Manufacturing Governor** — the fleet-standard
pattern with the strictest safety posture in the manufacturing wave:
the advisor LLM drafts batch-record assembly, GMP/pharmacopoeia
citation checks (per-jurisdiction spec-basis — never invented),
deviation/CAPA summaries and release checklists; the independent
`:pharma-manufacturing-governor` (a keyword unique fleet-wide) gates
every action. Physical-domain work (dispensing, granulation, filling,
packaging, cold-chain) is executed by robots under
`kotoba-lang/robotics` safety classes; batch release and anything
pharmacovigilance-relevant is always `:safety-critical` with human
sign-off — an LLM never releases a batch.

Operating states: `intake → design → produce → inspect → package → audit`.

## Why open

AGPL-3.0-or-later, forkable by any qualified operator, so regional
manufacturers never surrender batch, deviation and traceability data
to a closed SaaS. Part of the [cloud-itonami](https://itonami.cloud)
open business fleet.
