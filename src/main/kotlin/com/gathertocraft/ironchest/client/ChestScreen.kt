package com.gathertocraft.ironchest.client

import com.gathertocraft.ironchest.IronChestsOML
import com.gathertocraft.ironchest.core.ContainerGuiLayout
import com.gathertocraft.ironchest.screenhandlers.ChestScreenHandler
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.GuiPipeline
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory

/** The chest GUI. Three blitting modes: the vanilla full-panel band (narrow, up to 6 rows), a
 * tall variant that repeats vanilla's last row band for extra rows, and the mod's own wide strip
 * textures for the 12/14-column tiers. */
class ChestScreen(menu: ChestScreenHandler, inventory: Inventory, title: Component) :
    AbstractContainerScreen<ChestScreenHandler>(
        menu,
        inventory,
        title,
        ContainerGuiLayout.panelWidth(menu.chestColumns),
        ContainerGuiLayout.screenHeight(menu.chestRows, menu.playerRows),
    ) {

    private val containerRows: Int = menu.chestRows
    private val containerColumns: Int = menu.chestColumns
    private val playerRows: Int = menu.playerRows
    private val extendedInventory: Boolean = menu.isExtendedInventory
    private val layoutKind: ContainerGuiLayout.LayoutKind =
        ContainerGuiLayout.layoutKind(containerColumns, containerRows)

    // Segments of generic_54.png's 96px player panel (V=126): 14px top padding, 18px rows,
    // 4px gap, 18px hotbar, 6px bottom padding. Extra rows repeat the first inventory row.
    private val playerTopHeight = 14
    private val playerGapHeight = 4
    private val playerHotbarHeight = 18
    private val playerBottomHeight = 6

    init {
        inventoryLabelY = imageHeight - 94
        if (layoutKind == ContainerGuiLayout.LayoutKind.WIDE_STRIPS) {
            inventoryLabelX = ContainerGuiLayout.playerInventoryX(containerColumns)
        }
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor) {
        super.extractBackground(graphics)
        val xo = leftPos
        val yo = topPos

        when (layoutKind) {
            ContainerGuiLayout.LayoutKind.VANILLA_BLIT -> blitVanillaFullPanel(graphics, xo, yo)
            ContainerGuiLayout.LayoutKind.TALL_VANILLA_ROWS -> renderTallVanillaLayout(graphics, xo, yo)
            ContainerGuiLayout.LayoutKind.WIDE_STRIPS -> renderWideStripLayout(graphics, xo, yo)
        }
    }

    private fun blitVanillaFullPanel(graphics: GuiGraphicsExtractor, xo: Int, yo: Int) {
        val bodyHeight = bodyHeight()
        blitVanillaRegion(graphics, xo, yo, imageWidth, bodyHeight, 0.0f, 0.0f)
        blitPlayerFooter(graphics, xo, yo + bodyHeight, imageWidth)
    }

    private fun renderTallVanillaLayout(graphics: GuiGraphicsExtractor, xo: Int, yo: Int) {
        val panelWidth = imageWidth
        blitVanillaRegion(graphics, xo, yo, panelWidth, ContainerGuiLayout.TITLE_HEIGHT, 0.0f, 0.0f)

        for (row in 0 until containerRows) {
            val rowY = yo + ContainerGuiLayout.TITLE_HEIGHT + row * ContainerGuiLayout.ROW_HEIGHT
            val textureV =
                if (ContainerGuiLayout.usesVanillaRow(containerColumns, row)) {
                    ContainerGuiLayout.TITLE_HEIGHT + row * ContainerGuiLayout.ROW_HEIGHT
                } else {
                    ContainerGuiLayout.TITLE_HEIGHT + 5 * ContainerGuiLayout.ROW_HEIGHT
                }
            blitVanillaRegion(graphics, xo, rowY, panelWidth, ContainerGuiLayout.ROW_HEIGHT, 0.0f, textureV.toFloat())
        }

        blitPlayerFooter(graphics, playerFooterX(xo), yo + bodyHeight(), ContainerGuiLayout.VANILLA_PANEL_WIDTH)
    }

    private fun renderWideStripLayout(graphics: GuiGraphicsExtractor, xo: Int, yo: Int) {
        val panelWidth = imageWidth

        blitWideStrip(
            graphics,
            ContainerGuiLayout.wideTitleTexture(IronChestsOML.MOD_ID, containerColumns),
            xo,
            yo,
            panelWidth,
            ContainerGuiLayout.TITLE_HEIGHT,
        )

        for (row in 0 until containerRows) {
            val rowY = yo + ContainerGuiLayout.TITLE_HEIGHT + row * ContainerGuiLayout.ROW_HEIGHT
            blitWideStrip(
                graphics,
                ContainerGuiLayout.wideRowTexture(IronChestsOML.MOD_ID, containerColumns),
                xo,
                rowY,
                panelWidth,
                ContainerGuiLayout.ROW_HEIGHT,
            )
        }

        renderWidePlayerFooter(graphics, xo, yo + bodyHeight(), panelWidth)
    }

    private fun renderWidePlayerFooter(graphics: GuiGraphicsExtractor, xo: Int, footerY: Int, panelWidth: Int) {
        blitPlayerFooter(graphics, playerFooterX(xo), footerY, ContainerGuiLayout.VANILLA_PANEL_WIDTH)
        blitWideStrip(
            graphics,
            ContainerGuiLayout.widePlayerTexture(IronChestsOML.MOD_ID, containerColumns),
            xo,
            footerY,
            panelWidth,
            ContainerGuiLayout.WIDE_FOOTER_FRAME_HEIGHT,
        )
    }

    private fun bodyHeight(): Int = containerRows * ContainerGuiLayout.ROW_HEIGHT + ContainerGuiLayout.TITLE_HEIGHT

    private fun playerFooterX(xo: Int): Int =
        xo + ContainerGuiLayout.playerInventoryX(containerColumns) - ContainerGuiLayout.LEFT_INSET

    private fun blitWideStrip(
        graphics: GuiGraphicsExtractor,
        texture: Identifier,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        graphics.blit(GuiPipeline.TEXTURED, texture, x, y, 0.0f, 0.0f, width, height, width, height)
    }

    private fun blitVanillaRegion(
        graphics: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        u: Float,
        v: Float,
    ) {
        graphics.blit(
            GuiPipeline.TEXTURED,
            ContainerGuiLayout.VANILLA_BACKGROUND,
            x,
            y,
            u,
            v,
            width,
            height,
            ContainerGuiLayout.TEXTURE_SIZE,
            ContainerGuiLayout.TEXTURE_SIZE,
        )
    }

    private fun blitPlayerFooter(graphics: GuiGraphicsExtractor, x: Int, y: Int, width: Int) {
        if (!extendedInventory) {
            blitVanillaRegion(graphics, x, y, width, ContainerGuiLayout.playerPanelHeight(playerRows), 0.0f, ContainerGuiLayout.PLAYER_PANEL_V)
            return
        }
        val panelV = ContainerGuiLayout.PLAYER_PANEL_V
        val rowV = panelV + playerTopHeight
        val gapV = rowV + 3 * ContainerGuiLayout.ROW_HEIGHT
        blitVanillaRegion(graphics, x, y, width, playerTopHeight, 0.0f, panelV)
        val rowY = y + playerTopHeight
        for (row in 0 until playerRows) {
            blitVanillaRegion(
                graphics,
                x,
                rowY + row * ContainerGuiLayout.ROW_HEIGHT,
                width,
                ContainerGuiLayout.ROW_HEIGHT,
                0.0f,
                rowV,
            )
        }
        val afterRows = rowY + playerRows * ContainerGuiLayout.ROW_HEIGHT
        blitVanillaRegion(graphics, x, afterRows, width, playerGapHeight, 0.0f, gapV)
        blitVanillaRegion(graphics, x, afterRows + playerGapHeight, width, playerHotbarHeight, 0.0f, gapV + playerGapHeight)
        blitVanillaRegion(
            graphics,
            x,
            afterRows + playerGapHeight + playerHotbarHeight,
            width,
            playerBottomHeight,
            0.0f,
            gapV + playerGapHeight + playerHotbarHeight,
        )
    }
}
