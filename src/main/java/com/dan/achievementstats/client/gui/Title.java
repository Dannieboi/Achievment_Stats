package com.dan.achievementstats.client.gui;

import com.dan.achievementstats.client.AchievementStatsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Range;

import java.awt.*;

public class Title {

    private String title;
    private int x;
    private int y;
    private boolean selected;
    private String identifier;

    public Title(String title, String identifier, int x, int y) {
        this.title = title;
        this.identifier = identifier;
        this.x = x;
        this.selected = false;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getTitle() {
        return title;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void render(MatrixStack matrices, int yDifference) {
        String textToRender = this.title;
        int color = -1;
        if (this.selected) {
            textToRender = "§n" + textToRender;
            color = new Color(120, 190, 210).getRGB();
        }
        if(AchievementStatsClient.titleSelected.contains(this.title)) color = RGBAToInt(0, 0, 51, 255);
        this.y = yDifference;
        MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, this.title, this.x, yDifference, color, false);
    }


    public int RGBAToInt(@Range(from = 0, to = 255) int r, @Range(from = 0, to = 255) int g, @Range(from = 0, to = 255) int b, @Range(from = 0, to = 255) int a) {
        return r << (8 * 3) | g << (8 * 2) | b << 8 | a;
    }

    public void onMousePressed(double mouseX, double mouseY) {
        this.selected = isHovered(mouseX, mouseY);
    }

    public boolean isHovered(double mouseX, double mouseY) {
        //multiply by scale
        return mouseX >= this.x && mouseX <= this.x + MinecraftClient.getInstance().textRenderer.getWidth(this.title) && mouseY >= this.y && mouseY <= this.y + MinecraftClient.getInstance().textRenderer.fontHeight;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }


}
