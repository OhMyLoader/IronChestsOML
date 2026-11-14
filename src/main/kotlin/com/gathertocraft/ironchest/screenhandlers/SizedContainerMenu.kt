package com.gathertocraft.ironchest.screenhandlers

import com.gathertocraft.ironchest.core.ContainerGuiLayout
import com.gathertocraft.ironchest.core.TierSpec
import net.minecraft.core.BlockPos
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.ChestBlockEntity

/** A container menu whose chest area is laid out from the tier's geometry: rows × columns of
 * constrained slots, then the player inventory centered underneath (wide tiers widen the panel).
 * Container slots route placement through `Container.canPlaceItem` so click and shift-click
 * filtering shares one rule with hoppers. */
open class SizedContainerMenu(
    menuType: MenuType<*>,
    private val tier: TierSpec,
    syncId: Int,
    playerInventory: Inventory,
    private val container: Container,
) : AbstractContainerMenu(menuType, syncId) {

    val playerRows: Int = ContainerGuiLayout.playerRows(playerInventory)
    val chestBlockPos: BlockPos? =
        (container as? ChestBlockEntity)?.blockPos

    init {
        require(container.containerSize == tier.size) {
            "container size ${container.containerSize} does not match tier size ${tier.size}"
        }
        container.startOpen(playerInventory.player)

        val columns = tier.rowLength
        val rows = tier.rowCount
        var slotIndex = 0
        val containerSlotX = ContainerGuiLayout.containerSlotStartX(columns)

        for (row in 0 until rows) {
            val slotY = ContainerGuiLayout.SLOT_SIZE + row * ContainerGuiLayout.SLOT_SIZE
            for (column in 0 until columns) {
                addSlot(
                    ConstrainedSlot(
                        container,
                        slotIndex++,
                        containerSlotX + column * ContainerGuiLayout.SLOT_SIZE,
                        slotY,
                    ),
                )
            }
        }

        val playerInventoryX = ContainerGuiLayout.playerInventoryX(columns)
        val playerInventoryY = ContainerGuiLayout.playerInventoryY(rows)
        val hotbarOffset = ContainerGuiLayout.hotbarOffset(playerRows)
        for (row in 0 until playerRows) {
            for (column in 0 until 9) {
                addSlot(
                    Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        playerInventoryX + column * ContainerGuiLayout.SLOT_SIZE,
                        playerInventoryY + row * ContainerGuiLayout.SLOT_SIZE,
                    ),
                )
            }
        }
        for (column in 0 until 9) {
            addSlot(
                Slot(
                    playerInventory,
                    column,
                    playerInventoryX + column * ContainerGuiLayout.SLOT_SIZE,
                    playerInventoryY + hotbarOffset,
                ),
            )
        }
    }

    val isExtendedInventory: Boolean get() = playerRows > ContainerGuiLayout.VANILLA_PLAYER_ROWS

    val chestRows: Int get() = tier.rowCount

    val chestColumns: Int get() = tier.rowLength

    val blockInventory: Container get() = container

    override fun quickMoveStack(player: Player, slotIndex: Int): ItemStack {
        var movedStack = ItemStack.EMPTY
        val slot = slots[slotIndex]
        if (slot.hasItem()) {
            val stackInSlot = slot.item
            movedStack = stackInSlot.copy()
            val containerSlots = tier.size
            val moved =
                if (slotIndex < containerSlots) {
                    moveItemStackTo(stackInSlot, containerSlots, slots.size, true)
                } else {
                    moveItemStackTo(stackInSlot, 0, containerSlots, false)
                }
            if (!moved) return ItemStack.EMPTY

            if (stackInSlot.isEmpty) {
                slot.setByPlayer(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }
        }
        return movedStack
    }

    override fun stillValid(player: Player): Boolean = container.stillValid(player)

    override fun removed(player: Player) {
        super.removed(player)
        container.stopOpen(player)
    }

    companion object {
        /** The client-side menu factory gets a throwaway container: networked menus carry slot
         * data by index, and the real container lives server-side. */
        fun createClientContainer(tier: TierSpec): Container = SimpleContainer(tier.size)
    }

    /** Chest slot filtering via `Container.canPlaceItem`: filters what items can be placed in a
     * slot by clicking or shift-clicking. */
    private class ConstrainedSlot(container: Container, private val slotIndex: Int, x: Int, y: Int) :
        Slot(container, slotIndex, x, y) {

        override fun mayPlace(stack: ItemStack): Boolean =
            container.canPlaceItem(slotIndex, stack)
    }
}
