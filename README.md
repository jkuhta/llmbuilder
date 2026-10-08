# LLM Builder

A Fabric mod for Minecraft Java Edition 1.21.11 that generates realistic, detailed buildings from a
prompt typed in chat.

Most AI build mods ask a language model for block coordinates, which gives boxy, low-detail results.
LLM Builder doesn't do that. Claude writes a high-level **architectural spec** (style, footprint,
floors, roof, materials, facade rhythm), and deterministic Java code builds it from a library of
procedural components: recessed windows with sills and lintels, stair-and-slab roofs, cornices,
chimneys and more.

![screenshot](docs/screenshot.png) <!-- placeholder -->

> Status: under development. See the [roadmap](#roadmap).

## Roadmap

- [ ] v0.1.0: spec, procedural generator, `/build fromjson`
- [ ] v0.2.0: Claude planner, ghost preview, confirm/cancel/rotate/undo
- [ ] v0.3.0: refine, detailer pass, more components

## License

MIT
