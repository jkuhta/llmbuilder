# LLM Builder

A Fabric mod for Minecraft Java Edition 1.21.11 that generates realistic, detailed buildings from a
prompt typed in chat.

Most AI build mods ask a language model for block coordinates, which gives boxy, low-detail results.
LLM Builder doesn't do that. Claude writes a high-level **architectural spec** (style, footprint,
floors, roof, materials, facade rhythm), and deterministic Java code builds it from a library of
procedural components: recessed windows with sills and lintels, stair-and-slab roofs, cornices,
buttresses, chimneys and more. The model never outputs a coordinate.

![screenshot](docs/screenshot.png) <!-- placeholder: in-game screenshot of the canal house -->

> **Status:** v0.1.0 contains the spec format and the procedural generator, driven by
> `/build fromjson`. The Claude planner, preview and undo land in v0.2.0.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.19.5+ for Minecraft 1.21.11 and the
   [Fabric API](https://modrinth.com/mod/fabric-api).
2. Download `llmbuilder-<version>.jar` from the
   [releases page](https://github.com/jkuhta/llmbuilder/releases) and put it in `mods/`.
3. Start the game or server once to create `config/llmbuilder.json`.

The mod runs on the server. Single player works out of the box. On a dedicated server, only the
server needs the mod for `/build fromjson`.

## Commands

All commands require operator permission (level 2) by default.

| Command | What it does |
|---|---|
| `/build fromjson <file>` | Builds a spec at your crosshair, front facing you. `<file>` is a name from `config/llmbuilder/specs/` (without `.json`) or a bundled example. |
| `/build fromjson <file> at <x y z> <facing>` | Same, at an explicit position (front-centre at ground level) for consoles and command blocks. |

Bundled examples: `canal_house`, `medieval_tower`, `modern_villa`, `farmhouse`, `gothic_chapel`.

## Configuration

`config/llmbuilder.json`:

| Key | Default | Meaning |
|---|---|---|
| `maxBuildingSize` | `48` | Largest footprint width or depth, in blocks |
| `maxBuildingHeight` | `64` | Largest total height including the roof |
| `blocksPerTick` | `1500` | Blocks placed per server tick, shared by all running builds |
| `permissionLevel` | `2` | Permission level needed for `/build` (0 = everyone) |

## Writing specs

A spec is JSON with no coordinates. 1 block = 1 m. Minimal example:

```json
{
  "name": "Test house",
  "footprint": { "width": 9, "depth": 11 },
  "floors": { "count": 2, "heights": [4] },
  "roof": { "type": "gable" },
  "palette": { "wall": "bricks", "roof": [{ "material": "dark_oak", "weight": 3 }, { "material": "spruce", "weight": 1 }] },
  "facades": { "front": { "bays": 3, "doors": [{ "bay": 1 }] } }
}
```

| Section | Fields |
|---|---|
| `footprint` | `shape` (`rect`, `l`, `u`, `composite`), `width`, `depth`, `wings[]` (`side`, `align`, `width`, `depth`, `floors`) |
| `floors` | `count`, `heights[]` (3–5 typical), `groundFloor` (`standard`, `shopfront`, `raised_basement`), `plinth` |
| `roof` | `type` (`gable`, `hip`, `mansard`, `flat`, `stepped_gable`), `pitch` (`low`, `medium`, `steep`), `overhang`, `ridge` (`auto`, `width`, `depth`), `parapet`, `cornice` |
| `walls` | `corners` (`none`, `pillars`, `quoins`), `bands` |
| `palette` | `wall`, `trim`, `accent`, `roof`, `foundation`, `windowFrame`, `door`, `floor`: each a material family or a weighted list; plus `glass` |
| `facades.<side>` | `bays`, `windows` (`type`, `width`, `height`, `shutters`, `hood`), `floorWindows`, `doors[]`, `piers` (`none`, `pilasters`, `buttresses`), `blank` |
| `chimneys[]` | `side`, `position` (0–1 along the side) |
| `detail` | `level` (`low`, `medium`, `high`), `lighting` |

Window types: `sash`, `casement`, `arched`, `lancet`, `round`, `shopfront`, `ribbon`, `slit`, `none`.
Materials are families such as `stone_bricks`, `dark_oak` or `deepslate_tiles`; the generator
derives stairs, slabs, walls and so on from them. Errors name the exact JSON path, e.g.
`facades.front.bays: 6 bays do not fit a 9 m facade (max 4)`. The bundled
[examples](src/main/resources/llmbuilder/examples) show complete specs.

## Architecture

```
spec/       BuildingSpec records, JSON parser with path-qualified errors, semantic validator
generator/  Pure Java, no Minecraft classes: spec -> BlockBuffer
  core/       Block values, BlockBuffer, directions
  material/   Material families and the weighted, patchy palette sampler
  layout/     Masses (main block + wings), facade frames, symmetric bay solver
  structure/  Wall shell, plinth, bands, cornice, pilasters, buttresses, floors
  opening/    Windows (8 types) and doors
  roof/       Distance-field roof planner, roof renderer, flat roofs, stepped gables
  feature/    Chimneys
  resolve/    Stair corner shapes (ported from vanilla), pane/fence/wall connections
  check/      Realism rules as executable checks
placement/  Block mapping, transforms, tick-batched placement with undo snapshots
command/    /build commands
config/     config/llmbuilder.json
```

Pipeline: **spec** → validate → **generator** (components in a fixed order) → **realism checks**
→ **placement** spread over ticks.

The generator enforces these realism rules, which are checked after every build and asserted in
tests:

- no floating blocks;
- every window has a sill;
- roof stairs climb towards the ridge;
- every facade has at least two depth layers;
- every directional block has a complete state;
- large walls show palette variation.

## Development

Requires JDK 21. The Gradle daemon is pinned to Java 21 in `gradle/gradle-daemon-jvm.properties`.

```bash
./gradlew build          # compile and run all tests
./gradlew runClient      # launch Minecraft with the mod
./gradlew runServer      # launch a dedicated server
```

The generator is tested without starting Minecraft. Tests also check every generated block state
against vanilla's blockstate files, and map every block against the real registry. Running the
tests writes isometric and elevation previews of the examples to `build/renders/`.

## Roadmap

- [x] v0.1.0: spec, procedural generator, `/build fromjson`
- [ ] v0.2.0: Claude planner, ghost preview, confirm/cancel/rotate/undo
- [ ] v0.3.0: refine, detailer pass, more components (dormers, balconies, interiors, landscaping, domes)

## License

MIT
