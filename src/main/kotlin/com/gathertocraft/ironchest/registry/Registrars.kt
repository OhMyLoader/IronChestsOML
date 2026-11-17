package com.gathertocraft.ironchest.registry

import com.gathertocraft.ironchest.IronChestsOML
import com.gathertocraft.ironchest.blocks.ChestTypes
import com.gathertocraft.ironchest.blocks.GenericChestBlock
import com.gathertocraft.ironchest.blocks.blockentities.GenericChestEntity
import com.gathertocraft.ironchest.core.MaterialTier
import com.gathertocraft.ironchest.items.ChestBlockItem
import com.gathertocraft.ironchest.items.UpgradeItem
import com.gathertocraft.ironchest.items.materialTier
import com.gathertocraft.ironchest.screenhandlers.ChestScreenHandler
import com.gathertocraft.ironchest.screenhandlers.SizedContainerMenu
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.level.block.entity.BlockEntityType
import org.ohmyloader.api.OMLCreativeTabs
import org.ohmyloader.api.OMLRegistration

/** Every registration, expressed through the OML registration helpers: they apply the resource
 * keys, replay the block-state bookkeeping (registry + caches) and absorb the 26.3 private-entry
 * workarounds, so this file names only tiers and factories. Each chest tier binds its registered
 * instances back onto the enum, so common code never dereferences a registry. */
object Registrars {

    /** Creative-tab order: the tier ladder. */
    private val TAB_ORDER = listOf(
        "dirt_chest", "copper_chest", "iron_chest", "gold_chest", "diamond_chest",
        "emerald_chest", "crystal_chest", "obsidian_chest", "netherite_chest", "christmas_chest",
    )

    /** Upgrade items, keyed by the tier they produce (their id mirrors it: `<tier>_upgrade`). */
    private val upgradeItems = LinkedHashMap<ChestTypes, Item>()

    fun registerAll() {
        UpgradeItem.bindVanillaChest()
        val chestItems = registerBlocksAndItems()
        registerCreativeTab(chestItems)
        registerBlockEntityTypes()
        registerMenuTypes()
        registerUpgradeItems()
    }

    private fun registerBlocksAndItems(): Map<String, Item> {
        val items = LinkedHashMap<String, Item>()
        for (type in ChestTypes.PLAYABLE) {
            val block = OMLRegistration.registerBlock(type.registryId!!, type.blockProperties()) { properties ->
                GenericChestBlock(properties, type)
            }
            type.bindBlock(block)
            // Christmas and Dirt stay outside the upgrade ladder - they are craft-only.
            type.materialTier?.let { tier -> UpgradeItem.bindTier(block, tier) }

            var properties = Item.Properties()
                .useBlockDescriptionPrefix()
                .component(DataComponents.LORE, ItemLore(listOf(), ChestBlockItem.tooltipLines(type)))
            if (type == ChestTypes.NETHERITE) {
                properties = properties.fireResistant()
            }
            val item = OMLRegistration.registerItem(type.registryId!!, properties) { props ->
                ChestBlockItem(type, props)
            }
            items[type.registryId!!] = item
        }
        return items
    }

    private fun registerCreativeTab(chestItems: Map<String, Item>) {
        OMLCreativeTabs.declare("general") {
            title = Component.translatable("itemGroup.ironchest.general")
            icon = { ItemStack(requireNotNull(chestItems["iron_chest"])) }
            // The chest tab lands right after vanilla's ingredient category.
            after("minecraft:ingredients")
            display { TAB_ORDER.mapNotNull(chestItems::get) }
        }
    }

    private fun registerBlockEntityTypes() {
        for (type in ChestTypes.PLAYABLE) {
            val blockEntityType: BlockEntityType<GenericChestEntity> = BlockEntityType(
                { pos, state -> type.createBlockEntity(pos, state) },
                setOf(type.registeredBlock),
            )
            OMLRegistration.registerBlockEntityType(type.registryId!!, blockEntityType)
            type.bindBlockEntityType(blockEntityType)
        }
    }

    private fun registerMenuTypes() {
        for (type in ChestTypes.PLAYABLE) {
            val menuType = OMLRegistration.registerMenuType(type.registryId!!) { syncId, inventory ->
                ChestScreenHandler(
                    type.registeredMenuType,
                    type,
                    syncId,
                    inventory,
                    SizedContainerMenu.createClientContainer(type),
                )
            }
            type.bindMenuType(menuType)
        }
    }

    private fun registerUpgradeItems() {
        // Christmas and Dirt are craft-only targets: no upgrade item produces them.
        val upgradeTargets = listOf(
            ChestTypes.COPPER, ChestTypes.IRON, ChestTypes.GOLD, ChestTypes.DIAMOND,
            ChestTypes.EMERALD, ChestTypes.CRYSTAL, ChestTypes.OBSIDIAN, ChestTypes.NETHERITE,
        )
        for (type in upgradeTargets) {
            val item = OMLRegistration.registerItem("${type.registryId!!.removeSuffix("_chest")}_upgrade", Item.Properties()) { props ->
                UpgradeItem(type, props)
            }
            upgradeItems[type] = item
        }
    }
}
