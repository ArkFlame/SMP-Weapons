# SMP Weapons 1.0.3

Production-readiness loop focused on original-spec gaps left after 1.0.2.

## Added

- Timeline range schedules such as `10..40 step 2`.
- Map-style DSL selectors for caster, nearby living entities, nearby players, and self plus nearby.
- Map-style DSL actions for damage, raw damage, targeted potion effects, clear effects, particle shapes, gliding, glowing, virtual blocks, messages, action bars, titles, and command dispatch.
- Configurable menu `click-actions` with get/give, close, open menu, player command, and console command actions.
- Right-click, left-click, sneak, and not-sneak trigger matching through `trigger.type`.
- Cooldown bypass permission support with `smpweapons.bypasscooldown.<weapon>` and `smpweapons.bypasscooldown.*`.
- Optional virtual block refresh while moving near active fake blocks.
- Per-caster projectile cleanup on quit, death, and world changes.

## Notes

- Offhand swap remains compatibility-sensitive because the project compiles against old Bukkit. Use a right-click/offhand-style configured trigger alias only after a modern-only compatibility module is added.
- Maven was not available in the patching container, so final build must be run in a real Maven environment.
