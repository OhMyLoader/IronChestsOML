package com.gathertocraft.ironchest.blocks.blockentities

import com.gathertocraft.ironchest.screenhandlers.ChestScreenHandler
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.entity.ContainerOpenersCounter
import net.minecraft.world.level.block.state.BlockState

/**
 * The openers counter our chest entities swap in: vanilla's anonymous counter recognizes only
 * vanilla `ChestMenu`s as "own container", and the scheduled recheck recounts live menus with it —
 * a menu we open is counted back to zero one tick later, so the lid never animates and the count
 * the upgrade gate consults stays zero. This subclass answers the same question for
 * [ChestScreenHandler] menus backed by this chest.
 *
 * The base class carries the incremental counting and recheck machinery; only the recognition and
 * the side effects (sounds, block event) are overridden here, mirroring vanilla's anonymous
 * subclass.
 */
class GenericChestOpenersCounter(private val chest: GenericChestEntity) : ContainerOpenersCounter() {

    override fun isOwnContainer(player: Player): Boolean {
        val menu = player.containerMenu as? ChestScreenHandler
        if (menu == null) {
            java.io.File("debug-lid.log")
                .appendText("[server] isOwnContainer menu=NULL other=${player.containerMenu?.javaClass?.name}\n")
            return false
        }
        val idMatch = menu.blockInventory === chest
        val posMatch = menu.chestBlockPos == chest.blockPos
        java.io.File("debug-lid.log")
            .appendText("[server] isOwnContainer idMatch=$idMatch posMatch=$posMatch chest=${chest.hashCode()} menuContainer=${menu.blockInventory.hashCode()} pos=${chest.blockPos}\n")
        return idMatch || posMatch
    }

    override fun onOpen(level: Level, pos: BlockPos, state: BlockState) {
        playChestSound(level, pos, state, openSound(state))
    }

    override fun onClose(level: Level, pos: BlockPos, state: BlockState) {
        playChestSound(level, pos, state, closeSound(state))
    }

    /** Mirrors vanilla's `ChestBlockEntity.signalOpenCount`: the lid animates client-side through
     * the block event that carries the new open count. */
    /** TEMPORARY debug: measures the recount list the recheck bases its decision on. */
    override fun recheckOpeners(level: Level, pos: BlockPos, state: BlockState) {
        val size = getEntitiesWithContainerOpen(level, pos).size
        java.io.File("debug-lid.log")
            .appendText("[server] recheckOpeners listSize=$size pos=${chest.blockPos}\n")
        super.recheckOpeners(level, pos, state)
    }

    override fun openerCountChanged(level: Level, pos: BlockPos, state: BlockState, oldCount: Int, newCount: Int) {
        java.io.File("debug-lid.log").appendText("[server] openerCountChanged old=$oldCount new=$newCount isClient=${level.isClientSide}\n")
        level.blockEvent(pos, state.block, 1, newCount)
    }

    /** Vanilla's `getOpenChestSound` is public on the block and returns the constructor sound. */
    private fun openSound(state: BlockState): SoundEvent =
        (state.block as? ChestBlock)?.openChestSound ?: SoundEvents.CHEST_OPEN

    private fun closeSound(state: BlockState): SoundEvent =
        (state.block as? ChestBlock)?.closeChestSound ?: SoundEvents.CHEST_CLOSE

    /** Vanilla's private `ChestBlockEntity.playSound`, replicated for single chests (our chests
     * never merge, so the sound sits at the block center). */
    private fun playChestSound(level: Level, pos: BlockPos, state: BlockState, sound: SoundEvent) {
        level.playSound(
            null,
            pos.x + 0.5,
            pos.y + 0.5,
            pos.z + 0.5,
            sound,
            SoundSource.BLOCKS,
            0.5f,
            1.0f,
        )
    }
}
