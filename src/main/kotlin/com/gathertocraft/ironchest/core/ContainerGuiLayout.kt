package com.gathertocraft.ironchest.core

import net.minecraft.resources.Identifier

/** Slot/panel geometry shared by the menu (slot coordinates) and the screen (blitted panels).
 * Wide tiers (12/14 columns) are drawn from the mod's own strip textures; narrow ones reuse the
 * vanilla `generic_54.png` bands, repeating the last row band for tall layouts. */
object ContainerGuiLayout {
    const val VANILLA_PANEL_WIDTH = 176
    const val SLOT_SIZE = 18
    const val LEFT_INSET = 8
    const val RIGHT_INSET = 6
    const val PLAYER_INVENTORY_GAP = 13
    const val HOTBAR_OFFSET = 58
    const val NARROW_CENTER_OFFSET = 9

    const val TITLE_HEIGHT = 17
    const val ROW_HEIGHT = 18
    const val TEXTURE_SIZE = 256
    const val PLAYER_PANEL_HEIGHT = 96
    const val WIDE_FOOTER_FRAME_HEIGHT = 7
    const val PLAYER_PANEL_V = 126.0f
    const val VANILLA_ROWS = 6
    const val VANILLA_COLUMNS = 9
    const val VANILLA_PLAYER_ROWS = 3

    val VANILLA_BACKGROUND: Identifier = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png")
    private const val WIDE_TEXTURE_BASE = "textures/gui/container/wide/"

    enum class LayoutKind { VANILLA_BLIT, TALL_VANILLA_ROWS, WIDE_STRIPS }

    fun layoutKind(columns: Int, rows: Int): LayoutKind = when {
        columns > VANILLA_COLUMNS -> LayoutKind.WIDE_STRIPS
        rows > VANILLA_ROWS -> LayoutKind.TALL_VANILLA_ROWS
        else -> LayoutKind.VANILLA_BLIT
    }

    fun panelWidth(columns: Int): Int =
        if (columns <= VANILLA_COLUMNS) {
            maxOf(VANILLA_PANEL_WIDTH, LEFT_INSET + columns * SLOT_SIZE + RIGHT_INSET)
        } else {
            LEFT_INSET + (columns - 1) * SLOT_SIZE + (SLOT_SIZE - 1) + RIGHT_INSET
        }

    fun screenHeight(chestRows: Int, playerRows: Int = VANILLA_PLAYER_ROWS): Int =
        114 + chestRows * ROW_HEIGHT + maxOf(0, playerRows - VANILLA_PLAYER_ROWS) * ROW_HEIGHT

    fun hotbarOffset(playerRows: Int): Int =
        if (playerRows <= VANILLA_PLAYER_ROWS) HOTBAR_OFFSET
        else HOTBAR_OFFSET + (playerRows - VANILLA_PLAYER_ROWS) * ROW_HEIGHT

    fun playerPanelHeight(playerRows: Int): Int =
        PLAYER_PANEL_HEIGHT + maxOf(0, playerRows - VANILLA_PLAYER_ROWS) * ROW_HEIGHT

    fun containerSlotStartX(columns: Int): Int =
        LEFT_INSET + (maxOf(VANILLA_COLUMNS, columns) - columns) * NARROW_CENTER_OFFSET

    fun playerInventoryX(columns: Int): Int {
        val chestWidth = columns * SLOT_SIZE
        val playerWidth = 9 * SLOT_SIZE
        return containerSlotStartX(columns) + (chestWidth - playerWidth) / 2
    }

    fun playerInventoryY(rows: Int): Int = SLOT_SIZE + rows * SLOT_SIZE + PLAYER_INVENTORY_GAP

    fun usesVanillaRow(columns: Int, row: Int): Boolean = columns <= VANILLA_COLUMNS && row < VANILLA_ROWS

    fun wideTitleTexture(namespace: String, columns: Int): Identifier = wideTexture(namespace, "chest_title", columns)

    fun wideRowTexture(namespace: String, columns: Int): Identifier = wideTexture(namespace, "chest_row", columns)

    fun widePlayerTexture(namespace: String, columns: Int): Identifier = wideTexture(namespace, "chest_player", columns)

    private fun wideTexture(namespace: String, baseName: String, columns: Int): Identifier {
        require(columns > VANILLA_COLUMNS) { "Wide texture requires columns > 9, got $columns" }
        return Identifier.fromNamespaceAndPath(namespace, "$WIDE_TEXTURE_BASE${baseName}_$columns.png")
    }

    /** Player inventory rows: vanilla is 3; a future enlarged-inventory mod grows it dynamically. */
    fun playerRows(inventory: net.minecraft.world.entity.player.Inventory): Int {
        val nonEquipmentSize = inventory.nonEquipmentItems.size
        if (nonEquipmentSize <= net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE) {
            return VANILLA_PLAYER_ROWS
        }
        val selection = net.minecraft.world.entity.player.Inventory.SELECTION_SIZE
        return maxOf(VANILLA_PLAYER_ROWS, (nonEquipmentSize - selection) / selection)
    }
}
