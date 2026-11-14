package com.github.teamfossilsarcheology.fossil.client.gui.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DebugSelectionList<E extends ContainerObjectSelectionList.Entry<E>> extends ContainerObjectSelectionList<E> {
    protected final List<Renderable> renderables = new ArrayList<>();
    protected final List<GuiEventListener> listeners = new ArrayList<>();
    protected final int rowWidth;
    private GuiEventListener focused;

    /**
     * PORT(1.21.1): AbstractSelectionList is a widget now: (minecraft, width, height, y, itemHeight) where height is the list
     * height (1.20.1: y0..y1). The old screen height parameter is no longer needed but kept for the callers.
     */
    public DebugSelectionList(Minecraft minecraft, int rowWidth, int width, int height, int y0, int y1, int itemHeight) {
        super(minecraft, width, y1 - y0, y0, itemHeight);
        this.rowWidth = rowWidth;
    }

    @Override
    public int getRowWidth() {
        return rowWidth;
    }

    @Override
    public @NotNull Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        for (GuiEventListener guiEventListener : listeners) {
            if (!guiEventListener.isMouseOver(mouseX, mouseY)) continue;
            return Optional.of(guiEventListener);
        }
        return super.getChildAt(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (GuiEventListener guiEventListener : listeners) {
            if (!guiEventListener.mouseClicked(mouseX, mouseY, button)) continue;
            focused = guiEventListener;
            if (button == 0) {
                setDragging(true);
            }
            return true;
        }
        focused = null;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (focused != null && isDragging() && button == 0) {
            return focused.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * PORT(1.21.1): renderList -> renderListItems/renderWidget. The extra widgets (controller buttons etc.) are placed outside
     * the list area, so they are rendered after the list (outside of its scissor area) instead of inside renderListItems.
     */
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        renderables.forEach(renderable -> renderable.render(guiGraphics, mouseX, mouseY, partialTick));
    }

    protected <T extends Renderable & GuiEventListener> void removeWidget(T widget) {
        renderables.remove(widget);
        listeners.remove(widget);
    }

    protected <T extends Renderable & GuiEventListener> T addWidget(T widget) {
        renderables.add(widget);
        listeners.add(widget);
        return widget;
    }
}
