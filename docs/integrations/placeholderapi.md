---
title: PlaceholderAPI
parent: Integrations
nav_order: 2
---

## Overview

When [PlaceholderAPI](https://modrinth.com/mod/placeholderapi) is installed,
EzFlyTime registers a set of placeholders that expose fly-time values. These can
be used in scoreboards, chat formats, GUIs (e.g. DeluxeMenus), and any other
plugin that supports PAPI.

Without PlaceholderAPI, EzFlyTime runs normally - placeholders simply remain
unregistered and other plugins will display them as raw text.

---

## Setup

1. Install PlaceholderAPI.
2. Start the server. EzFlyTime registers its placeholders automatically on startup.
3. Confirm with `/papi list` - `ezflytime` should appear in the list.

---

## Available placeholders

| Placeholder | Description |
| :--- | :--- |
| `%ezflytime_time_remaining%` | Formatted remaining flight time (e.g. `1h 2m 3s`) |
| `%ezflytime_formatted%` | Alias for `time_remaining` |
| `%ezflytime_time_remaining_raw%` | Raw remaining seconds as a plain integer |
| `%ezflytime_seconds%` | Alias for `time_remaining_raw` |
| `%ezflytime_minutes%` | Whole minutes remaining (floor division) |
| `%ezflytime_fuel%` | Remaining flight as a percentage (0–100, fuel mode) |

---

## Notes

- `%ezflytime_fuel%` returns a percentage regardless of the `display.flytime-mode`
  setting - it is always calculated from the ratio of remaining to maximum time.
- Players with `ezflytime.bypass` and `bypass-grants-unlimited: true` will have
  these placeholders return `∞` or the configured unlimited display string.
