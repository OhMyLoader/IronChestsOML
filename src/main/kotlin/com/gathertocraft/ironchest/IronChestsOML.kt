package com.gathertocraft.ironchest

import com.gathertocraft.ironchest.blocks.ChestTypes
import com.gathertocraft.ironchest.screenhandlers.ChestScreenHandler
import com.gathertocraft.ironchest.client.ChestEntityRenderer
import com.gathertocraft.ironchest.client.ChestScreen
import com.gathertocraft.ironchest.blocks.GenericChestBlock
import com.gathertocraft.ironchest.config.ChestRowsConfig
import com.gathertocraft.ironchest.registry.Registrars
import com.gathertocraft.ironchest.support.CopperGolemSupport
import net.minecraft.resources.Identifier
import org.ohmyloader.api.OMLRegistration
import org.ohmyloader.api.core.Mod
import org.ohmyloader.api.core.ModContext
import org.ohmyloader.api.core.OMLModInitializer
import org.ohmyloader.api.core.OMLRegistryInitializer

/**
 * Iron Chests on OML — a GPL-3.0 port of [4nner/IronChests](https://github.com/4nner/IronChests)
 * (26.3 fabric/common line). Registration goes through OML's registry-declare phase, block-entity
 * and creative-tab construction through the vanilla registry constructors, the three Fabric mixins
 * through OML `@Hooked` carriers with the same anchors, and ironchest.json through the loader's
 * TOML config; row counts per tier stay configurable. Not ported (v2): the ironcore key/lock
 * system and the hand truck.
 */
@Mod(id = "ironchest", name = "Iron Chests OML", version = "0.1.0")
class IronChestsOML : OMLRegistryInitializer, OMLModInitializer {

    override fun onDeclareRegistries(context: ModContext) {
        ChestRowsConfig.init(context.config)
        GenericChestBlock.openUnderSolidBlocks =
            context.config.getBoolean(ChestRowsConfig.OPEN_UNDER_SOLID_BLOCKS)
        CopperGolemSupport.setEnabled(context.config.getBoolean(ChestRowsConfig.COPPER_GOLEM_INTERACTION))
        ChestTypes.setConfiguredRows(
            ChestTypes.PLAYABLE.associate { it.configKey() to context.config.getInt(it.configKey()) },
        )
        Registrars.registerAll()
    }

    override fun onInitialize(context: ModContext) {
        // Client-only registration: screen factories and block entity renderers are vanilla client
        // static maps; the dedicated server must not touch them.
        if (!isClient()) return
        for (type in ChestTypes.PLAYABLE) {
            OMLRegistration.registerMenuScreen(type.registeredMenuType) { menu, inventory, title ->
                ChestScreen(menu as ChestScreenHandler, inventory, title)
            }
            @Suppress("UNCHECKED_CAST")
            val entityType = type.registeredBlockEntityType
                as net.minecraft.world.level.block.entity.BlockEntityType<com.gathertocraft.ironchest.blocks.blockentities.GenericChestEntity>
            OMLRegistration.registerBlockEntityRenderer(entityType, ::ChestEntityRenderer)
        }
    }

    private fun isClient(): Boolean =
        runCatching { net.minecraft.client.Minecraft.getInstance() }.getOrNull() != null

    companion object {
        const val MOD_ID = "ironchest"

        fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
    }
}
