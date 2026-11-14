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
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.block.entity.BlockEntityType

/** Every vanilla-registry registration, in the dependency order the freeze point expects:
 * blocks -> items -> creative tab -> block entity types -> menus -> upgrade items. Each chest tier
 * binds its registered instances back onto the enum, so common code never dereferences a registry. */
object Registrars {

    /** Creative-tab order: the tier ladder. */
    private val TAB_ORDER = listOf(
        "dirt_chest", "copper_chest", "iron_chest", "gold_chest", "diamond_chest",
        "emerald_chest", "crystal_chest", "obsidian_chest", "netherite_chest", "christmas_chest",
    )

    /** Upgrade items, keyed by the tier they produce (their id mirrors it: `<tier>_upgrade`). */
    private val upgradeItems = LinkedHashMap<ChestTypes, Item>()

    fun registerAll() {
        registerBlocks()
        UpgradeItem.bindVanillaChest()
        val chestItems = registerChestItems()
        registerCreativeTab(chestItems)
        registerBlockEntityTypes()
        registerMenuTypes()
        registerUpgradeItems()
    }

    private fun registerBlocks() {
        for (type in ChestTypes.PLAYABLE) {
            val id = IronChestsOML.id(type.registryId!!)
            val block = GenericChestBlock(
                type.blockProperties().setId(ResourceKey.create(Registries.BLOCK, id)),
                type,
            )
            Registry.register(BuiltInRegistries.BLOCK, id, block)
            type.bindBlock(block)
            // Christmas and Dirt stay outside the upgrade ladder - they are craft-only.
            type.materialTier?.let { tier -> UpgradeItem.bindTier(block, tier) }
        }
    }

    private fun registerChestItems(): Map<String, Item> {
        val items = LinkedHashMap<String, Item>()
        for (type in ChestTypes.PLAYABLE) {
            val id = IronChestsOML.id(type.registryId!!)
            var properties = Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .useBlockDescriptionPrefix()
                .component(DataComponents.LORE, ItemLore(listOf(), ChestBlockItem.tooltipLines(type)))
            if (type == ChestTypes.NETHERITE) {
                properties = properties.fireResistant()
            }
            val item = ChestBlockItem(type, properties)
            Registry.register(BuiltInRegistries.ITEM, id, item)
            items[requireNotNull(type.registryId)] = item
        }
        return items
    }

    private fun registerCreativeTab(chestItems: Map<String, Item>) {
        val tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.ironchest.general"))
            .icon { ItemStack(requireNotNull(chestItems["iron_chest"])) }
            .displayItems(displayItemsGenerator { TAB_ORDER.mapNotNull(chestItems::get) })
            .build()
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, IronChestsOML.TAB.identifier(), tab)
    }

    private fun registerBlockEntityTypes() {
        for (type in ChestTypes.PLAYABLE) {
            val id = IronChestsOML.id(type.registryId!!)
            val blockEntityType: BlockEntityType<GenericChestEntity> = BlockEntityType(
                { pos, state -> type.createBlockEntity(pos, state) },
                setOf(type.registeredBlock),
            )
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, blockEntityType)
            type.bindBlockEntityType(blockEntityType)
        }
    }

    private fun registerMenuTypes() {
        for (type in ChestTypes.PLAYABLE) {
            val id = IronChestsOML.id(type.registryId!!)
            val menuType = newMenuType { syncId, inventory ->
                ChestScreenHandler(
                    type.menuType ?: error("menu type for ${type.name} not bound yet"),
                    type,
                    syncId,
                    inventory,
                    SizedContainerMenu.createClientContainer(type),
                )
            }
            Registry.register(BuiltInRegistries.MENU, id, menuType)
            type.bindMenuType(menuType)
        }
    }

    /**
     * 26.3 privatized the `MenuType` constructor (vanilla's own entries build through a private
     * static `register`), and OML has no access-widener pass - Fabric mods reach it through Fabric
     * API's transitive access wideners instead. The reflection is contained here: one lookup, run
     * once per tier during the registry-declare phase, before any menu is ever created.
     */
    private fun newMenuType(factory: (Int, net.minecraft.world.entity.player.Inventory) -> ChestScreenHandler): MenuType<ChestScreenHandler> {
        // The supplier interface itself is package-private in 26.3, so a dynamic proxy implements it.
        val supplierClass = MenuType::class.java.declaredClasses.first { it.simpleName == "MenuSupplier" }
        val supplier = java.lang.reflect.Proxy.newProxyInstance(
            supplierClass.classLoader,
            arrayOf(supplierClass),
        ) { _, method, args ->
            when (method.name) {
                "create" -> factory(args[0] as Int, args[1] as net.minecraft.world.entity.player.Inventory)
                "toString" -> "IronChestsOML menu supplier"
                else -> null
            }
        }
        val constructor = MenuType::class.java.getDeclaredConstructor(supplierClass, FeatureFlagSet::class.java)
        constructor.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return constructor.newInstance(supplier, FeatureFlags.VANILLA_SET) as MenuType<ChestScreenHandler>
    }

    /**
     * `CreativeModeTab.Output` is protected in 26.3, so the display generator cannot be written as
     * a Kotlin lambda against it - a proxy implements the public `DisplayItemsGenerator` and calls
     * `Output.accept` reflectively (one lookup per item; the tab builds its display a handful of
     * times per screen rebuild).
     */
    private fun displayItemsGenerator(items: () -> List<Item>): CreativeModeTab.DisplayItemsGenerator {
        val iface = CreativeModeTab.DisplayItemsGenerator::class.java
        val itemLike = ItemLike::class.java
        @Suppress("UNCHECKED_CAST")
        return java.lang.reflect.Proxy.newProxyInstance(iface.classLoader, arrayOf(iface)) { _, method, args ->
            if (method.name == "accept") {
                val output = args[1]
                val accept = output.javaClass.methods.first { it.name == "accept" && it.parameterCount == 1 && it.parameterTypes[0] == itemLike }
                accept.isAccessible = true
                for (item in items()) accept.invoke(output, item)
            }
            null
        } as CreativeModeTab.DisplayItemsGenerator
    }

    private fun registerUpgradeItems() {
        // Christmas and Dirt are craft-only targets: no upgrade item produces them.
        val upgradeTargets = listOf(
            ChestTypes.COPPER, ChestTypes.IRON, ChestTypes.GOLD, ChestTypes.DIAMOND,
            ChestTypes.EMERALD, ChestTypes.CRYSTAL, ChestTypes.OBSIDIAN, ChestTypes.NETHERITE,
        )
        for (type in upgradeTargets) {
            val id = IronChestsOML.id("${type.registryId!!.removeSuffix("_chest")}_upgrade")
            val item = UpgradeItem(
                type,
                Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)),
            )
            Registry.register(BuiltInRegistries.ITEM, id, item)
            upgradeItems[type] = item
        }
    }
}
