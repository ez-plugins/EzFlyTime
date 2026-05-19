---
title: TeamsAPI
parent: Integrations
nav_order: 4
---

## Overview

When [TeamsAPI](https://modrinth.com/plugin/teams-api) is installed, EzFlyTime
integrates with the team plugin to provide two features:

1. A `/f fly` subcommand registered inside the team command tree, so players can
   toggle EzFlyTime flight without leaving their team-command workflow.
2. An optional `claimed-chunks-only` restriction that prevents EzFlyTime-managed
   flight outside a player's own team-claimed chunks.

Without TeamsAPI, EzFlyTime runs normally - the subcommand and chunk restriction
are simply not registered.

---

## Setup

1. Install [TeamsAPI](https://modrinth.com/plugin/teams-api) and at least one
   TeamsAPI-compatible team plugin.
2. Start the server. EzFlyTime detects TeamsAPI at startup and logs
   `Registered TeamsAPI /fly subcommand.` when successful.
3. No additional EzFlyTime configuration is required for the `/f fly` subcommand.

To disable the integration while keeping TeamsAPI installed, set:

```yaml
# config.yml
teams:
  enabled: false
```

---

## `/f fly` subcommand

Once registered, players can toggle EzFlyTime flight from the team command:

```text
/f fly
```

The command behaves identically to `/fly`: it toggles flight on or off and
consumes from the player's fly-time balance. The `ezflytime.fly` permission is
required (default: `true`).

The subcommand name used in the examples above (`/f`) depends on which team plugin
is installed - TeamsAPI registers the subcommand under whatever command the
provider exposes.

---

## Claimed-chunks-only restriction

When enabled, EzFlyTime-managed flight is limited to chunks claimed by the
player's own team. Attempting to enable flight in unclaimed territory is blocked,
and flying into an unclaimed chunk automatically disables flight.

Enable in `config.yml`:

```yaml
teams:
  enabled: true
  claimed-chunks-only: true
```

**Requirements:**

- A TeamsAPI-compatible team plugin that registers a
  [TeamsClaimService](https://ez-plugins.github.io/teams-api/api.html#teamsclaimservice-interface)
  provider. If no claim provider is registered the restriction is silently skipped
  and flight is unrestricted.
- The player must belong to a team. Players without a team cannot fly when this
  option is active.

**Behavior summary:**

| Situation | Result |
| :--- | :--- |
| Player in team, chunk claimed by their team | Flight allowed |
| Player in team, chunk unclaimed (wilderness) | Flight blocked / stopped |
| Player in team, chunk claimed by a different team | Flight blocked / stopped |
| Player not in any team | Flight blocked / stopped |
| No claim provider registered | No restriction (fail-open) |
| TeamsAPI absent | No restriction |

Players with the `ezflytime.teams.fly.bypass` permission are exempt from the
restriction regardless of chunk ownership.

---

## Permissions

| Permission | Default | Description |
| :--- | :--- | :--- |
| `ezflytime.fly` | `true` | Required to use `/f fly` (same node as the `/fly` command) |
| `ezflytime.teams.fly.bypass` | `false` | Exempt from `claimed-chunks-only` restriction |
