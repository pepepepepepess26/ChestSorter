package com.example.chestsorter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;

/** Кнопка 20x20 с иконкой предмета. Зелёная рамка = категория включена для этого сундука. */
public class CategoryButton extends Button {
    private final ItemStack icon;
    private final BooleanSupplier active;

    public CategoryButton(int x, int y, Category c, BooleanSupplier active, OnPress onPress) {
        super(x, y, 20, 20, Component.literal(c.title), onPress, DEFAULT_NARRATION);
        this.icon = c.icon.getDefaultInstance();
        this.active = active;
        setTooltip(Tooltip.create(Component.literal(c.title)));
    }

    @Override
    public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        super.renderWidget(g, mx, my, pt);
        g.renderItem(icon, getX() + 2, getY() + 2);
        if (active.getAsBoolean()) {
            g.renderOutline(getX(), getY(), width, height, 0xFF55FF55);
            g.renderOutline(getX() + 1, getY() + 1, width - 2, height - 2, 0xFF55FF55);
        }
    }
}
