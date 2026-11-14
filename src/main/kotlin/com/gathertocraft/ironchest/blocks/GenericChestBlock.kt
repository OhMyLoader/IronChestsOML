package com.gathertocraft.ironchest.blocks

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.RandomSource
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.animal.feline.Cat
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.ChestType
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult

/** A single-block chest of one [ChestTypes] tier. Chests of this mod never merge into doubles —
 * `getStateForPlacement` and `updateShape` both pin `ChestType.SINGLE`. */
class GenericChestBlock(properties: BlockBehaviour.Properties, private val type: ChestTypes) :
    ChestBlock(type::registeredBlockEntityType, getOpenSound(type), getCloseSound(type), properties) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        type.createBlockEntity(pos, state)

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        val direction = context.horizontalDirection.opposite
        val fluidState = context.level.getFluidState(context.clickedPos)
        return defaultBlockState()
            .setValue(FACING, direction)
            .setValue(TYPE, ChestType.SINGLE)
            .setValue(WATERLOGGED, fluidState.type == Fluids.WATER)
    }

    fun getType(): ChestTypes = type

    override fun getMenuProvider(state: BlockState, level: Level, pos: BlockPos): MenuProvider? {
        if (openUnderSolidBlocks && isBlockedByBlockOnly(level, pos)) {
            // Our chests are always single, so the block entity itself is the menu provider,
            // mirroring vanilla's acceptSingle path without the solid-block gate.
            val entity = level.getBlockEntity(pos)
            if (entity is MenuProvider) return entity
        }
        return super.getMenuProvider(state, level, pos)
    }

    override fun updateShape(
        state: BlockState,
        level: LevelReader,
        tickView: net.minecraft.world.level.ScheduledTickAccess,
        pos: BlockPos,
        direction: Direction,
        neighborPos: BlockPos,
        neighborState: BlockState,
        random: RandomSource,
    ): BlockState =
        super.updateShape(state, level, tickView, pos, direction, neighborPos, neighborState, random)
            .setValue(TYPE, ChestType.SINGLE)

    companion object {
        /** Applied from the config before blocks register; chests open under solid blocks when true. */
        @Volatile
        var openUnderSolidBlocks: Boolean = false

        /** True when a solid block sits above the chest but no cat is sitting on it. Mirrors
         * `ChestBlock` internals (the `isBlockedChestByBlock` half of `isChestBlockedAt` plus its
         * sitting-cat check), both private in vanilla. */
        private fun isBlockedByBlockOnly(level: Level, pos: BlockPos): Boolean {
            val above = pos.above()
            if (!level.getBlockState(above).isRedstoneConductor(level, above)) {
                return false
            }
            for (cat in level.getEntitiesOfClass(Cat::class.java, AABB(above))) {
                if (cat.isInSittingPose) return false
            }
            return true
        }

        private fun usesCopperChestSound(type: ChestTypes): Boolean =
            type == ChestTypes.COPPER || type == ChestTypes.IRON ||
                type == ChestTypes.GOLD || type == ChestTypes.NETHERITE

        private fun getOpenSound(type: ChestTypes): SoundEvent =
            if (usesCopperChestSound(type)) SoundEvents.COPPER_CHEST_OPEN else SoundEvents.CHEST_OPEN

        private fun getCloseSound(type: ChestTypes): SoundEvent =
            if (usesCopperChestSound(type)) SoundEvents.COPPER_CHEST_CLOSE else SoundEvents.CHEST_CLOSE
    }
}
