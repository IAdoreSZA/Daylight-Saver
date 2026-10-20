# Daylight-Saver

A Paper plugin for Minecraft 26.3 that changes how long Minecraft days and nights last **without** speeding up or slowing down anything else. Crops, mobs, redstone and furnaces all still run at normal speed. Only the sun and moon move slower (or faster).

> **AI disclosure:** This plugin was written with [Claude Code](https://claude.com/claude-code), an AI coding tool. If you'd rather not use AI-made software, you may want to skip this one.

## Download

Grab the latest jar from the [Releases](https://github.com/IAdoreSZA/Daylight-Saver/releases) page. Each release says which Minecraft version it's for (currently **26.3**, requires Java 25). Drop it into your server's `plugins` folder and restart.

## Configuration

`plugins/DaylightSaver/config.yml`:

```yaml
day-length: 20.0     # minutes from sunrise to sunset   (vanilla: 10)
night-length: 20.0   # minutes from sunset to sunrise   (vanilla: 10)

worlds:              # optional per-world overrides
  world:
    day-length: 30.0
    night-length: 10.0

disabled-worlds: []  # worlds to leave alone
```

The defaults give a 40-minute full cycle (vanilla is 20). Values below 10 make that part of the cycle faster than vanilla.

## Commands

| Command | Description |
| --- | --- |
| `/daylightsaver status` | Show the day and night lengths for each world |
| `/daylightsaver reload` | Reload `config.yml` |

Alias: `/dls`. Permission: `daylightsaver.admin` (ops by default).

## Notes

- Sleeping, `/time set` and `/time pause` all still work normally.
- Leave the vanilla `/time rate` at 1. If it is changed, the plugin steps aside and lets vanilla control the clock.
- The Nether and the End are skipped because they have no day cycle.
- Times are based on 20 TPS. If the server lags, days get longer, just like in vanilla.

## Building

Requires Java 25 and Maven:

```
mvn package
```

The jar is written to `target/DaylightSaver-<version>.jar`. Every push to GitHub also builds the jar automatically (see the **Actions** tab).
