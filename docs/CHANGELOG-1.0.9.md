# SMPWeapons 1.0.9

## Added

- Bundled `weapons/client-weapons.yml` with the client-requested weapon pack.
- Force Bow shoot-power dash ability.
- Boom Crossbow vanilla projectile impact tracking with AOE explosion timeline.
- Repell Shield shield-block reflection passive.
- Ultratotem pop-trigger timeline through reflective resurrect event.
- Flux Sword darkness passive and beam timeline action.
- Zero Point inventory passive scanner.
- Heavy Core pull timeline action.
- Banner pattern support for shield-style items.

## Compatibility

Existing YAML remains compatible. The modern-only totem pop event is registered reflectively to preserve legacy startup. New mechanics use existing timelines, projectiles, and passives where possible.