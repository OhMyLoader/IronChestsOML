package com.gathertocraft.ironchest

import com.gathertocraft.ironchest.blocks.ChestTypes
import com.gathertocraft.ironchest.client.ClientBridges
import com.gathertocraft.ironchest.screenhandlers.ChestScreenHandler
import com.gathertocraft.ironchest.client.ChestEntityRenderer
import com.gathertocraft.ironchest.client.ChestScreen
import com.gathertocraft.ironchest.blocks.GenericChestBlock
import com.gathertocraft.ironchest.config.ChestRowsConfig
import com.gathertocraft.ironchest.registry.Registrars
import com.gathertocraft.ironchest.support.CopperGolemSupport
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.CreativeModeTab
import org.ohmyloader.api.Mod
import org.ohmyloader.api.ModContext
import org.ohmyloader.api.OMLModInitializer
import org.ohmyloader.api.OMLRegistryInitializer

/**
 * Iron Chests on OML — a GPL-3.0 port of [4nner/IronChests](https://github.com/4nner/IronChests)
 * (26.3 fabric/common line). The differences are exactly where the Fabric API surface meets OML's:
 *
 * - Fabric `ModInitializer` runs before registry freeze → OML's registry-declare phase
 *   ([OMLRegistryInitializer]) is the registration point; `ClientModInitializer` work rides
 *   [OMLModInitializer.onInitialize] guarded by the client side
 * - `FabricBlockEntityTypeBuilder` / `FabricCreativeModeTab` → the vanilla registry constructors
 * - the three Fabric mixins ([ChestBlockEntityViewerCountMixin]-alike, its accessor, and the
 *   copper-golem interaction) → OML `@Hooked` carriers, same anchors
 * - ironchest.json → the loader's TOML config; row counts per tier stay configurable
 *
 * Not ported (v2): the ironcore key/lock system and the hand truck.
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
            ClientBridges.registerMenuScreen(type.registeredMenuType) { menu, inventory, title ->
                ChestScreen(menu as ChestScreenHandler, inventory, title)
            }
            ClientBridges.registerBlockEntityRenderer(type.registeredBlockEntityType, ::ChestEntityRenderer)
        }
    }

    private fun isClient(): Boolean =
        runCatching { net.minecraft.client.Minecraft.getInstance() }.getOrNull() != null

    companion object {
        const val MOD_ID = "ironchest"

        /** The creative tab the chest items land in. */
        val TAB: ResourceKey<CreativeModeTab> = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(MOD_ID, "general"),
        )

        fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
    }
}
