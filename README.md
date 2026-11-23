# Ragdoll Remains (Fabric 1.21.11)

Client-side mod: when a mob dies it turns into a physics ragdoll corpse you can walk into / sprint into to push around.
Works on any server (vanilla included) because it is purely client-side. Needs only Fabric Loader (no Fabric API).

## Build the jar with GitHub
1. Create a new GitHub repo and upload everything in this zip (keep the `.github` folder).
2. Go to the **Actions** tab -> "build" runs automatically (or press *Run workflow*).
3. Open the finished run -> **Artifacts** -> download `killragdoll-jar`, unzip, put the jar in `.minecraft/mods`.

## Tweaks
`src/main/java/com/killragdoll/CorpseManager.java`: `LIFETIME_TICKS`, `MAX_CORPSES`, and push strength in `step()`.

If the build fails with "could not resolve", update `loom_version` / `loader_version` in `gradle.properties` from https://fabricmc.net/develop
