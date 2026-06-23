# SMP Weapons 1.0.6

Production alignment pass for final Skript parity issues.

## Changed

- Disabled example weapons by default and kept examples only as developer templates.
- Removed default/op cooldown bypass behavior. Cooldown bypass now requires an explicit bypass permission grant.
- Added `settings.require-use-permission`, default `false`, so normal players can use owned weapons like the original Skripts.
- Client item cooldown now tries modern ItemStack cooldown first, then Bukkit Material cooldown fallback.
- Temporary weapon blocks now default to packet-only virtual blocks through `sendBlockChange`.
- Added stacked virtual block tracking per block position so overlapping cobweb bombs/spheres unwind independently.
- Added packet cleanup smoke through `virtual-blocks.cleanup-particle`, default `WHITE_SMOKE`.
- Added `virtual-blocks.force-packet-only`, default `true`.
- Added cached MiniMessage/legacy rendering in `TextBridge` so item names/lore are decoded once and reused.
- Normalized bundled weapon YAML text away from legacy `&` formatting toward MiniMessage-compatible tags.
- Added colored dust particle data through reflective Bukkit/Paper `DustOptions` support.
- Fixed internal particle helpers that ignored configured particle names and always displayed cloud particles.
- Added Rocket Spear `gliding-at-peak` config option and aligned default behavior with the original Skript peak glide.

## Notes

- Existing real cobweb clearing for Slam Mace remains because it is source-parity behavior for clearing real cobwebs in the arena.
- Packet-only cobweb visuals do not provide server-side collision. This matches the requested no-real-block behavior for placed skill visuals.
