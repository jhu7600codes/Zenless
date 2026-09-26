# Zenless

holo themed clicker game for android, with doors-style entities that show up while you tap.

- pure java, no androidx, `Theme.Holo.NoActionBar` straight from the framework (minSdk 19, targetSdk 35), immersive fullscreen
- tap the big blue circle for holos, buy buildings in the shop, tiered upgrades, rebirth for superterrestrial items
- a door opens every 30s, each tap on the circle brings it 0.25s closer
- doors/rooms style spawns: each entity has a scripted first meeting, then a small chance per door
- first rebirth unlocks a per-save admin panel toggle (off by default) to spawn entities manually

## spawns

| entity | first meeting | after that |
|---|---|---|
| rush | door 10 | 8% per door |
| a-90 | door 30 | 6% |
| figure | door 50 and door 100 | 2% after door 100 |
| a-90b | door 70 | 4% |

0.001% of meet-and-greet doors bring a SUPER version (red tint, half the reaction time, double penalty and reward),
and door 1 has a 0.001% chance of `???`. admin panel: tap a spawn button for the normal entity, hold it for the SUPER one.

the 2 doors after an encounter are quiet (scripted meetings still happen). tweak it all in `enemy/SpawnTable.java`.

## difficulty

asked at the start of every run (first launch and after each rebirth):

| mode | entities | prices |
|---|---|---|
| easy | none, pure farming | 20% cheaper |
| normal | only figure, every 30 doors | 20% cheaper |
| hard | all, chances above | normal |
| extreme | double chances, a-90b 4x in his original sprites | normal |
| SUPER HARD MODE | chances +547%, doors 3x faster | +50% prices, upgrades and buildings 50% weaker |

## build

```
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest
```

needs an android sdk with platform 35 (`local.properties` -> `sdk.dir=...`).

## entities

| entity | what to do | fail |
|---|---|---|
| A-90 | shows for 1s (touches ignored), then a stop sign for 2.8s. don't touch anything, or tap the sign's hand | distorted jumpscare, -1/8 holos |
| A-90B | ~15s of HALT (hold) / PROCEED (let go + tap) | distorted jumpscare, -1/6 holos |
| Rush | rumble + flickering lights, be holding when it arrives until it's gone | lunge jumpscare, -1/14 holos |
| Figure | 50s, hold while it approaches, let go while it retreats | lunge jumpscare, -1/5 holos |

new entities: subclass `enemy/Enemy` (spawn / tick / resolvePlayerAction / onFail / onSuccess) and add it to `enemy/EnemyType`.

## textures

`app/src/main/assets/textures/<enemy>/` keeps the exact names from `textures.zip`
(android drawable names can't contain `-`, so they're loaded from assets via `Textures.java`).
the animated figure textures are split into frame folders (`figure/figure-idle/000.webp` ...)
so they animate on every api level. the launcher icon is `icon.jpg` scaled into `mipmap-*`.
## sounds

`app/src/main/assets/sounds/<enemy>/`, taken from the [doors wiki](https://doors-game.fandom.com)
(a-90, rush, figure) and the [rooms revisited wiki](https://rooms-revisited.fandom.com) (a-90b).
`PlaySound (Rush)` is trimmed to the warning length with a fade in, halt/proceed have their silence cut.
the survive chime is synthesized in `Sfx.java`. all game sounds and textures belong to their original creators,
this is a non-commercial fan project.
