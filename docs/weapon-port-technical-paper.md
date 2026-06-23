# SMP Weapons 1.0.0 Technical Paper

## Scope

SMP Weapons replaces the unstable Skript weapon system with one self-contained Bukkit-family plugin. Scope is weapons only. Menus and custom commands exist only to list and distribute weapons.

Out of scope: `/daily`, `/bc`, unrelated SMP scripts, external item plugins, external menu plugins, database storage, GitHub Actions.

## Product problem

The server was losing player trust because Skript-backed legendary weapons, menus and command visibility failed intermittently. Existing weapons such as Dash Trident, Rocket Mace and older legendary items stopped working after time. The product owner wanted the weapons moved into a compiled plugin to keep behavior consistent and make future weapons configurable.

## Compatibility contract

Old weapons must keep their identity. The plugin recognizes legacy items by custom model data where the Skript used it and by stripped display-name match where the Skript had no model data.

Default FFA weapons preserve the original visible title, visible lore and custom model data values:

| Weapon | Legacy custom model data |
|---|---:|
| Dash Sword | 23425 |
| Dash Mace | 23426 |
| Wind Spear | 23427 |
| Venom Sword | 23428 |
| Cobweb Axe | 23429 |
| Viking Spear | 23430 |
| Gravity Axe | 23431 |
| Rocket Spear | 23432 |

Second Skript weapons are preserved by legacy display-name matching:

- Slam Mace
- Zoom Mace
- Tide Trident
- Explosive Mace
- Flow Spear
- Warrior Spear

## Configuration model

General settings live in `config.yml`.

Messages live in `messages.yml` and are fully configurable. Defaults avoid internal implementation wording.

Menus live in `menus.yml`. Menus can register custom commands such as `/ffaweapons`. Command-map registration is used for tab visibility, and command preprocess interception is also present as a fallback.

Weapons live in `weapons/*.yml`. Each file may define one or multiple weapons. Server owners can copy files, split files, or add `more_weapons.yml` style packs.

## Default commands

- `/smpweapons menu [menu]`
- `/smpweapons list`
- `/smpweapons get <weapon> [amount]`
- `/smpweapons give <player> <weapon> [amount]`
- `/smpweapons create <weapon>`
- `/smpweapons delete <weapon>`
- `/smpweapons cooldown [player]`
- `/smpweapons reload`
- `/ffaweapons`

## Ability templates

The plugin ports the Skript behavior into reusable ability templates. These templates are configurable per weapon:

- `DASH_AOE`
- `VENOM_DASH`
- `COBWEB_FIELD`
- `SELF_BUFF`
- `GRAVITY_LIFT`
- `ROCKET_LIFT`
- `SLAM_MACE`
- `ZOOM_MACE`
- `TIDE_TRIDENT`
- `EXPLOSIVE_MACE`
- `FLOW_SPEAR`
- `NONE`

`NONE` is used for custom created weapons until the owner assigns an ability template in YAML.

## Server support

The project compiles against Bukkit/Spigot 1.8 API and uses reflection for modern-only item identity, particles, cooldowns, gliding, glowing and Folia schedulers. This keeps old server classloading safe while still enabling modern behavior when the runtime supports it.
