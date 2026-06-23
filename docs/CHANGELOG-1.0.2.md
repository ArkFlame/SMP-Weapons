# SMP Weapons 1.0.2

Production-readiness update after the first full weapon port.

## Added

- Config validation report for weapon loading.
- Duplicate weapon protection with explicit `override: true` support.
- `/smpweapons debug validate`, `/smpweapons debug item`, `/smpweapons debug blocks`, and `/smpweapons debug restoreblocks`.
- Per-weapon cooldown reset through `/smpweapons cooldown <player> <weapon|all>`.
- Menu pagination for large weapon lists.
- Virtual block refresh on player join and world change.
- Temporary block restore on chunk unload.
- Real projectile cleanup on reload/disable/expire.
- Projectile carrier support for snowball, arrow, egg, ender pearl, and fireball with snowball fallback.
- Map-style owner DSL actions for `TIMELINE` abilities, alongside the existing compact string actions.
- Lore-fragment legacy matching support.

## Fixed

- Modern fake-block visuals now send the requested material block data instead of accidentally resending the real block.
- Reload now cancels old scheduled tasks after restoring temporary state to avoid stale cooldown/projectile/block tasks.
- Weapon validation no longer silently overwrites duplicate weapon ids from later files.
- Runtime now uses `engine.max-air-loop-ticks` and `engine.max-target-distance` when present.

## Notes

Maven compile was not run in the generation container because Maven is not installed there. YAML resources were parsed and source packaging was verified.
