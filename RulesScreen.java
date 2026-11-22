package com.example.chestsorter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Редактор правил одного сундука. */
public class RulesScreen extends Screen {
    private static final int ROWS = 7;
    private final String key;
    private final List<String> rules;
    private EditBox input;
    private int scroll = 0;

    public RulesScreen(String key) {
        super(Component.literal("Правила сундука"));
        this.key = key;
        this.rules = new ArrayList<>(RuleStore.get(key));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        input = new EditBox(font, cx - 150, 28, 190, 20, Component.literal("правило"));
        input.setMaxLength(100);
        input.setHint(Component.literal("id, #тег, @мод, ~часть"));
        addRenderableWidget(input);

        addRenderableWidget(Button.builder(Component.literal("Добавить"), b -> addRule(input.getValue()))
                .bounds(cx + 44, 28, 56, 20).build());
        addRenderableWidget(Button.builder(Component.literal("В руке"), b -> addHeld())
                .bounds(cx + 102, 28, 48, 20).build());

        for (int i = 0; i < ROWS; i++) {
            int idx = scroll + i;
            if (idx >= rules.size()) break;
            final int remove = idx;
            addRenderableWidget(Button.builder(Component.literal("X"), b -> {
                rules.remove(remove);
                RuleStore.set(key, rules);
                rebuildWidgets();
            }).bounds(cx + 126, 58 + i * 22, 24, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose())
                .bounds(cx - 50, height - 28, 100, 20).build());
    }

    private void addRule(String raw) {
        String r = raw.trim();
        if (r.isEmpty() || rules.contains(r)) return;
        rules.add(r);
        RuleStore.set(key, rules);
        scroll = Math.max(0, rules.size() - ROWS);
        rebuildWidgets();
    }

    private void addHeld() {
        if (minecraft == null || minecraft.player == null) return;
        ItemStack s = minecraft.player.getMainHandItem();
        if (!s.isEmpty()) addRule(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && input != null && input.isFocused()) {
            addRule(input.getValue());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int max = Math.max(0, rules.size() - ROWS);
        int ns = Math.max(0, Math.min(max, scroll - (int) Math.signum(sy)));
        if (ns != scroll) {
            scroll = ns;
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int cx = width / 2;
        g.drawCenteredString(font, title, cx, 10, 0xFFFFFF);
        for (int i = 0; i < ROWS; i++) {
            int idx = scroll + i;
            if (idx >= rules.size()) break;
            g.drawString(font, rules.get(idx), cx - 150, 64 + i * 22, 0xFFFFFF);
        }
        if (rules.isEmpty()) {
            g.drawCenteredString(font, Component.literal("Правил нет"), cx, 90, 0xAAAAAA);
        }
    }

    @Override
    public void onClose() {
        RuleStore.set(key, rules);
        super.onClose();
    }
}
