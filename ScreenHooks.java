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
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

/** Панель СЛЕВА от окна сундука: категории + кнопки. */
@EventBusSubscriber(modid = ChestSorter.MODID, value = Dist.CLIENT)
public class ScreenHooks {
    private static final String[] SHORT = {"Инстр.", "Ферма", "Дерево", "Мясо", "Блоки"};

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof ContainerScreen cs)) return;
        final String key = resolveKey(cs);

        int w = 78;
        int x = Math.max(2, cs.getGuiLeft() - w - 3);
        int y = cs.getGuiTop();

        int i = 0;
        for (Category c : Category.values()) {
            e.addListener(new SideButton(x, y + i * 20, w, 18, c.icon.getDefaultInstance(), SHORT[i], c.title,
                    b -> toggle(key, c), () -> RuleStore.get(key).contains(c.rule())));
            i++;
        }

        int y2 = y + 5 * 20 + 6;
        e.addListener(Button.builder(Component.literal("Сортировать"), b -> deposit(cs, key))
                .bounds(x, y2, w, 18).build());
        e.addListener(Button.builder(Component.literal("Правила"),
                        b -> Minecraft.getInstance().setScreen(new RulesScreen(key)))
                .bounds(x, y2 + 20, w, 18).build());
        e.addListener(Button.builder(Component.literal("Запомнить"), b -> learn(cs, key))
                .bounds(x, y2 + 40, w, 18).build());
    }

    /** Ключ сундука: сначала по блоку, на который смотрит игрок, потом по последнему клику, потом по названию. */
    private static String resolveKey(ContainerScreen cs) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.hitResult instanceof BlockHitResult bhr) {
            String k = ClientEvents.keyFor(mc.level, bhr.getBlockPos());
            if (k != null) return k;
        }
        if (ClientEvents.lastKey != null) return ClientEvents.lastKey;
        return ClientEvents.worldId() + "|container|" + cs.getTitle().getString();
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
            msg("Сначала выберите категорию слева");
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
