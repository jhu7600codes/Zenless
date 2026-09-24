# Zenless

holo themed clicker game for android, with doors-style entities that show up while you tap.

- pure java, no androidx, `Theme.Holo.NoActionBar` straight from the framework (minSdk 19, targetSdk 35), immersive fullscreen
- tap the big blue circle for holos, buy buildings in the shop, tiered upgrades, rebirth for superterrestrial items
- every 30s a door opens: 51% nothing, 49% an entity (rush / a-90b / figure / a-90 by a 0-100 roll)
- first rebirth unlocks a per-save admin panel toggle (off by default) to spawn entities manually

## difficulty

asked at the start of every run (first launch and after each rebirth):

| mode | entities | prices |
|---|---|---|
| easy | none, pure farming | 20% cheaper |
| normal | only figure | 20% cheaper |
| hard | all, normal 49% door spawns | normal |
| extreme | 98% door spawns, more a-90b in his original sprites | normal |
| SUPER HARD MODE | every door (+547%), doors 3x faster | +50% prices, upgrades and buildings 50% weaker |

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
all sounds are synthesized at startup in `Sfx.java` since the zip has no audio.
