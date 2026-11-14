package com.gathertocraft.ironchest.hooks

import org.ohmyloader.api.hook.At
import org.ohmyloader.api.hook.Hooked
import org.ohmyloader.api.hook.HookValue
import org.ohmyloader.api.hook.Inject
import org.ohmyloader.api.hook.LocalCapture

/** TEMPORARY debug: traces the client-side lid trigger and tick stream. Remove after diagnosis. */
@Hooked(target = "net.minecraft.world.level.block.entity.ChestBlockEntity")
class LidDebugHook {

    @Inject(
        method = "triggerEvent",
        desc = "(II)Z",
        at = [At(At.HEAD)],
        require = 1,
        locals = LocalCapture.CAPTURE_FAILHARD,
    )
    fun logTrigger(event: Int, count: Int, ci: org.ohmyloader.api.hook.HookContext) {
        java.io.File("debug-lid.log")
            .appendText("[client @${System.currentTimeMillis()}] triggerEvent event=$event count=$count\n")
    }
}

@Hooked(target = "net.minecraft.world.level.block.entity.ChestLidController")
class LidTickDebugHook {

    @Inject(
        method = "tickLid",
        desc = "()V",
        at = [At(At.HEAD)],
        require = 1,
    )
    fun logTick(ci: org.ohmyloader.api.hook.HookContext) {
        java.io.File("debug-lid.log")
            .appendText("[client @${System.currentTimeMillis()}] tickLid\n")
    }
}

@Hooked(target = "net.minecraft.world.entity.player.Player")
class ContainerRangeDebugHook {

    @Inject(
        method = "getContainerInteractionRange",
        desc = "()D",
        at = [At(At.RETURN)],
        require = 1,
    )
    fun logRange(hv: HookValue<Double>) {
        java.io.File("debug-lid.log")
            .appendText("[probe @${System.currentTimeMillis()}] getContainerInteractionRange=${hv.getReturnValueD()}\n")
    }
}
