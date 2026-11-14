package com.gathertocraft.ironchest.config

import org.ohmyloader.api.config.OMLConfig

/** Row-count options for `<config>/ironchest.toml`. Columns are fixed per tier; only rows are
 * configurable (1–12). Defaults match the baked row counts; invalid values keep the defaults. */
object ChestRowsConfig {
    const val MAX_ROWS = 12
    const val MIN_ROWS = 1
    const val OPEN_UNDER_SOLID_BLOCKS = "openUnderSolidBlocks"
    const val COPPER_GOLEM_INTERACTION = "copperGolemInteraction"

    fun init(config: OMLConfig) {
        for (type in com.gathertocraft.ironchest.blocks.ChestTypes.PLAYABLE) {
            config.define(
                type.configKey(),
                type.defaultRowCount(),
                "${type.configKey().replaceFirstChar { it.uppercase() }} chest rows ($MIN_ROWS-$MAX_ROWS)",
            )
        }
        config.define(OPEN_UNDER_SOLID_BLOCKS, false, "Open chests placed under solid blocks (true/false)")
        config.define(COPPER_GOLEM_INTERACTION, true, "Allow copper golems to store items in these chests (true/false)")
    }
}
