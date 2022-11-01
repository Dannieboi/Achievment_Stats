package com.dan.achievementstats.client.gui;

import com.dan.achievementstats.AchievementStat;
import com.dan.achievementstats.Packets;
import com.dan.achievementstats.client.AchievementStatsClient;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.network.ClientAdvancementManager.Listener;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TitleScreen extends Screen implements Listener {

    //308 x 201
    private final Identifier BACKGROUND = new Identifier("achievementstats", "textures/book.png");

    private final ClientAdvancementManager advancementHandler;
    private final Map<Advancement, Advancement> tabs = Maps.newLinkedHashMap();
    private Title currentTitle;
    private TitleListWidget widget;
    private ButtonWidget selectButton;
    private ButtonWidget removeButton;
    private List<Title> titles;

    public TitleScreen() {
        super(Text.of(""));
        titles = new ArrayList<>();
        this.advancementHandler = MinecraftClient.getInstance().player.networkHandler.getAdvancementHandler();
    }

    //Screen.drawTexture() but with custom uv
    public static void renderImage(Matrix4f matrix, float x0, float y0, int u, int v, float width, float height, int regionWidth, int regionHeight, int textureWidth, int textureHeight, float transparency) {
        float x1 = x0 + width;
        float y1 = y0 + height;
        int z = 0;
        float u0 = (u + 0.0F) / (float) textureWidth;
        float u1 = (u + (float) regionWidth) / (float) textureWidth;
        float v0 = (v + 0.0F) / (float) textureHeight;
        float v1 = (v + (float) regionHeight) / (float) textureHeight;
        RenderSystem.enableBlend();
        if (transparency != 1)
            RenderSystem.setShaderColor(1, 1, 1, transparency);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        bufferBuilder.vertex(matrix, (float) x0, (float) y1, (float) z).texture(u0, v1).next();
        bufferBuilder.vertex(matrix, (float) x1, (float) y1, (float) z).texture(u1, v1).next();
        bufferBuilder.vertex(matrix, (float) x1, (float) y0, (float) z).texture(u1, v0).next();
        bufferBuilder.vertex(matrix, (float) x0, (float) y0, (float) z).texture(u0, v0).next();
        Tessellator.getInstance().draw();
        RenderSystem.disableBlend();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //background image
        RenderSystem.setShaderTexture(0, this.BACKGROUND);
        renderImage(matrices.peek().getPositionMatrix(), width / 2 - 160, height / 2 - 128, 5, 5, 306, 245, 245, 251, 251, 251, 1.0f);
        super.render(matrices, mouseX, mouseY, delta);
        this.widget.render(matrices);
        //render the stats if they exist
        if (null != this.widget.getActiveTitle()) {
            selectButton.visible = !AchievementStatsClient.titleSelected.contains(this.widget.getActiveTitle().getTitle());
            if(AchievementStatsClient.maxTitles <= AchievementStatsClient.titleSelected.size()) selectButton.visible = false;
            drawCenteredText(matrices, textRenderer, "§l§n" + this.widget.getActiveTitle().getTitle(), width / 2 + 60, height / 2 - 114, -1);
            AchievementStat stat = getStat(this.widget.getActiveTitle().getIdentifier());
            if (stat != null) {
                drawCenteredText(matrices, textRenderer, "Health: " + stat.getHealth().getValue(), width / 2 + 60, height / 2 - 60, -1);
                drawCenteredText(matrices, textRenderer, "Mining Speed: " + stat.getMiningSpeed().getValue(), width / 2 + 60, height / 2 - 60 + 10, -1);
                drawCenteredText(matrices, textRenderer, "Attack Damage: " + stat.getAttacKDamage().getValue(), width / 2 + 60, height / 2 - 60 + 20, -1);
                drawCenteredText(matrices, textRenderer, "Walk Speed: " + stat.getSpeed().getValue(), width / 2 + 60, height / 2 - 60 + 30, -1);
                if (AchievementStatsClient.titleSelected.contains(this.widget.getActiveTitle().getTitle()))
                    this.removeButton.visible = true;
                else
                    this.removeButton.visible = false;
            }
        }
    }

    //returns stats from identifier
    public AchievementStat getStat(String stat) {
        for (AchievementStat achievementStat : AchievementStatsClient.stats) {
            if (achievementStat.getStat().equals(stat.replaceAll("minecraft:", ""))) return achievementStat;
        }
        return null;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.widget.onMouseClicked(mouseX, mouseY);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        this.widget.onScroll(amount);
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    //update the selected stats
    public void update() {
        AchievementStatsClient.titleSelected.add(this.widget.getActiveTitle().getTitle());
        PacketByteBuf data = PacketByteBufs.create();
        data.writeString(this.widget.getActiveTitle().getIdentifier().replaceAll("minecraft:", ""));
        ClientPlayNetworking.send(Packets.TITLE_CHANGED_PACKED, data);
    }


    @Override
    protected void init() {
        super.init();
        this.advancementHandler.setListener(this);
        selectButton = new ButtonWidget(width / 2 + 40, height / 2 - 60 + 45, 40, 20, Text.of("Select"), (button -> {
            update();
        }));
        this.addDrawableChild(selectButton);
        selectButton.visible = false;
        removeButton = new ButtonWidget(width / 2 + 40, height / 2 - 60 + 67, 40, 20, Text.of("Remove"), (button -> {

            AchievementStatsClient.titleSelected.remove(this.widget.getActiveTitle().getTitle());
            PacketByteBuf data = PacketByteBufs.create();
            data.writeString("null" + this.widget.getActiveTitle().getIdentifier().replaceAll("minecraft:", ""));
            ClientPlayNetworking.send(Packets.TITLE_CHANGED_PACKED, data);
            this.widget.desel();
            this.removeButton.visible = false;
        }));
        removeButton.visible = false;
        this.addDrawableChild(removeButton);
        widget = new TitleListWidget(width / 2 - 118, height / 2 - 114, 110, 198, this.titles);
    }

    public void setTitle(Title title) {
        this.currentTitle = title;
    }


    @Override
    public void close() {
        this.advancementHandler.setListener((Listener) null);
        super.close();
    }

    @Override
    public void onRootAdded(Advancement root) {
        if (root != null) {
            this.tabs.put(root, root);
        }
    }

    @Override
    public void onRootRemoved(Advancement root) {

    }

    @Override
    public void onDependentAdded(Advancement dependent) {

    }

    @Override
    public void onDependentRemoved(Advancement dependent) {

    }

    @Override
    public void onClear() {

    }

    @Override
    public void setProgress(Advancement advancement, AdvancementProgress progress) {
        if (advancement.getId().toString().contains("recipe")) return;
        if (progress.isDone()) {
            String name;
            try {
                name = advancement.getDisplay().getTitle().getString();
            } catch (Exception e) {
                name = advancement.getId().getPath();
            }
            //if stats are none ignore
            if (getStat(advancement.getId().getPath()) == null) return;
            //if the widget hasn't been initialized store them as cache, if not add them to it
            if (this.widget != null)
                this.widget.addTitle(new Title(name, advancement.getId().getPath(), this.widget.getX(), -100));
            else
                this.titles.add(new Title(name, advancement.getId().getPath(), -1, -1));
        }
    }

    @Override
    public void selectTab(@Nullable Advancement advancement) {

    }
}
