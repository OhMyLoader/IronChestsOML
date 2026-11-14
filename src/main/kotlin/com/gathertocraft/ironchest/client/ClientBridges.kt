package com.gathertocraft.ironchest.client

import com.gathertocraft.ironchest.blocks.blockentities.GenericChestEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.MenuAccess
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.state.ChestRenderState
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.ItemLike

/**
 * 26.3 privatized the client registration entries a chest mod needs (`MenuScreens.register` and
 * its package-private `ScreenConstructor`; `BlockEntityRenderers.register`). Fabric mods reach
 * them through Fabric API's transitive access wideners; OML has no widener pass, so these bridges
 * go through reflection — contained here, run once per registration during mod init.
 */
object ClientBridges {

    /** Registers the screen factory for [menuType] (the vanilla `MenuScreens.register` path). */
    @Suppress("UNCHECKED_CAST")
    fun registerMenuScreen(menuType: MenuType<*>, create: (AbstractContainerMenu, Inventory, Component) -> Screen) {
        val constructorClass = MenuScreens::class.java.declaredClasses.first { it.simpleName == "ScreenConstructor" }
        val constructor = java.lang.reflect.Proxy.newProxyInstance(
            constructorClass.classLoader,
            arrayOf(constructorClass),
        ) { _, method, args ->
            when (method.name) {
                "create" ->
                    create(args[0] as AbstractContainerMenu, args[1] as Inventory, args[2] as Component)
                @Suppress("UNCHECKED_CAST")
                "fromPacket" -> {
                    // The interface's default: build the menu from the window id, then open.
                    val mc = args[2] as Minecraft
                    val player = mc.player ?: return@newProxyInstance null
                    val menu = (args[1] as MenuType<AbstractContainerMenu>).create(args[3] as Int, player.inventory)
                    val screen = create(menu, player.inventory, args[0] as Component)
                    player.containerMenu = (screen as MenuAccess<*>).menu
                    mc.gui.setScreen(screen)
                    null
                }
                else -> null
            }
        }
        val register = MenuScreens::class.java.declaredMethods.first { it.name == "register" }
        register.isAccessible = true
        register.invoke(null, menuType, constructor)
    }

    /** Registers the block entity renderer (the vanilla `BlockEntityRenderers.register` path). */
    fun registerBlockEntityRenderer(
        blockEntityType: BlockEntityType<*>,
        renderer: (BlockEntityRendererProvider.Context) -> ChestEntityRenderer,
    ) {
        val provider = object : BlockEntityRendererProvider<GenericChestEntity, ChestRenderState> {
            override fun create(context: BlockEntityRendererProvider.Context): ChestEntityRenderer =
                renderer(context)
        }
        val register = BlockEntityRenderers::class.java.declaredMethods.first { it.name == "register" }
        register.isAccessible = true
        register.invoke(null, blockEntityType, provider)
    }
}
