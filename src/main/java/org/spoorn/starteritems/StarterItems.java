package org.spoorn.starteritems;

import lombok.extern.log4j.Log4j2;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.spoorn.starteritems.config.ModConfig;

import java.util.List;
import java.util.Set;

@Log4j2
public class StarterItems implements ModInitializer {
    
    public static final String MODID = "starteritems";
    private static final String JOINED_ID = MODID + "_joined";
    
    @Override
    public void onInitialize() {
        log.info("Hello from StarterItems!");

        // Config
        ModConfig.init();
        
        // On player log in, give starter items
        ServerEntityEvents.ENTITY_LOAD.register(((entity, world) -> {
            if (entity instanceof ServerPlayerEntity player) {
                Set<String> scoreboardTags = player.getScoreboardTags();
                if (!scoreboardTags.contains(JOINED_ID)) {
                    List<String> starterItems = ModConfig.get().starterItems;
                    
                    if (!starterItems.isEmpty()) {
                        log.info("Player={} is joining the world for the first time.  Giving them starter items...", player.getGameProfile().getId());

                        for (String itemId : starterItems) {
                            Identifier identifier = new Identifier(itemId);

                            if (Registry.ITEM.containsId(identifier)) {
                                Item item = Registry.ITEM.get(identifier);
                                boolean insertedItem = player.getInventory().insertStack(new ItemStack(item));
                                if (!insertedItem) {
                                    log.error("[StarterItems] Player={} inventory is full!  Cannot add anymore starter items at={} and after", player.getGameProfile().getId(), itemId);
                                    break;
                                }
                            } else {
                                log.error("[StarterItems] attempted to give player={} item={}, but was not found in the registry!", player.getGameProfile().getId(), itemId);
                            }
                        }
                    }
                    
                    if (!player.addScoreboardTag(JOINED_ID)) {
                        throw new RuntimeException("Player's scoreboard tags are full!  Gonna need to find a different way to track player joined worlds");
                    }
                }
            }
        }));
    }
}
