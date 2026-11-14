package com.gathertocraft.ironchest.blocks

import com.gathertocraft.ironchest.IronChestsOML
import com.gathertocraft.ironchest.blocks.blockentities.CrystalChestEntity
import com.gathertocraft.ironchest.blocks.blockentities.GenericChestEntity
import com.gathertocraft.ironchest.core.TierSpec
import com.gathertocraft.ironchest.screenhandlers.ChestScreenHandler
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState

/** One chest tier: capacity geometry, registry id and the chest-atlas texture its renderer draws.
 * Registrations bind eagerly in the registry-declare phase, in order: block -> block entity type ->
 * menu type - so every [block] / [blockEntityType] / [menuType] dereference is safe afterwards.
 *
 * The geometry members are properties (not `size()`/`rowCount()` methods) because the tier's live
 * size depends on the configured row count: `rowCount` is config-overridable and `size` derives
 * from it. */
enum class ChestTypes(
    /** Baked capacity: the default row count times the baked column count. */
    val bakedSize: Int,
    val bakedRowLength: Int,
    val registryId: String?,
    val texture: Identifier,
) : TierSpec {
    NETHERITE(126, 14, "netherite_chest", IronChestsOML.id("entity/chest/netherite_chest")),
    OBSIDIAN(108, 12, "obsidian_chest", IronChestsOML.id("entity/chest/obsidian_chest")),
    CRYSTAL(108, 12, "crystal_chest", IronChestsOML.id("entity/chest/crystal_chest")),
    DIAMOND(108, 12, "diamond_chest", IronChestsOML.id("entity/chest/diamond_chest")),
    EMERALD(108, 12, "emerald_chest", IronChestsOML.id("entity/chest/emerald_chest")),
    GOLD(81, 9, "gold_chest", IronChestsOML.id("entity/chest/gold_chest")),
    IRON(54, 9, "iron_chest", IronChestsOML.id("entity/chest/iron_chest")),
    COPPER(45, 9, "copper_chest", IronChestsOML.id("entity/chest/copper_chest")),
    CHRISTMAS(27, 9, "christmas_chest", Identifier.withDefaultNamespace("entity/chest/christmas")),
    DIRT(126, 14, "dirt_chest", IronChestsOML.id("entity/chest/dirt_chest")),
    WOOD(27, 9, null, Identifier.withDefaultNamespace("entity/chest/normal")),
    ;

    /** Every tier the mod registers and ships; in creative-tab order. */
    companion object {
        val PLAYABLE: List<ChestTypes> =
            listOf(DIRT, COPPER, IRON, GOLD, DIAMOND, EMERALD, CRYSTAL, OBSIDIAN, NETHERITE, CHRISTMAS)

        private val configuredRows = mutableMapOf<ChestTypes, Int>()

        /** Applies row counts from the config; missing keys leave defaults in place. */
        fun setConfiguredRows(rowsByKey: Map<String, Int>) {
            configuredRows.clear()
            for (type in PLAYABLE) {
                rowsByKey[type.configKey()]?.let { configuredRows[type] = it }
            }
        }
    }

    override val rowLength: Int get() = bakedRowLength

    override val rowCount: Int get() = configuredRows[this] ?: (bakedSize / bakedRowLength)

    override val size: Int get() = rowCount * rowLength

    /** Rows baked into the enum; the config defaults to these. */
    fun defaultRowCount(): Int = bakedSize / bakedRowLength

    /** Lowercase tier name used as the key in the config file. */
    fun configKey(): String = name.lowercase()

    @Volatile
    var block: Block? = null
        private set

    @Volatile
    var blockEntityType: BlockEntityType<out GenericChestEntity>? = null
        private set

    @Volatile
    var menuType: MenuType<ChestScreenHandler>? = null
        private set

    fun bindBlock(block: Block) {
        this.block = block
    }

    fun bindBlockEntityType(type: BlockEntityType<out GenericChestEntity>) {
        this.blockEntityType = type
    }

    fun bindMenuType(menuType: MenuType<ChestScreenHandler>) {
        this.menuType = menuType
    }

    // Java-flavored `getX` accessors would clash with the bind properties JVM signatures, so the
    // registered instances surface as properties instead.
    val registeredBlock: Block get() = block ?: Blocks.CHEST

    val registeredBlockEntityType: BlockEntityType<out GenericChestEntity>
        get() = requireNotNull(blockEntityType) { "block entity type for $name read before registration" }

    val registeredMenuType: MenuType<ChestScreenHandler>
        get() = requireNotNull(menuType) { "menu type for $name read before registration" }

    fun createBlockEntity(pos: BlockPos, state: BlockState): GenericChestEntity =
        if (this == CRYSTAL) CrystalChestEntity(pos, state)
        else GenericChestEntity(this, pos, state)

    fun blockProperties(): BlockBehaviour.Properties = when (this) {
        COPPER, GOLD ->
            BlockBehaviour.Properties.of().strength(3.0f, 6.0f).sound(SoundType.COPPER)
                .requiresCorrectToolForDrops()
        IRON ->
            BlockBehaviour.Properties.of().strength(5.0f, 6.0f).sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
        DIAMOND, EMERALD ->
            BlockBehaviour.Properties.of().strength(5.0f, 6.0f).sound(SoundType.STONE)
                .requiresCorrectToolForDrops()
        CRYSTAL ->
            BlockBehaviour.Properties.of().strength(0.3f, 0.3f).sound(SoundType.AMETHYST)
                .noOcclusion().requiresCorrectToolForDrops()
        OBSIDIAN ->
            BlockBehaviour.Properties.of().strength(50.0f, 1200.0f).sound(SoundType.STONE)
                .requiresCorrectToolForDrops()
        NETHERITE ->
            BlockBehaviour.Properties.of().strength(50.0f, 1200.0f).sound(SoundType.NETHERITE_BLOCK)
                .requiresCorrectToolForDrops()
        WOOD, CHRISTMAS ->
            BlockBehaviour.Properties.of().strength(2.5f, 2.5f).sound(SoundType.WOOD)
        DIRT ->
            BlockBehaviour.Properties.of().strength(0.5f, 0.5f).sound(SoundType.GRAVEL)
    }
}
