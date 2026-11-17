# IronChestsOML

Iron Chests on OML (OhMyLoader) — a GPL-3.0 port of
[4nner/IronChests](https://github.com/4nner/IronChests) (the `26.3` fabric/common line) to OML's
unobfuscated-jar mod surface. Every chest tier ships: dirt, copper, iron, gold, diamond, emerald,
crystal, obsidian, netherite and christmas — with the shift-right-click upgrade items, the chest
lid animation, and copper-golem deposit support.

## What the port changed

- Fabric `ModInitializer` (runs before registry freeze) → OML's registry-declare phase
  (`OMLRegistryInitializer.onDeclareRegistries`); `ClientModInitializer` work runs in
  `onInitialize` guarded to the client side.
- `FabricBlockEntityTypeBuilder` / `FabricCreativeModeTab` / `CreativeModeTabEvents` → the vanilla
  registry constructors (`BlockEntityType`, `CreativeModeTab.builder`).
- 26.3 privatized `MenuType`'s constructor, `MenuScreens.register`, `BlockEntityRenderers.register`
  and `CreativeModeTab.Output`; Fabric reaches them through transitive access wideners. OML now
  carries these bridges itself (`org.ohmyloader.api.game.OMLRegistration`), alongside the block-state
  bookkeeping (state registry + caches) and the creative-tab slot picking — the registration helpers
  this mod was the first real user of.
- The viewer-count mixin is restructured, not dropped: vanilla's openers counter recognizes
  only vanilla `ChestMenu`s and its scheduled recheck recounts live menus with that test, so any
  menu we open is counted back to zero a tick later (no lid animation, upgrade gate dead). The
  block entity swaps the private `openersCounter` field — reached through an `@FieldAccess`
  accessor on the nameable `ChestBlockEntity`, no anonymous-class self needed — for a subclass
  whose `isOwnContainer` knows our menus; an own open-viewer counter feeds the upgrade gate.
  The copper-golem interaction stays a hook: `@WrapMethod` around
  `TransportItemsBetweenContainers.isWantedBlock` (a RETURN HookValue degrades to notify —
  short-circuiting only lands at method-entry anchors).
- ironchest.json (jankson) → the loader's TOML config (`config/ironchest.toml`): per-tier row
  counts (1–12), open-under-solid-blocks, copper-golem interaction.
- Not ported (v2): the ironcore key/lock system, the hand truck, and the pre-3.0 legacy upgrade
  item migration (a new world never needs it).

## Build and run

```bash
gradlew build      # jar under build/libs
gradlew runClient  # launch the game with the mod installed
```

The loader, plugin and game jar resolve through the OML Gradle plugin; run
`gradlew publishToMavenLocal` in the sibling `OhMyLoader/` checkout first when its API changed.

## License

Iron Chests is licensed GPL-3.0 (code and assets alike); this port is a derivative work and
carries the same license — see [LICENSE](LICENSE). The reference sources live in a sibling
`ironchests-reference/` checkout for provenance; nothing here builds against it.
