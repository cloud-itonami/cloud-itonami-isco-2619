# cloud-itonami-isco-2619

Open Occupation Blueprint for **ISCO-08 2619**: Legal Professionals Not Elsewhere Classified.

This repository designs a forkable OSS business for an independent legal support and compliance practice: a document notarization and certification-support robot handles physical stamping and binding under a governor-gated actor, so the practice keeps its own certification records instead of renting a closed compliance SaaS.

**Maturity: `:implemented`.** `src/legalcompliance/` implements the
`LegalComplianceActor` as a `langgraph.graph/state-graph`
(`legalcompliance.actor`) wired to a `Compliance Advisor` (`legalcompliance.advisor`)
and an independent `LegalComplianceGovernor` (`legalcompliance.governor`),
following the itonami actor pattern (ADR-2607011000): `:intake -> :advise
-> :govern -> :decide -+-> :commit (:ok?) +-> :request-approval (:escalate?,
human-in-the-loop interrupt) +-> :hold (:hard?)`. 14 tests / 29 assertions
green (`clojure -M:test`). HARD invariants (always hold, never
overridable): client provenance, no-actuation (`:effect` must be
`:propose`), a registered document basis for any certification
proposal, the proposed certified-copy count not exceeding the
document's registered authorized quantity (issuing more certified
copies than authorized is unregulated duplication, not efficient
service), and verified identity before any certification can be
committed (certifying a document without verified identity is a
notarization fraud risk, not efficient service). Always-escalate ops
(human sign-off regardless of confidence, mapping this repo's Trust
Controls in [`docs/business-model.md`](docs/business-model.md)):
`:approve-certification-issuance` (no notarization or certification
issuance without the governor gate) and `:approve-regulatory-filing`
(submitting a regulatory filing on the client's behalf always requires
human sign-off).

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a document notarization and certification-support robot performs physical stamping, seal application and certified-copy binding under an actor that proposes
actions and an independent **Legal Compliance Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
notarization or certification issuance) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
document submission + identity verification + certification request
        |
        v
Compliance Advisor -> Legal Compliance Governor -> certify/advise, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `2619`). Required capabilities:

- :robotics
- :identity
- :forms
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
