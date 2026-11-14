package com.gathertocraft.ironchest.blocks.blockentities

import com.gathertocraft.ironchest.blocks.ChestTypes
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState

/** The see-through chest: same capacity as diamond, but the full inventory is synced to clients
 * every change so the renderer can draw the top twelve stacks floating inside it. */
class CrystalChestEntity(pos: BlockPos, state: BlockState) : GenericChestEntity(ChestTypes.CRYSTAL, pos, state) {

    /** The first twelve non-empty stacks (copies), in slot order. */
    fun getTopStacks(): NonNullList<ItemStack> {
        val topStacks = NonNullList.withSize(12, ItemStack.EMPTY)
        var itemCount = 0
        for (stack in items) {
            if (stack.isEmpty) continue
            topStacks[itemCount++] = stack.copy()
            if (itemCount >= 12) break
        }
        return topStacks
    }

    override fun setChanged() {
        super.setChanged()
        if (level is ServerLevel) {
            (level as ServerLevel).chunkSource.blockChanged(worldPosition)
        }
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? =
        ClientboundBlockEntityDataPacket.create(this)

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag =
        saveWithoutMetadata(registries)
}
