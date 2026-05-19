# mcMMO Integration Configuration

This plugin supports a dedicated `mcmmo.yml` to control mcMMO-related auto-reward behavior.

Location: `mcmmo.yml` in the plugin data folder. A default is bundled with the plugin.

Example keys:
- `enabled` (boolean): enable/disable mcMMO integration.
- `permission-bypass` (string): permission node to bypass mcMMO-based rewards.
- `global-multiplier` (number): global multiplier applied to mcMMO-derived rewards.
- `skills` (map): per-skill entries with `thresholds` (level -> seconds) and optional `multiplier`.

If `mcmmo.yml` is missing, the plugin will fall back to the legacy `auto-flight-rewards.mcmmo` section in `config.yml`.
