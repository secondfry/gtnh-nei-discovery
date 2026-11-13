# Build validation — 2026-11-13

The main jar targets Java **25**, Minecraft 1.7.10 and NEI **2.8.171-GTNH**. Verified class-file major version is **69** (Java 25), including test classes. The jar contains addon classes, `mcmod.info` and the NEI option translations; it does not bundle NEI or Minecraft.

Completed on the final Java 25 target: `spotlessApply build`, including Spotless checks, Checkstyle, compilation, reobfuscation and **11 passing JUnit tests** (4 discovery graph tests, 4 item identity tests, 2 recipe-matching tests, 1 Forge generated-event-handler regression). `git diff --check` also passes.

Installable artifact: `build/libs/neidiscovery-0.1.2.jar` (19,690 bytes). SHA-256: `989f046bb409a6534ed6c58f4812a4e6dd5896a9c4a3f46611041edfd85cccfe`.

Tests cover the pickaxe example, alternative ingredients, direct/chained recipes, unseeded cycles, empty recipes, independent knowledge sets, immutable snapshots, GregTech tool wear versus material/type, ordinary tool durability and NBT distinctions.

Version 0.1.0 crashed because its package-private `DiscoveryClient` could not be accessed by Forge's generated event-handler class in another class loader. `DiscoveryEventHandlerTest` reproduced that exact `IllegalAccessError` using the real Forge `ASMEventHandler`, then passed after making `DiscoveryClient` public. Version 0.1.1 uses package `com.secondfry.mc.neidiscovery`; the built jar was checked for the new class path and absence of the old package.

Version 0.1.2 reproduces and fixes a tagged inventory mallet being accepted by NEI Craftables' item/metadata comparison but rejected by Discovery's exact ingredient keys. The isolated regression uses the real NEI comparator and the production discovery graph; it does not claim to replay the user's exact registered Wooden Helmet recipe. Generic ingredient keys can now match observed items without NBT, while explicitly NBT-bearing recipe ingredients retain their requirements. Observed-item/output identities remain exact.

Non-crafting handlers now require an actually observed NEI workstation catalyst. Derived machines do not count, even with chains enabled. Unknown workstation metadata hides inferred outputs, while directly observed outputs remain visible. Tests cover unobserved machines, derived machines, known machines, and explicit hammer material requirements. Catalyst sets are cached per handler and shared across recipes. This gates categories, not individual recipe voltage or multiblock construction.

Read-only inspection of the user's current client log found `Indexed 281395 recipes from 366 NEI handlers` and no addon recipe-skip errors. No client files were changed. A full handler-level test harness was attempted but requires Forge fluid registries and Minecraft rendering initialization; the regression instead tests the graph and item identities without faking those systems.

Not verified in a launched GTNH client: plugin discovery/menu rendering, full recipe-handler coverage, fluid registration/display integration, discovery-file round trips with Forge registries, and performance with the entire pack. The initial registry-dependent unit checks required Forge's LaunchClassLoader and were replaced with isolated identity checks; registry integration remains an in-game check. No client or server instance was modified.

## Reproduce on this workspace

From the workspace root in PowerShell:

```powershell
$init = (Resolve-Path .research/nei-discovery/mirror.init.gradle).Path
$jdks = (Get-ChildItem .research/nei-discovery/jdks -Directory | ForEach-Object FullName) -join ','
.\nei-discovery\gradlew.bat -p nei-discovery -I $init build --console=plain "-Porg.gradle.java.installations.paths=$jdks" '-Porg.gradle.java.installations.auto-download=false'
```

The local init script supplies a small Maven mirror under `.research/nei-discovery/maven/` for stalled GTNH artifact downloads. Those jars were retrieved from the original GTNH Maven URLs with a download query parameter. The Java 8, 17 and 21 toolchains are unpacked under `.research/nei-discovery/jdks/`; system Java 25 runs Gradle. These are build-environment workarounds, not runtime dependencies of the addon. On a machine with normal repository access and working toolchain auto-provisioning, `gradlew.bat build` is sufficient.
