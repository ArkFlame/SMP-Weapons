# SMP Weapons 1.0.4

Original-spec completion loop focused on YAML engine completeness and safer production reload behavior.

## Added

- YAML v2 trigger-map parsing through `triggers.<id>.events`, `conditions`, `timeline`, and `cooldown`.
- Cooldown-map parsing through `cooldowns.<id>.seconds`.
- Named trigger timelines under `ability.timelines.<name>`.
- Offhand-aware trigger condition checks through reflection, without direct modern-only Bukkit imports.
- Per-weapon use permission gate: `smpweapons.weapon.<weapon>.use`.
- Atomic reload preservation when a reload has validation errors and an older good weapon registry is already active.
- Global duplicate getter-command validation across weapon files.
- Projectile caps for per-player and global active projectile counts.
- Generic map `spawn_projectile` routing by projectile id.
- Additional timeline actions: `damage_once`, `restore_block`, `sync_block`, `cooldown_set`, `cooldown_reset`, and `remove_projectile`.
- Selector filters for nearest sorting, line-of-sight, max-targets, nearby players, nearby living entities, and self plus nearby.
- Origin map offsets for DSL actions.
- Cached block-shape primitives for sphere, cube, cuboid, and cylinder.
- Expanded `particle_shape` support for helix, spiral, arc, sphere, random-in-sphere, and random-on-shell.

## Notes

- This remains self-contained. No PacketEvents, WorldEdit, ProtocolLib, Skript, or external menu dependency was added.
- Maven was not available in the patching container, so final build must be run in a real Maven environment.
