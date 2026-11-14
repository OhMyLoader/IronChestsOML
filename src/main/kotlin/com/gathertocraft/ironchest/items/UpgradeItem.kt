package com.gathertocraft.ironchest.items

import com.gathertocraft.ironchest.blocks.ChestTypes
import com.gathertocraft.ironchest.blocks.blockentities.GenericChestEntity
import com.gathertocraft.ironchest.core.MaterialTier
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.sounds.SoundSource
import net.minecraft.util.ProblemReporter
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.LevelEvent
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.ValueInput

/** Shift-right-click a chest to transform it in place: the whole block entity tag (inventory
 * included) is carried across the block swap, then the new chest clamps its capacity — growing
 * keeps everything, shrinking spills the overflow at the chest's feet. */
class UpgradeItem(private val target: ChestTypes, properties: Properties) : Item(properties) {

    override fun useOn(context: UseOnContext): InteractionResult {
        val level: Level = context.level
        val blockPos: BlockPos = context.clickedPos
        val state = level.getBlockState(blockPos)

        if (level.isClientSide) {
            return InteractionResult.SUCCESS
        }

        val player: Player = context.player ?: return InteractionResult.PASS
        if (!player.mayBuild()) return InteractionResult.PASS

        val source = tierOf(state.block)
        if (source == null || target.materialTier !in source.sources) return InteractionResult.PASS

        val blockEntity = level.getBlockEntity(blockPos)
        if (blockEntity !is ChestBlockEntity) return InteractionResult.PASS
        if (blockEntity is GenericChestEntity && !blockEntity.isAvailableForUpgrade(player)) {
            return InteractionResult.PASS
        }
        // An open chest must not be swapped under the viewer; the vanilla open counter guards it.
        if (ChestBlockEntity.getOpenCount(level, blockPos) > 0 || !blockEntity.stillValid(player)) {
            return InteractionResult.PASS
        }

        val oldState = state
        val oldChestTag: CompoundTag = blockEntity.saveWithoutMetadata(level.registryAccess())

        level.removeBlockEntity(blockPos)
        level.removeBlock(blockPos, false)
        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, blockPos, Block.getId(oldState))

        val newState = resultState(oldState)
        level.setBlock(blockPos, newState, 3)
        level.sendBlockUpdated(blockPos, newState, newState, 3)

        val newBlockEntity = level.getBlockEntity(blockPos)
        if (newBlockEntity != null) {
            val valueInput: ValueInput =
                TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), oldChestTag)
            newBlockEntity.loadWithComponents(valueInput)
            if (newBlockEntity is GenericChestEntity) {
                newBlockEntity.clampInventoryToCapacity()
            }
            newBlockEntity.setChanged()
        }

        level.playSound(
            null,
            blockPos,
            oldState.soundType.breakSound,
            SoundSource.BLOCKS,
            oldState.soundType.volume,
            oldState.soundType.pitch,
        )
        level.playSound(
            null,
            blockPos,
            newState.soundType.placeSound,
            SoundSource.BLOCKS,
            newState.soundType.volume,
            newState.soundType.pitch,
        )
        if (!player.abilities.instabuild) {
            context.itemInHand.shrink(1)
        }
        return InteractionResult.SUCCESS
    }

    /** Copies every shared blockstate property (FACING/WATERLOGGED, …) from the old chest. */
    private fun resultState(oldState: BlockState): BlockState {
        var newState = target.registeredBlock.defaultBlockState()
        for (oldProperty in oldState.properties) {
            newState = copyIfPresent(oldState, oldProperty, newState)
        }
        return newState
    }

    private fun <T : Comparable<T>> copyIfPresent(
        oldState: BlockState,
        oldProperty: Property<T>,
        newState: BlockState,
    ): BlockState {
        val raw = newState.block.stateDefinition.getProperty(oldProperty.name) ?: return newState
        if (raw.valueClass != oldProperty.valueClass) return newState
        @Suppress("UNCHECKED_CAST")
        val newProperty = raw as Property<T>
        val value = oldState.getValue(oldProperty)
        if (newProperty.possibleValues.contains(value)) {
            return newState.setValue(newProperty, value)
        }
        return newState
    }

    companion object {
        /** Block → material tier, bound during the registry-declare phase (chest blocks only). */
        private val blockTiers = HashMap<Block, MaterialTier>()

        fun bindTier(block: Block, tier: MaterialTier) {
            blockTiers[block] = tier
        }

        fun bindVanillaChest() {
            bindTier(net.minecraft.world.level.block.Blocks.CHEST, MaterialTier.WOOD)
        }

        fun tierOf(block: Block): MaterialTier? = blockTiers[block]
    }
}

/** Merge point between the chest world and the upgrade ladder. */
val ChestTypes.materialTier: MaterialTier?
    get() = when (this) {
        ChestTypes.WOOD -> MaterialTier.WOOD
        ChestTypes.COPPER -> MaterialTier.COPPER
        ChestTypes.IRON -> MaterialTier.IRON
        ChestTypes.GOLD -> MaterialTier.GOLD
        ChestTypes.DIAMOND -> MaterialTier.DIAMOND
        ChestTypes.EMERALD -> MaterialTier.EMERALD
        ChestTypes.CRYSTAL -> MaterialTier.CRYSTAL
        ChestTypes.OBSIDIAN -> MaterialTier.OBSIDIAN
        ChestTypes.NETHERITE -> MaterialTier.NETHERITE
        else -> null
    }
