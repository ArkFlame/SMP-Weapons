# SMP Weapons 1.0.5 Changelog

## Production goal

Continue closing the original spec gap by making the DSL context-aware and making projectile/passive behavior run through YAML timelines instead of hardcoded-only paths.

## Added

- Multiple `triggers:` entries per weapon can now match independently.
- Per-trigger cooldown keys and cooldown seconds are supported through `cooldowns.<key>.seconds`.
- Trigger timelines execute directly from the matched trigger.
- Top-level `projectiles:` definitions are supported in addition to legacy `ability.projectiles`.
- Projectile `on_tick`, `on_hit`, and `on_expire` timeline hooks are supported.
- Projectile timeline context now exposes `projectile`, `impact_location`, and `hit_entity`.
- `passives:` map support was added for hit passive definitions with chance, conditions, and timeline execution.
- Passive timeline context exposes `victim` / `hit_entity`.
- DSL selectors now understand `victim`, `hit_entity`, and contextual projectile/impact origins.
- `temporary_block` and `virtual_block` can place configured shapes, not only a single block.
- New `shape_block` action alias for shaped temporary block placement.
- Cached block shapes now include ellipsoid, cone, disc/ring, and wall.
- Config validation checks missing trigger/passive/projectile timeline references and projectile lifetime caps.

## Notes

- This remains self-contained. No Skript, WorldEdit, ProtocolLib, PacketEvents, or external menu plugin dependency was added.
- Maven compile was not run in the container because Maven is not installed.
