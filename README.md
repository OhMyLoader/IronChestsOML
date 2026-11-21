# IronChestsOML

The Iron Chests mod ported to OhMyLoader — a GPL-3.0 port of
[4nner/IronChests](https://github.com/4nner/IronChests). All chest tiers and the upgrade items are
present, with the lid animation and copper-golem deposit support.

```bash
gradlew build      # jar under build/libs
gradlew runClient  # launch the game with the mod installed
```

The loader, plugin, and game jar resolve through the OML Gradle plugin; publish the loader to the
local Maven repository first when its API changed (see [CONTRIBUTING.md](CONTRIBUTING.md)).

## License

Iron Chests is licensed GPL-3.0 (code and assets alike); this port is a derivative work and carries
the same license — see [LICENSE](LICENSE).
