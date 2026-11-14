package com.gathertocraft.ironchest.client

import com.gathertocraft.ironchest.blocks.GenericChestBlock
import com.gathertocraft.ironchest.blocks.blockentities.CrystalChestEntity
import com.gathertocraft.ironchest.blocks.blockentities.GenericChestEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.model.`object`.chest.ChestModel
import net.minecraft.client.renderer.MultiblockChestResources
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.ChestRenderer
import net.minecraft.client.renderer.blockentity.state.ChestRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.resources.model.sprite.SpriteGetter
import net.minecraft.client.resources.model.sprite.SpriteId
import net.minecraft.core.NonNullList
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

/** The per-tier chest renderer: same vanilla chest model, but the sprite comes from the tier's own
 * texture in the chest atlas. Falls through to the vanilla path for anything that is not one of
 * our blocks (so a WOOD-bound tier renders vanilla). Crystal additionally draws its top twelve
 * stacks floating inside the chest. Every block entity the tiers register is a
 * [GenericChestEntity], so the renderer is concrete rather than intersection-generic — Kotlin
 * cannot declare `T : BlockEntity & LidBlockEntity` at a class site. */
class ChestEntityRenderer(context: BlockEntityRendererProvider.Context) :
    ChestRenderer<GenericChestEntity>(context) {

    private val sprites: SpriteGetter = context.sprites()
    private val models: MultiblockChestResources<ChestModel> =
        LAYERS.map { layer -> ChestModel(context.bakeLayer(layer)) }

    override fun createRenderState(): ChestRenderState = IronChestRenderState()

    override fun extractRenderState(
        blockEntity: GenericChestEntity,
        state: ChestRenderState,
        partialTicks: Float,
        cameraPosition: Vec3,
        breakProgress: ModelFeatureRenderer.CrumblingOverlay?,
    ) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress)
        val renderState = state as IronChestRenderState
        val block = blockEntity.blockState.block
        if (block is GenericChestBlock) {
            renderState.modSprite = SpriteId(Sheets.CHEST_SHEET, block.getType().texture)
        }
        if (blockEntity is CrystalChestEntity) {
            renderState.topStacks = blockEntity.getTopStacks()
        }
    }

    override fun submit(
        state: ChestRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState,
    ) {
        val renderState = state as IronChestRenderState
        val sprite = renderState.modSprite
        if (sprite == null) {
            super.submit(state, poseStack, submitNodeCollector, camera)
            return
        }

        poseStack.pushPose()
        poseStack.mulPose(modelTransformation(state.facing))
        var open = state.open
        open = 1.0f - open
        open = 1.0f - open * open * open
        val model = models.select(state.type)
        submitNodeCollector.submitModel(
            model,
            open,
            poseStack,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            -1,
            sprite,
            sprites,
            0,
        )
        val breakProgress = state.breakProgress
        if (breakProgress != null) {
            submitNodeCollector
                .order(1)
                .submitCrumblingOverlay(
                    model,
                    open,
                    poseStack,
                    sprite.renderType(model.renderType()),
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    -1,
                    breakProgress,
                )
        }
        val topStacks = renderState.topStacks
        if (topStacks != null) {
            renderItems(
                poseStack,
                topStacks,
                submitNodeCollector,
                state.lightCoords,
                state.blockPos.asLong().toInt(),
            )
        }

        poseStack.popPose()
    }

    private fun renderItems(
        poseStack: PoseStack,
        items: NonNullList<ItemStack>,
        collector: SubmitNodeCollector,
        light: Int,
        seed: Int,
    ) {
        val mc = Minecraft.getInstance()
        val level: Level = mc.level ?: return
        val rotation = level.gameTime + mc.deltaTracker.getGameTimeDeltaPartialTick(false)
        var i = 0
        for (j in 0 until 3) renderItem(0.55, 0.3 + j * 0.5, 0.7, items[i++], poseStack, collector, light, seed, mc, level, rotation)
        for (j in 0 until 3) renderItem(1.4, 0.3 + j * 0.5, 0.7, items[i++], poseStack, collector, light, seed, mc, level, rotation)
        for (j in 0 until 3) renderItem(0.55, 0.3 + j * 0.5, 1.4, items[i++], poseStack, collector, light, seed, mc, level, rotation)
        for (j in 0 until 3) renderItem(1.4, 0.3 + j * 0.5, 1.4, items[i++], poseStack, collector, light, seed, mc, level, rotation)
    }

    private fun renderItem(
        x: Double,
        y: Double,
        z: Double,
        item: ItemStack,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        light: Int,
        seed: Int,
        mc: Minecraft,
        level: Level,
        rotation: Float,
    ) {
        if (item.isEmpty) return
        poseStack.pushPose()
        poseStack.scale(0.5f, 0.5f, 0.5f)
        poseStack.translate(x.toFloat(), y.toFloat(), z.toFloat())
        poseStack.rotateDegrees(Axis.YP, rotation)
        val itemRenderState = ItemStackRenderState()
        mc.itemModelResolver.updateForTopItem(itemRenderState, item, ItemDisplayContext.GROUND, level, null, seed)
        itemRenderState.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0)
        poseStack.popPose()
    }

    private class IronChestRenderState : ChestRenderState() {
        var modSprite: SpriteId? = null
        var topStacks: NonNullList<ItemStack>? = null
    }
}
