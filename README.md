# NEI Discovery

Client addon for Minecraft 1.7.10 / GTNH 2.9.0, built against **NEI 2.8.171-GTNH**.

Java package: `com.secondfry.mc.neidiscovery`.

Requires **Java 25 or newer** and GTNH's modern-Java launcher setup. Java 8/17/21 cannot load this addon.

MIT licensed. Copyright © 2026 Rustam @SecondFry Gubaidullin. The upstream build scaffolding retains its original attribution; see [LICENSE](LICENSE).

## What appears in NEI

With **NEI Options → Inventory → Discovery → Filter undiscovered items** enabled (the default), the normal item list and its search results contain:

- Item variants seen in your inventory, armor slots, cursor, or the server-synchronized slots of a container you open.
- Outputs of NEI recipes for which every ingredient slot has at least one previously seen alternative. Non-crafting recipes also require a workstation from that NEI category to have actually been seen in an inventory; merely being craftable does not unlock a workstation.

Thus searching `pickaxe` shows previously seen pickaxes and recipes with known ingredient types, rather than every pickaxe in the pack. Ore-dictionary alternatives exposed by NEI are respected. Looking at NEI's item list or recipe display does **not** itself discover the displayed ingredients.

**Include recipe chains** is off by default. Turn it on to repeatedly include outputs as ingredients: a known log can reveal planks, then sticks, then recipes using those. Unseeded recipe cycles do not unlock themselves. Turning either option off immediately rebuilds the filter; discovery tracking continues while filtering is off.

This is an ingredient-discovery filter, not an autocrafting or progression simulator. Counts are intentionally ignored: an ingredient discovered yesterday remains known even when you have none left. NEI workstation catalysts gate non-crafting categories. Categories without catalyst metadata stay hidden until their outputs are observed directly. Machine voltage, multiblock assembly, power, research and chance are not simulated; encountering one category catalyst does not establish that every recipe in that category can run. Machines placed before installing the addon need to be observed as inventory items to count. A visible output means its displayed ingredient types are known; it is not a guarantee you can produce it immediately. Existing NEI recipe browsing is unchanged.

## Install

Version **0.1.2** fixes generic recipe ingredients rejecting inventory tool NBT and adds observed-workstation requirements. Specific NBT-bearing recipe ingredients still retain material requirements. Only directly observed items are saved; inferred dust from older versions disappears automatically when no valid recipe unlocks it. Remove older NEI Discovery jars before installing this version.

1. Build with `./gradlew build` (Windows: `gradlew.bat build`). The wrapper uses Gradle 9.5.1 and Java 25 for Gradle, addon compilation and tests. The GTNH build also requests older toolchains for preparing Minecraft's development classes; those do not set the addon's runtime target.
2. Put `build/libs/neidiscovery-0.1.2.jar` in the GTNH **client instance's** `mods/` folder. Do not install a `-sources`, `-dev`, or `-javadoc` jar.
3. Start GTNH and open your inventory. The first recipe index builds incrementally after NEI's item list finishes loading. Inventories are sampled every client tick, with filter calculations at most once per second.

No server installation or world regeneration is required. To restore the ordinary list, disable the filter in NEI options or remove this addon.

## History

History lives under the client instance's `saves/NEIDiscovery/<sha256>.dat`. The key includes NEI's world/server identifier and the player UUID. Dimensions of the same world share discoveries. Different NEI world identities and players do not. Switching address/NEI identity can create a new history; this does not automatically merge servers with multiple addresses.

Only observations made after installing the addon are available; it cannot reconstruct earlier inventory history. Registry names, metadata and item NBT preserve material variants. Ordinary tool durability and GregTech tool wear/mode/charge are ignored while GregTech material/type distinctions are retained. Fluids in supported filled inventory containers also discover the corresponding NEI fluid identity; tank GUIs without inventory items are not inspected.

History is saved every five seconds and on disconnect, using a temporary file and atomic replacement where supported. An unreadable/unknown-format history is logged and left untouched. A crash can lose observations since the last save. Back up this folder alongside your client map data.

## Checks

`gradlew.bat test` exercises pickaxe ingredient alternatives, direct versus chained discovery, cycles, empty recipes, independent knowledge sets and immutable filter snapshots. An in-game check is still necessary for GTNH's full set of third-party recipe handlers.

See [BUILD-VALIDATION.md](BUILD-VALIDATION.md) for completed checks, remaining integration checks and the local build command.

Suggested in-game check: start in a disposable world with an empty inventory; search `pickaxe`; obtain known crafting ingredients; open a chest with another material; close it; check that discovery survives reconnecting, does not leak into another world, and that disabling the filter restores all search results. Check both chain settings, a worn GregTech tool and a filled fluid cell.

Reference checkouts are outside the addon in `../.research/3rd-party/`; they are not bundled in the jar. Build scaffolding follows GTNH's ExampleMod convention. Unsupported handlers are logged and skipped so they cannot break inventory access; their undiscovered outputs may remain hidden.
