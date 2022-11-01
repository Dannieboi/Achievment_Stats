package com.dan.achievementstats.client;

import com.dan.achievementstats.AchievementStat;
import com.dan.achievementstats.Packets;
import com.dan.achievementstats.client.gui.TitleScreen;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.impl.client.keybinding.KeyBindingRegistryImpl;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.List;

public class AchievementStatsClient implements ClientModInitializer {

    public static final KeyBinding OPEN_MENU = new KeyBinding("Title select screen", InputUtil.Type.KEYSYM, -1, "Achievement Stats");
    public static List<String> titleSelected = new ArrayList<>();
    public static int maxTitles = 1;
    public static List<AchievementStat> stats = new ArrayList<>();
    public static final Gson GSON = new GsonBuilder().create();

    @Override
    public void onInitializeClient() {
        KeyBindingRegistryImpl.registerKeyBinding(OPEN_MENU);
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (OPEN_MENU.wasPressed()) {
                client.setScreen(new TitleScreen());
            }
        });

        // C2S, on join data request
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            sender.sendPacket(Packets.REQUEST_INFO, PacketByteBufs.create());
            sender.sendPacket(Packets.SELECTED_TITLE, PacketByteBufs.create());
        });

        //S2C, titles stats
        ClientPlayNetworking.registerGlobalReceiver(Packets.SEND_INFO, ((client, handler, buf, responseSender) -> {
            try {
                String s = buf.readString();
                System.out.println(s);
                stats = GSON.fromJson(s, new TypeToken<ArrayList<AchievementStat>>() {
                }.getType());
                maxTitles = buf.readInt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        //S2C, client selected title
        ClientPlayNetworking.registerGlobalReceiver(Packets.SELECTED_TITLE, ((client, handler, buf, responseSender) -> {
            try {
            String data = buf.readString();
            titleSelected = GSON.fromJson(data, new TypeToken<ArrayList<AchievementStat>>() {
            }.getType());
            if(titleSelected == null) {
                titleSelected = new ArrayList<>();
            }
            } catch (Exception e) {

            }
        }));
    }
}
