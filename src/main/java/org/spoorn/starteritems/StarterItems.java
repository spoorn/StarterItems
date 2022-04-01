package org.spoorn.starteritems;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.spoorn.starteritems.config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class StarterItems implements ModInitializer {
    
    public static final String MODID = "starteritems";
    private static final String JOINED_ID = MODID + "_joined";
    private static final Pattern ITEM_REGEX = Pattern.compile("^((?<count>\\d+)\\s+)?\\s*(?<item>[\\w:]+)\\s*(\\s+(?<nbt>\\{.*\\}))?$");
    
    @Override
    public void onInitialize() {
        log.info("Hello from StarterItems!");

        // Config
        ModConfig.init();
        
        // Validate and cache starter items
        List<String> starterItems = ModConfig.get().starterItems;
        final List<ItemStack> starterItemStacks = new ArrayList<>();
        for (String starterItem : starterItems) {
            Matcher matcher = ITEM_REGEX.matcher(starterItem.trim());
            
            if (!matcher.matches()) {
                throw new RuntimeException("Starter Item {" + starterItem + "} is not in a valid format.  " +
                        "Please check the config file at config/starteritems.json5 for acceptable formats.");
            }
            
            String countStr = matcher.group("count");
            String itemStr = matcher.group("item").trim();
            String nbtStr = matcher.group("nbt");

            Identifier identifier = new Identifier(itemStr);
            if (!Registry.ITEM.containsId(identifier)) {
                throw new RuntimeException("Starter Item {" + itemStr + "} does not exist in the Item registry.  " +
                        "Are you sure the identifier is correct?");
            }
            
            Item item = Registry.ITEM.get(identifier);
            int count = countStr == null ? 1 : Integer.parseInt(countStr.trim());
            
            ItemStack itemStack = new ItemStack(item, count);
            if (nbtStr != null) {
                try {
                    NbtCompound nbtCompound = StringNbtReader.parse(nbtStr);
                    itemStack.setNbt(nbtCompound);
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException("[StarterItems] Could not read Nbt compound from \"" + starterItem + "\"");
                }
            }
            starterItemStacks.add(itemStack);
        }
        
        // On player log in, give starter items
        if (!starterItemStacks.isEmpty()) {
            log.info("StarterItems={} loaded", starterItemStacks);
            
            ServerEntityEvents.Load loadLambda = ((entity, world) -> {
                if (entity instanceof ServerPlayerEntity player) {
                    Set<String> scoreboardTags = player.getScoreboardTags();
                    if (!scoreboardTags.contains(JOINED_ID)) {
                        log.info("Player={} is joining the world for the first time.  Giving them starter items...", player);

                        if (!player.addScoreboardTag(JOINED_ID)) {
                            throw new RuntimeException("Player " + player + " scoreboard tags are full!  Might need to find a different way to track player joined worlds");
                        }

                        if (ModConfig.get().clearInventoryBeforeGivingItems) {
                            player.getInventory().clear();
                        }
                        
                        // Copy as inserting stacks mutates it
                        List<ItemStack> starterItemStacksCopy = copyItemStacks(starterItemStacks);
                        for (ItemStack itemStack : starterItemStacksCopy) {
                            boolean insertedItem = player.getInventory().insertStack(itemStack);
                            if (!insertedItem) {
                                log.error("[StarterItems] Player={} inventory is full!  Cannot add anymore starter items at={} and after", player, itemStack);
                                break;
                            }
                        }
                    }
                }
            });
            
            if (ModConfig.get().clearInventoryBeforeGivingItems) {
                Identifier postDefaultPhase = new Identifier(MODID, "postdefault");
                ServerEntityEvents.ENTITY_LOAD.addPhaseOrdering(Event.DEFAULT_PHASE, postDefaultPhase);
                log.info("[StarterItems] Clearing player inventory before giving starter items.");
                ServerEntityEvents.ENTITY_LOAD.register(postDefaultPhase, loadLambda);
            } else {
                ServerEntityEvents.ENTITY_LOAD.register(loadLambda);
            }
        }
    }
    
    private List<ItemStack> copyItemStacks(List<ItemStack> stacks) {
        List<ItemStack> res = new ArrayList<>();
        for (ItemStack stack : stacks) {
            res.add(stack.copy());
        }
        return res;
    }
}
