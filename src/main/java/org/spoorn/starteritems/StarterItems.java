package org.spoorn.starteritems;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import org.spoorn.starteritems.config.Message;
import org.spoorn.starteritems.config.ModConfig;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class StarterItems implements ModInitializer {
    
    public static final String MODID = "starteritems";
    public static final String JOINED_ID = MODID + "_joined";
    private static final Pattern ITEM_REGEX = Pattern.compile("^((?<count>\\d+)\\s+)?\\s*(?<item>[\\w:]+)\\s*(\\s+(?<nbt>\\{.*\\}))?$");
    
    // Keep track of players that have been loaded.  This is to avoid sending the welcome message when player reloads
    // as in changing dimensions.
    public static final Set<String> LOADED_PLAYERS = new HashSet<>();
    
    @Override
    public void onInitialize() {
        log.info("Hello from StarterItems!");

        // Config
        ModConfig.init();
        
        // Validate and cache starter items
        List<String> starterItems = ModConfig.get().starterItems;
        List<ItemInfo> itemInfos = new ArrayList<>();
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

            NbtCompound nbtCompound = null;
            if (nbtStr != null) {
                try {
                    nbtCompound = StringNbtReader.parse(nbtStr);
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException("[StarterItems] Could not read Nbt compound from \"" + starterItem + "\"");
                }
            }
            
            itemInfos.add(new ItemInfo(countStr, identifier, nbtCompound));
            log.info("StarterItems={} loaded", itemInfos);
        }
        
        // Parse First Join Messages
        List<Text> firstJoinMessages = parseMessages(ModConfig.get().firstJoinMessages);
        
        // Parse Welcome Messages
        List<Text> welcomeMessages = parseMessages(ModConfig.get().welcomeMessages);
        
        // On player log in, give starter items
        if (!itemInfos.isEmpty() || !firstJoinMessages.isEmpty()) {
            List<ItemStack> starterItemStacks = new ArrayList<>();
            AtomicBoolean parsedItems = new AtomicBoolean(false);

            ServerEntityEvents.Load loadLambda = ((entity, world) -> {
                if (entity instanceof ServerPlayerEntity player) {
                    Set<String> scoreboardTags = player.getScoreboardTags();
                    if (scoreboardTags.contains(JOINED_ID)) {
                        // Only send the welcome message if this is the first time player is loading in
                        if (!welcomeMessages.isEmpty() && !LOADED_PLAYERS.contains(player.getGameProfile().getId().toString())) {
                            // Welcome messages
                            log.info("Sending welcome messages to {}", player);
                            sendMessagesToPlayer(player, welcomeMessages);
                        }
                    } else {
                        log.info("Player={} is joining the world for the first time.  Processing Starter stuff", player);
                        
                        // Parse starter items
                        if (!parsedItems.get()) {
                            for (ItemInfo itemInfo : itemInfos) {
                                Identifier identifier = itemInfo.itemId;

                                if (!Registry.ITEM.containsId(identifier)) {
                                    throw new RuntimeException("Starter Item {" + identifier + "} does not exist in the Item registry.  " +
                                            "Are you sure the identifier is correct?");
                                }

                                Item item = Registry.ITEM.get(identifier);
                                int count = itemInfo.countStr == null ? 1 : Integer.parseInt(itemInfo.countStr.trim());

                                ItemStack itemStack = new ItemStack(item, count);
                                if (itemInfo.nbt != null) {
                                    itemStack.setNbt(itemInfo.nbt);
                                }
                                starterItemStacks.add(itemStack);
                            }
                            parsedItems.set(true);
                        }

                        if (!player.addScoreboardTag(JOINED_ID)) {
                            throw new RuntimeException("Player " + player + " scoreboard tags are full!  Might need to find a different way to track player joined worlds");
                        } else {
                            // send first join messages to player
                            log.info("Sending first join message to player {}", player);
                            sendMessagesToPlayer(player, firstJoinMessages);
                        }

                        // Starter Items
                        if (!starterItemStacks.isEmpty()) {
                            log.info("Giving starter items to player={}", player);
                            if (ModConfig.get().clearInventoryBeforeGivingItems) {
                                log.info("Clearing player inventory before giving starter items");
                                player.getInventory().clear();
                            }

                            // Copy as inserting stacks mutates it
                            List<ItemStack> starterItemStacksCopy = copyItemStacks(starterItemStacks);
                            for (ItemStack itemStack : starterItemStacksCopy) {
                                if (!player.giveItemStack(itemStack)) {
                                    player.dropStack(itemStack);
                                }
                            }
                        }
                    }
                    
                    // Keep track of loaded players
                    LOADED_PLAYERS.add(player.getGameProfile().getId().toString());
                }
            });
            
            if (ModConfig.get().clearInventoryBeforeGivingItems) {
                Identifier postDefaultPhase = new Identifier(MODID, "postdefault");
                ServerEntityEvents.ENTITY_LOAD.addPhaseOrdering(Event.DEFAULT_PHASE, postDefaultPhase);
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

    private void sendMessagesToPlayer(ServerPlayerEntity player, List<Text> firstJoinMessages) {
        if (player != null && !firstJoinMessages.isEmpty()) {
            for (Text message : firstJoinMessages) {
                player.sendMessage(message, MessageType.CHAT, Util.NIL_UUID);
            }
        }
    }
    
    private List<Text> parseMessages(List<Message> messages) {
        Pattern colorPattern = Pattern.compile("^(#[0-9a-fA-F]{6})|(\\d+$)");
        List<Text> res = new ArrayList<>();
        for (Message s : messages) {
            MutableText text = new LiteralText(s.text);
            
            String colorStr = s.color;
            if (colorStr != null) {
                if (!colorPattern.matcher(colorStr).matches()) {
                    throw new RuntimeException("[StarterItems] Pattern {" + colorStr + "} is not a valid color.  Must be RGB Decimal or Hex color");
                }
                
                int color;
                if (colorStr.startsWith("#")) {
                    color = Integer.parseInt(colorStr.substring(1), 16);
                } else {
                    color = Integer.parseInt(colorStr);
                }

                text = text.setStyle(Style.EMPTY.withColor(color));
            }
            
            res.add(text);
        }
        return res;
    }
    
    @AllArgsConstructor
    @ToString
    private static class ItemInfo {
        @Nullable String countStr;
        Identifier itemId;
        @Nullable NbtCompound nbt;
    }
}
