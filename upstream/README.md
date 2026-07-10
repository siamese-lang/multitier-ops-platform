# Upstream Application

This directory documents upstream OpenKoda source information and verification notes.

Do not place unmodified upstream source code here unless there is a clear reason and the license allows it. Prefer recording the upstream repository URL, checked commit, and local modification strategy.

## OpenKoda source information

| Item | Value |
|---|---|
| Upstream repository | `<TO_BE_FILLED>` |
| License | `<TO_BE_FILLED>` |
| Checked commit or release | `<TO_BE_FILLED>` |
| Verification date | `<TO_BE_FILLED>` |

## Local source strategy

The preferred strategy is:

1. Verify upstream OpenKoda without modification.
2. Document required operational hardening.
3. Apply local modifications in `app/` or in a clearly identified fork/branch.
4. Keep every modification tied to an issue and verification evidence.

## Required notes before modification

- Why the modification is required
- Which upstream files are affected
- How the modification is verified
- Whether the change is operational or functional
- Related issue and PR
