# Changelog

All notable changes to ObsidianDragon will be documented in this file.

---

## [0.1.1] - 2026-01-06

### Added
- `/dragon editor` command that opens the in-game loot editor.
- In-game loot editor: change loot (name, amount, lore) and sync changes with `loot.yml`.
- Edit item amount and sort items (by chance) from the editor.
- Dynamic pagination for large loot lists and improved, clearer menu layout.

---

## [0.1.0-alpha] - 2026-01-03

**Initial alpha release** - Basic dragon management features for your server.

### Added
- `/obsidiandragon` command with four subcommands:
  - `/dragon menu` - Opens an interactive GUI to manage the dragon
  - `/dragon spawn` - Manually spawn the Ender Dragon
  - `/dragon kill` - Instantly kill the dragon (with configurable cooldown)
  - `/dragon reload` - Reload configuration without restarting
- **Economy integration** - Optional cost to spawn dragons (supports Vault and CoinsEngine)
- **Custom loot system** - Configure custom drops when the dragon dies
- **Permission system** - Control who can spawn, kill, or manage the dragon
- Full configuration file with customizable messages and costs

### Notes
- This is an **alpha release** and may contain bugs
- Requires Minecraft 1.21 or higher
- Economy features require Vault or CoinsEngine (optional)
- Feedback and bug reports are appreciated!

---
