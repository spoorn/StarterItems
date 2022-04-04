package org.spoorn.starteritems.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spoorn.starteritems.StarterItems;

@Mixin(ServerPlayNetworkHandler.class)
public class PlayerManagerMixin {

    @Shadow public ServerPlayerEntity player;
    
    @Inject(method = "onDisconnected", at = @At(value = "TAIL"))
    private void markPlayerDisconnect(Text reason, CallbackInfo ci) {
        StarterItems.LOADED_PLAYERS.remove(this.player.getGameProfile().getId().toString());
    }
}
