# SMP Weapons 1.0.8

- Bumped project version to 1.0.8.
- Added `settings.cooldown-bypass-enabled` defaulting to `false`; bypass permissions no longer skip ability cooldowns unless this config key is explicitly enabled.
- Added Cobweb Axe `replace_blocks` support for the cobweb bomb impact.
- Default Cobweb Axe now removes real `WEB`/`COBWEB` blocks in the configured sphere after the visual wave delay.
- Real block replacement immediately resends the currently tracked packet-side fake block at the same location, so active virtual cobwebs stay visible while server-side old cobwebs are cleared underneath.
