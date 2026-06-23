# SMP Weapons 1.0.7

## Changed

- Added Rocket Spear forced glide sustain. The plugin now keeps a short active glide session after the lift peak and cancels server glide-off toggles while the session is active on servers that expose `EntityToggleGlideEvent`.
- Rocket Spear now starts glide at the detected peak/fall transition using `glide-start-velocity-y`, with `duration` as fallback.
- Fixed Rocket Spear trail startup so it does not immediately terminate on the same tick the launch velocity is applied while Bukkit still reports the player as on ground.
- Added Rocket Spear config keys `glide-sustain-ticks` and `glide-start-velocity-y`.
- Disabled example weapons remain `enabled: false`; enabled weapon surfaces now filter through enabled weapons only, so examples do not list, register get commands, or appear in all-weapons menus.
- Default FFA weapon display names and display IDs now use small-capital lettering to match the original Skript weapon style. Legacy normal-name matching remains as a compatibility alias.
