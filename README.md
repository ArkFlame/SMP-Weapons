# SMP Weapons

SMP Weapons is a self-contained configurable legendary weapons plugin for Bukkit, Spigot, Paper and Folia servers.

## Build

```bash
mvn clean package
```

The compiled plugin jar is created at:

```text
target/SMPWeapons-1.1.0.jar
```

## Server install

1. Drop the jar into `plugins/`.
2. Start the server once.
3. Edit:
   - `plugins/SMPWeapons/config.yml`
   - `plugins/SMPWeapons/messages.yml`
   - `plugins/SMPWeapons/menus.yml`
   - `plugins/SMPWeapons/weapons/*.yml`
4. Run `/smpweapons reload`.

## Commands

```text
/smpweapons menu [menu]
/smpweapons list
/smpweapons get <weapon> [amount]
/smpweapons give <player> <weapon> [amount]
/smpweapons create <weapon>
/smpweapons delete <weapon>
/smpweapons cooldown [player]
/smpweapons reload
```

Default custom menu command:

```text
/ffaweapons
```

## Weapon files

Weapons are loaded from every `.yml` file under `plugins/SMPWeapons/weapons/`. A file may contain one weapon or many weapons.

Default files:

```text
weapons/default-weapons.yml
weapons/more-weapons.yml
weapons/custom-weapons.yml
```

Old Skript weapons are recognized by their original display names and custom model data where available. The default generated FFA weapons preserve the original titles, lore and custom model data values from the provided Skript.


## 1.0.1 Update

SMP Weapons 1.0.1 adds the first configurable engine layer required by the weapon-port report: projectile-backed cobweb bombs, temporary/fake block ownership, cached shape waves, old getter commands, and a minimal timeline DSL for custom weapons.

The bundled weapons remain self-contained and loaded from `weapons/*.yml`. The old FFA custom model data and old display-name based weapons are preserved by default.


## 1.0.2 Update

SMP Weapons 1.0.2 hardens the 1.0.1 port for client delivery. It adds validation reporting, duplicate-safe weapon loading with `override: true`, debug/admin commands, per-weapon cooldown reset, menu pagination, virtual block refresh on join/world change, chunk-unload block cleanup, real projectile cleanup, expanded projectile carriers, lore-fragment legacy matching, and map-style timeline actions for owner-created weapons.


## 1.0.3 Update

SMP Weapons 1.0.3 adds another original-spec completion pass: timeline range schedules like `10..40 step 2`, richer map DSL actions for damage/raw damage/selectors/potion targets/particle shapes/gliding/glowing/text/commands, configurable menu click actions, trigger variants for right-click and left-click weapons, cooldown bypass permissions, virtual block refresh while moving near active visuals, and projectile cleanup scoped to the caster on quit, death, and world changes.


## 1.0.4 Update

SMP Weapons 1.0.4 adds the next original-spec completion pass: YAML v2 trigger maps with event/condition lists, named trigger timelines, cooldown-map parsing, offhand-aware reflection checks for modern servers while preserving 1.8 compile shape, per-weapon use permission gating, atomic reload preservation when validation errors happen after a previous good registry is active, global duplicate getter-command validation, projectile caps, generic `spawn_projectile` map action routing, `damage_once`, `restore_block`, `sync_block`, `cooldown_set`, `cooldown_reset`, `remove_projectile`, richer selector filters, origin offsets, and expanded cached shape primitives.


## 1.0.7 Update

SMP Weapons 1.0.7 fixes the final Rocket Spear glide gap and tightens enabled-weapon surfaces. Rocket Spear now starts a forced glide session at the detected lift peak/fall transition, repeatedly sustains the server gliding state, and cancels glide-off toggles while the weapon glide session is active on servers that expose `EntityToggleGlideEvent`. This keeps the no-Elytra glide behavior closer to the original Skript visual feel.

Example weapons remain `enabled: false`, and enabled-only filtering is now enforced at registry/list/menu/dynamic-command surfaces so examples do not appear or register getter commands by default. The original Skript weapon names now use small-capital display IDs and item names consistently across the default and FFA2 weapon packs, while normal legacy names remain as matching aliases where needed.


## 1.0.8 Notes

- Cooldown bypass permissions are disabled by default through `settings.cooldown-bypass-enabled: false`.
- Cobweb Axe supports `replace_blocks` to remove real cobwebs after the packet-side sphere finishes expanding, while immediately resyncing tracked fake cobwebs.

## 1.0.9 client weapon pack

This release bundles `weapons/client-weapons.yml` with Force Bow, Boom Crossbow, Repell Shield, Ultratotem, Flux Sword, Zero Point, and Heavy Core. It also adds shoot triggers, shield-block passives, inventory passives, totem-pop activation, beam timelines, pull timelines, and banner-pattern item support.
