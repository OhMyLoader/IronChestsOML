package com.gathertocraft.ironchest.blocks.blockentities

import com.gathertocraft.ironchest.blocks.ChestTypes
import com.gathertocraft.ironchest.config.ChestRowsConfig
import com.gathertocraft.ironchest.core.InventorySanitizer
import com.gathertocraft.ironchest.core.TierSpec
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.network.chat.Component
import net.minecraft.world.Containers
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.tags.ItemTags

/** One tier's chest inventory, sized by [TierSpec] (config-overridable rows). Extends the vanilla
 * `ChestBlockEntity` so lid animation, open counting, loot and save format all come from vanilla.
 * The `sizingForLoad` trick lets configured rows shrink without silently dropping saved stacks:
 * vanilla sizes the item list from `getContainerSize()`, so during load we report the largest
 * possible inventory and clamp (spilling overflow into the world) afterwards. */
open class GenericChestEntity(
    private val tier: ChestTypes,
    pos: BlockPos,
    state: BlockState,
) : ChestBlockEntity(tier.registeredBlockEntityType, pos, state) {

    private val pendingOverflow = ArrayList<ItemStack>()
    private var sizingForLoad = false

    init {
        setItems(NonNullList.withSize(tier.size, ItemStack.EMPTY))
        // Swap the vanilla counter for one that recognizes our menus: the vanilla anonymous
        // counter's isOwnContainer only matches ChestMenu, and its scheduled recheck recounts
        // live menus with it - a chest we open is counted back to zero one tick later, killing
        // the lid animation. The field lives on the (nameable) ChestBlockEntity, so accessor
        // synthesis reaches it without touching the anonymous counter class.
        (this as com.gathertocraft.ironchest.hooks.ChestBlockEntityAccess).setOpenersCounter(
            GenericChestOpenersCounter(this),
        )
    }

    /** How many menus are currently open on this chest, our own count: the vanilla static
     * `getOpenCount` only recognizes vanilla `ChestMenu`s, so the upgrade item asks here instead.
     * `startOpen` / `stopOpen` drive the vanilla lid counter too, so one callback pair feeds both. */
    private var openViewers = 0

    override fun startOpen(user: net.minecraft.world.entity.ContainerUser) {
        java.io.File("debug-lid.log").appendText("[server] startOpen openViewers=${openViewers + 1}\n")
        super.startOpen(user)
        openViewers++
    }

    override fun stopOpen(user: net.minecraft.world.entity.ContainerUser) {
        java.io.File("debug-lid.log").appendText("[server] stopOpen openViewers=$openViewers\n")
        super.stopOpen(user)
        if (openViewers > 0) openViewers--
    }

    /** True when nobody is using this chest and it may be transformed in place by an upgrade. */
    fun isAvailableForUpgrade(player: Player): Boolean =
        openViewers == 0 && stillValid(player)

    override fun getContainerSize(): Int =
        if (sizingForLoad) maxCapacity() else tier.size

    override fun setItem(slot: Int, stack: ItemStack) {
        super.setItem(slot, InventorySanitizer.sanitize(stack))
    }

    /** The Dirt Chest 9000 only stores blocks under the `minecraft:dirt` tag. */
    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean {
        if (tier == ChestTypes.DIRT && !stack.`is`(ItemTags.DIRT)) {
            return false
        }
        return super.canPlaceItem(slot, stack)
    }

    /** Largest inventory this tier can ever hold; anything beyond is overflow by definition. */
    private fun maxCapacity(): Int = ChestRowsConfig.MAX_ROWS * tier.rowLength

    override fun createMenu(syncId: Int, inventory: Inventory): AbstractContainerMenu =
        com.gathertocraft.ironchest.screenhandlers.ChestScreenHandler(
            tier.registeredMenuType, tier, syncId, inventory, this,
        )

    override fun getDefaultName(): Component =
        Component.translatable(blockState.block.descriptionId)

    fun clampInventoryToCapacity() {
        val capacity = containerSize
        val items = items
        if (items.size == capacity) {
            InventorySanitizer.sanitize(items)
            setChanged()
            return
        }

        val clamped = NonNullList.withSize(capacity, ItemStack.EMPTY)
        for (slot in 0 until minOf(items.size, capacity)) {
            clamped[slot] = items[slot]
        }

        val level: Level? = this.level
        val pos = blockPos
        for (slot in capacity until items.size) {
            val overflow = items[slot]
            if (!overflow.isEmpty) {
                placeOverflow(clamped, overflow, capacity, level, pos)
            }
        }

        setItems(clamped)
        InventorySanitizer.sanitize(clamped)
        setChanged()
    }

    private fun placeOverflow(
        inventory: NonNullList<ItemStack>,
        stack: ItemStack,
        capacity: Int,
        level: Level?,
        pos: BlockPos,
    ) {
        for (slot in 0 until capacity) {
            if (inventory[slot].isEmpty) {
                inventory[slot] = stack
                return
            }
        }
        if (level != null) {
            Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
        } else {
            pendingOverflow.add(stack)
        }
    }

    override fun setLevel(level: Level) {
        super.setLevel(level)
        if (level.isClientSide || pendingOverflow.isEmpty()) return
        val pos = blockPos
        for (stack in pendingOverflow) {
            Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
        }
        pendingOverflow.clear()
    }

    override fun loadAdditional(input: ValueInput) {
        sizingForLoad = true
        try {
            super.loadAdditional(input)
        } finally {
            sizingForLoad = false
        }
        clampInventoryToCapacity()
    }

    override fun saveAdditional(output: ValueOutput) {
        InventorySanitizer.sanitize(items)
        super.saveAdditional(output)
    }
}
