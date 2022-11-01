package com.dan.achievementstats.client.gui;

import com.dan.achievementstats.AchievementStat;
import com.dan.achievementstats.client.AchievementStatsClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;

public class TitleListWidget {

    private int x;
    private int y;
    private int width;
    private int height;
    private List<Title> titles;
    private Title activeTitle;
    private ScrollBar scrollBar;

    public TitleListWidget(int x, int y, int width, int height, List<Title> titles) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.titles = createTitleList(titles);
        this.activeTitle = null;
        scrollBar = new ScrollBar(0, titles.size() - 18);
    }

    static void beginScissor(double x, double y, double endX, double endY) {
        double width = endX - x;
        double height = endY - y;
        width = Math.max(0, width);
        height = Math.max(0, height);
        float d = (float) MinecraftClient.getInstance().getWindow().getScaleFactor();
        int ay = (int) ((MinecraftClient.getInstance().getWindow().getScaledHeight() - (y + height)) * d);
        RenderSystem.enableScissor((int) (x * d), ay, (int) (width * d), (int) (height * d));
    }

    public void addTitle(Title title) {
        this.titles.add(title);
    }

    public int getX() {
        return x;
    }

    private List<Title> createTitleList(List<Title> titls) {
        List<Title> result = new ArrayList<>();
        int y = this.y;
        for (Title titl : titls) {
            result.add(new Title(titl.getTitle(), titl.getIdentifier(), this.x, y));
            y += MinecraftClient.getInstance().textRenderer.fontHeight + 2;
        }
        return result;
    }

    public AchievementStat getStat(String stat) {
        for (AchievementStat achievementStat : AchievementStatsClient.stats) {
            if (achievementStat.getStat().equals(stat.replaceAll("minecraft:", ""))) return achievementStat;
        }
        return null;
    }

    public void render(MatrixStack matrices) {
        //begin scissor for smooth rendering and scroll
        beginScissor(x, y, x + width, y + height);
        int currentScroll = this.scrollBar.current;
        int currentY = this.y;
        for (Title title : titles) {
            if(getStat(title.getIdentifier()) == null) {
                return;
            }
            if (currentScroll != 0) {
                currentScroll--;
                continue;
            }
            title.render(matrices, currentY);
            currentY += MinecraftClient.getInstance().textRenderer.fontHeight + 2;
        }
        RenderSystem.disableScissor();
    }

    public void onMouseClicked(double mouseX, double mouseY) {
        for (Title title : titles) {
            title.onMousePressed(mouseX, mouseY);
            if (title.isSelected()) {
                this.activeTitle = title;
            }
        }
    }

    public Title getActiveTitle() {
        return activeTitle;
    }

    public void desel() {
        this.activeTitle = null;
    }

    public void onScroll(double delta) {
        this.scrollBar.onScroll(delta);
    }

    static class ScrollBar {

        private int min;
        private int max;
        private int current;

        public ScrollBar(int min, int max) {
            this.min = min;
            this.current = 0;
            this.max = max;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        public void onScroll(double delta) {
            if (delta == 1 && this.current == max) return;
            else if (delta == -1 && this.current == min) return;
            if (delta == 1.0) this.current++;
            else this.current--;
        }
    }


}
