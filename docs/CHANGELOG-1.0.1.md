# SMP Weapons 1.0.1

## Scope

This update ports the full weapon set from the supplied Skript report into the plugin runtime and adds the first configurable systems required by the feature-definition report.

## Added

- Cobweb projectile ability type: `COBWEB_PROJECTILE`.
- Projectile runtime for snowball-backed custom projectiles.
- Projectile hit handling for entity and block impacts.
- Temporary block service with a `created_blocks` registry.
- Real temporary block placement with ownership-safe restore.
- Fake/virtual block send support through `Player#sendBlockChange` reflection.
- `sync_block` behavior when players interact with a virtual block location.
- Cached sphere shape engine.
- Expanding and collapsing hollow cobweb sphere wave.
- Hybrid block mode: real inner radius, fake outer shell.
- Owner-facing disabled examples in `weapons/examples.yml`.
- Legacy getter command registration for `/getslammace`, `/getzoommace`, `/gettidetrident`, `/getexplosivemace`, `/getflowspear`, and `/getwarriorspear`.
- Command-preprocess fallback for those getter commands if command-map registration fails.
- Minimal timeline DSL ability type: `TIMELINE` with string actions for sound, particle, velocity, potion, temp block, and shape wave.

## Changed

- Version bumped to `1.0.1`.
- Cobweb Axe default active ability now uses projectile cobweb bomb behavior.
- Raw damage now subtracts Bukkit health points directly instead of halving the configured value.
- Zoom Mace uses normal Bukkit damage for its impact damage, matching the Skript behavior.
- Spear material aliases now include `NETHERITE_SPEAR` before fallback options.
- Real temporary blocks are restored on reload and immediate restore is attempted on disable.

## Validation

- YAML resources parsed successfully with Python YAML parser.
- Maven compile was not run in this container because `mvn` is not installed.
