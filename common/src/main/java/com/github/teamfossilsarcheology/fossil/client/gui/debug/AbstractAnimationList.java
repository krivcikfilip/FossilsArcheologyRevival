package com.github.teamfossilsarcheology.fossil.client.gui.debug;

import com.github.teamfossilsarcheology.fossil.entity.animation.AnimationInfo;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AbstractAnimationList extends DebugSelectionList<AbstractAnimationList.AnimationEntry> {
    private final Consumer<AnimationObject> consumer;
    protected String currentControllerName;
    protected double speed = 1;
    protected int transitionLength = 5;
    protected boolean loop;

    public AbstractAnimationList(int x0, int height, int itemHeight, int yOffset, Map<String, ? extends AnimationInfo> animations, Minecraft minecraft, Consumer<AnimationObject> function) {
        super(minecraft, 100, 215, height, yOffset, height + 60, itemHeight);
        //PORT(1.21.1): x0/x1 fields -> widget position (the width stays the same)
        setX(x0);
        List<String> sortedAnimations = animations.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getKey).toList();
        sortedAnimations.forEach(animation -> addEntry(new AnimationEntry(animation)));
        this.consumer = function;
    }

    //PORT(1.21.1): setRenderBackground(false) was removed (1.20.5), the list background is a separate method now
    @Override
    protected void renderListBackground(GuiGraphics guiGraphics) {
    }

    //PORT(1.21.1): setRenderTopAndBottom(false) was removed (1.20.5), the separators are a separate method now
    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {
    }

    /**
     * PORT(1.21.1): getEntryAtPosition is final in vanilla; this override needs the {@code extendable} entry in fossil.accesswidener
     * (kept by core).
     * PORT(1.21.1, verified phase 2): verify the AW 'extendable' entry is converted to an AT by Loom on NeoForge (otherwise this fails to load) -> remapJar writes public-f to META-INF/accesstransformer.cfg
     */
    @Override
    protected @Nullable AnimationEntry getEntryAtPosition(double mouseX, double mouseY) {
        int x1 = getEntryLeftPos() + getRowWidth();
        int m = Mth.floor(mouseY - (double) getY()) - headerHeight + (int) getScrollAmount() - 4;
        int n = m / itemHeight;
        if (mouseX < getScrollbarPosition() && mouseX >= getX() && mouseX <= x1 && n >= 0 && m >= 0 && n < getItemCount()) {
            return children().get(n);
        }
        return null;
    }

    protected int getEntryLeftPos() {
        return getX();
    }

    @Override
    protected int getScrollbarPosition() {
        return getX() + rowWidth + 6;
    }

    protected class AnimationEntry extends ContainerObjectSelectionList.Entry<AnimationEntry> {
        private final Button changeButton;

        AnimationEntry(String animation) {
            String[] split = animation.split("\\.");
            Component display = Component.literal(split.length > 0 ? StringUtils.capitalize(split[split.length - 1]) : "");
            changeButton = Button.builder(display, button -> consumer.accept(new AnimationObject(animation, currentControllerName, speed, transitionLength, loop)))
                    .bounds(0, 0, 100, 20).build();
        }

        @Override
        public @NotNull List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(changeButton);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver,
                           float partialTick) {
            changeButton.setX(getEntryLeftPos());
            changeButton.setY(top);
            changeButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return ImmutableList.of(changeButton);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return changeButton.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return changeButton.mouseReleased(mouseX, mouseY, button);
        }
    }

    public record AnimationObject(String name, String controller, double speed, int transitionLength, boolean loop) {

    }
}
