package com.example.chestsorter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

/** Панель справа от окна сундука: иконки категорий + кнопки. */
@EventBusSubscriber(modid = ChestSorter.MODID, value = Dist.CLIENT)
public class ScreenHooks {

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof ContainerScreen cs)) return;
        String key = ClientEvents.lastKey;
        if (key == null) return;

        int x = cs.getGuiLeft() + cs.getXSize() + 4;
        int y = cs.getGuiTop();

        // иконки категорий (клик = включить/выключить для этого сундука)
        int i = 0;
        for (Category c : Category.values()) {
            e.addListener(new CategoryButton(x + i * 22, y, c,
                    () -> RuleStore.get(key).contains(c.rule()),
                    b -> toggle(key, c)));
            i++;
        }

        e.addListener(Button.builder(Component.literal("Сортировать"), b -> deposit(cs, key))
                .bounds(x, y + 24, 108, 20).build());
        e.addListener(Button.builder(Component.literal("Свои правила"),
                        b -> Minecraft.getInstance().setScreen(new RulesScreen(key)))
                .bounds(x, y + 46, 108, 20).build());
        e.addListener(Button.builder(Component.literal("Запомнить содержимое"), b -> learn(cs, key))
                .bounds(x, y + 68, 108, 20).build());
    }

    private static void toggle(String key, Category c) {
        List<String> l = new ArrayList<>(RuleStore.get(key));
        boolean on = !l.remove(c.rule());
        if (on) l.add(c.rule());
        RuleStore.set(key, l);
        msg(c.title + (on ? ": включено" : ": выключено"));
    }

    /** Кладёт в сундук подходящие предметы из основного инвентаря (хотбар не трогаем). */
    private static void deposit(ContainerScreen cs, String key) {
        Minecraft mc = Minecraft.getInstance();
        List<String> rules = RuleStore.get(key);
        if (rules.isEmpty()) {
            msg("Выберите иконку категории у этого сундука");
            return;
        }
        AbstractContainerMenu menu = cs.getMenu();
        int moved = 0;
        for (Slot slot : menu.slots) {
            if (!(slot.container instanceof Inventory)) continue;
            int idx = slot.getContainerSlot();
            if (idx < 9 || idx > 35) continue;
            ItemStack s = slot.getItem();
            if (s.isEmpty() || !RuleStore.matchesAny(s, rules)) continue;
            mc.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, 0, ClickType.QUICK_MOVE, mc.player);
            moved++;
        }
        msg("Перенесено стаков: " + moved);
    }

    private static void learn(ContainerScreen cs, String key) {
        List<String> rules = new ArrayList<>(RuleStore.get(key));
        int added = 0;
        for (Slot slot : cs.getMenu().slots) {
            if (slot.container instanceof Inventory) continue;
            ItemStack s = slot.getItem();
            if (s.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
            if (!rules.contains(id)) {
                rules.add(id);
                added++;
            }
        }
        RuleStore.set(key, rules);
        msg("Добавлено правил: " + added);
    }

    private static void msg(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal(text), true);
    }
}
