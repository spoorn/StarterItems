package org.spoorn.starteritems.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spoorn.starteritems.StarterItems;
import org.spoorn.starteritems.config.ModConfig;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    
    private final Logger log = LogManager.getLogger("StarterItemsServerPlayerEntity");
    
    private final Object lock = new Object();
    
    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void tryClearInventory(CallbackInfo ci) {
        if (ModConfig.get().clearInventoryBeforeGivingItems) {
            PlayerEntity player = (PlayerEntity) (Object) this;
            String playerId = player.getGameProfile().getId().toString();
            synchronized (lock) {
                if (StarterItems.NEW_PLAYERS.contains(playerId)) {
                    log.info("[StarterItems] Clearing player's inventory of non-starter items in case other mods added at this point");
                    PlayerInventory playerInventory = player.getInventory();
                    for (int i = 0; i < playerInventory.size(); i++) {
                        if (!containsItemInStarterItems(playerInventory.getStack(i))) {
                            playerInventory.removeStack(i);
                        }
                    }
                    StarterItems.NEW_PLAYERS.remove(playerId);
                }
            }
        }
    }
    
    private boolean containsItemInStarterItems(ItemStack stack) {
        for (ItemStack starter : StarterItems.starterItemStacks) {
            if (starter.getItem().equals(stack.getItem())) {
                return true;
            }
        }
        
        return false;
    }
}
