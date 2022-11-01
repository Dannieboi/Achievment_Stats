package com.dan.achievementstats.server;

import com.dan.achievementstats.AchievementStat;
import com.dan.achievementstats.Packets;
import com.dan.achievementstats.Stat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancement.Advancement;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class AchievementStatsServer implements ModInitializer {

    private static final SuggestionProvider<ServerCommandSource> SUGGESTION_PROVIDER = (context, builder) -> {
        Collection<Advancement> collection = ((ServerCommandSource) context.getSource()).getServer().getAdvancementLoader().getAdvancements();
        //remove recipes advancements from the suggestions
        collection.removeIf((advancement -> advancement.getId().toString().contains("recipe")));
        return CommandSource.suggestIdentifiers(collection.stream().map(Advancement::getId), builder);
    };

    //stats suggestions
    private static final SuggestionProvider<ServerCommandSource> suggestions = ((context, builder) -> {
        Collection<Stat> collection = Arrays.asList(new Stat("attackdamage", 69), new Stat("movementspeed", 69), new Stat("health", 69), new Stat("miningspeed", 69));
        return CommandSource.suggestIdentifiers(collection.stream().map(Stat::getStat), builder);
    });

    public static void clearStats(PlayerEntity player, boolean life) {
        if (life) {
            EntityAttributeInstance instanceHealth = player.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (instanceHealth != null) {
                instanceHealth.clearModifiers();
                player.setHealth(20.0f);
            }
        }
        EntityAttributeInstance instanceSpeed = player.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (instanceSpeed != null) {
            instanceSpeed.clearModifiers();
            //default speed
            player.setMovementSpeed(0.0f);
        }
        EntityAttributeInstance instanceAttack = player.getAttributes().getCustomInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (instanceAttack != null) {
            instanceAttack.clearModifiers();
        }
    }

    /**
     * Executes the title change
     *
     * @param player , can be either ServerPlayerEntity or ClientPlayerEntity
     * @param stat   , selected title stats
     */
    public static void executeChange(PlayerEntity player, List<AchievementStat> stat, List<AchievementStat> statsBefore) {

        float totalHealth = 0.0f;
        float totalAttack = 0.0f;
        float totalSpeed = 0.0f;

        float totalHealthP = 0.0f;
        float totalAttackP = 0.0f;
        float totalSpeedP = 0.0f;

        //compare all previous stats with current to see if anything needs to be reset
        for (AchievementStat s : statsBefore) {
            totalAttackP += s.getAttacKDamage().getValue();
            totalHealthP += s.getHealth().getValue();
            totalSpeedP += s.getSpeed().getValue();
        }

        for (AchievementStat s : stat) {
            totalAttack += s.getAttacKDamage().getValue();
            totalHealth += s.getHealth().getValue();
            totalSpeed += s.getSpeed().getValue();
        }

        clearStats(player, totalHealthP > totalHealth);
        //health modifiers

        EntityAttributeInstance instanceHealth = player.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (instanceHealth != null && totalHealth != 0.0f && totalHealth != totalHealthP) {
            float healthToAdd = 0.0f;
            if(totalHealth > totalHealthP) {
                healthToAdd = totalHealth - totalHealthP;
            } else {
                healthToAdd = totalHealth;
            }
            instanceHealth.addTemporaryModifier(new EntityAttributeModifier(player.getName().getString(), healthToAdd, EntityAttributeModifier.Operation.ADDITION));
        }

        //speed modifiers
        EntityAttributeInstance instanceSpeed = player.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (instanceSpeed != null && totalSpeed != 0.0f) {
            instanceSpeed.addTemporaryModifier(new EntityAttributeModifier(player.getName().getString(), totalSpeed * 0.03, EntityAttributeModifier.Operation.ADDITION));
        }

        //Attack damage modifiers
        EntityAttributeInstance instanceAttack = player.getAttributes().getCustomInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (instanceAttack != null && totalAttack != 0.0f) {
            instanceAttack.addTemporaryModifier(new EntityAttributeModifier(player.getName().getString(), totalAttack * 0.2, EntityAttributeModifier.Operation.ADDITION));
        }
    }

    @Override
    public void onInitialize() {
        Config.setInstance(new Config());
        Config.getInstance().loadConfig();
        Gson GSON = new GsonBuilder().create();
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {

            dispatcher.register(CommandManager.literal("achievementstats").requires(player -> player.hasPermissionLevel(2))
                    .then(CommandManager.literal("title_amount")
                            .then(CommandManager.argument("amount", IntegerArgumentType.integer())
                                    .executes(context -> {
                                        int amount = IntegerArgumentType.getInteger(context, "amount");
                                        if (amount < Config.getInstance().titleAmount) {
                                            Config.getInstance().map.clear();
                                            PacketByteBuf data = PacketByteBufs.create();
                                            data.writeString("");
                                            context.getSource().getServer().getPlayerManager().sendToAll(ServerPlayNetworking.createS2CPacket(Packets.SELECTED_TITLE, data));
                                            for (ServerPlayerEntity serverPlayerEntity : context.getSource().getServer().getPlayerManager().getPlayerList()) {
                                                clearStats(serverPlayerEntity, true);
                                            }
                                        }
                                        Config.getInstance().titleAmount = amount;
                                        Config.getInstance().saveConfig();
                                        PacketByteBuf data = PacketByteBufs.create();
                                        data.writeString(GSON.toJson(Config.getInstance().stats));
                                        data.writeInt(Config.getInstance().titleAmount);
                                        context.getSource().getServer().getPlayerManager().sendToAll(ServerPlayNetworking.createS2CPacket(Packets.SEND_INFO, data));
                                        return 1;
                                    })))
                    .then(CommandManager.argument("advancement", IdentifierArgumentType.identifier()).suggests(SUGGESTION_PROVIDER)
                            .then(CommandManager.argument("stat", IdentifierArgumentType.identifier()).suggests(suggestions)
                                    .then(CommandManager.argument("amount", FloatArgumentType.floatArg(0))
                                            .executes(context -> {
                                                String advancement = IdentifierArgumentType.getIdentifier(context, "advancement").getPath();
                                                AchievementStat stat = new AchievementStat(advancement);
                                                AchievementStat previous = Config.getInstance().getStat(advancement);
                                                if (previous == null) {
                                                    previous = new AchievementStat("sexo");
                                                }
                                                String statToAdd = IdentifierArgumentType.getIdentifier(context, "stat").getPath();
                                                float value = FloatArgumentType.getFloat(context, "amount");
                                                if (statToAdd.equals("attackdamage")) {
                                                    stat.setAttacKDamage(value);
                                                } else {
                                                    stat.setAttacKDamage(previous.getAttacKDamage().getValue());
                                                }
                                                if (statToAdd.equals("movementspeed")) {
                                                    stat.setSpeed(value);
                                                } else {
                                                    stat.setSpeed(previous.getSpeed().getValue());
                                                }
                                                if (statToAdd.equals("health"))
                                                    stat.setHealth(value);
                                                else
                                                    stat.setHealth(previous.getHealth().getValue());
                                                if (statToAdd.equals("miningspeed")) {
                                                    stat.setMiningSpeed(value);
                                                } else {
                                                    stat.setMiningSpeed(previous.getMiningSpeed().getValue());
                                                }
                                                Config.getInstance().addStat(stat);
                                                Config.getInstance().saveConfig();
                                                PacketByteBuf data = PacketByteBufs.create();
                                                data.writeString(GSON.toJson(Config.getInstance().stats));
                                                data.writeInt(Config.getInstance().titleAmount);
                                                context.getSource().getServer().getPlayerManager().sendToAll(ServerPlayNetworking.createS2CPacket(Packets.SEND_INFO, data));
                                                return 1;
                                            })))));
        });

        // C2S , on client title changed
        ServerPlayNetworking.registerGlobalReceiver(Packets.TITLE_CHANGED_PACKED, ((server, player, handler, buf, responseSender) -> {
            List<AchievementStat> previous = Config.getInstance().getAllStats(player.getName().getString());
            String data = buf.readString();
            if (data.startsWith("null")) {
                Config.getInstance().removeTitle(player.getName().getString(), data.split("null")[1]);
            } else {
                List<String> s = new ArrayList<>();
                s.add(data);
                Config.getInstance().addPlayer(player.getName().getString(), s);
            }
            Config.getInstance().saveConfig();
            List<AchievementStat> stat = Config.getInstance().getAllStats(player.getName().getString());
            executeChange(player, stat, previous);
        }));

        //C2S , request data, returns SEND_INFO
        ServerPlayNetworking.registerGlobalReceiver(Packets.REQUEST_INFO, ((server, player, handler, buf, responseSender) -> {
            PacketByteBuf data = PacketByteBufs.create();
            data.writeString(GSON.toJson(Config.getInstance().stats));
            data.writeInt(Config.getInstance().titleAmount);
            responseSender.sendPacket(Packets.SEND_INFO, data);
        }));

        //C2C , requests player selected title
        ServerPlayNetworking.registerGlobalReceiver(Packets.SELECTED_TITLE, ((server, player, handler, buf, responseSender) -> {
            try {
                PacketByteBuf data = PacketByteBufs.create();
                List<String> title = Config.getInstance().getPlayerTitles(player.getName().getString().trim());
                data.writeString(GSON.toJson(title));
                responseSender.sendPacket(Packets.SELECTED_TITLE, data);
            } catch (Exception e) {

            }
        }));
    }
}
