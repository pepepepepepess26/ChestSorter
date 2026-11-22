package com.example.chestsorter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Хранит правила: ключ сундука -> список правил. Файл: config/chestsorter_rules.json */
public final class RuleStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("chestsorter_rules.json");
    private static Map<String, List<String>> rules = new LinkedHashMap<>();

    private RuleStore() {}

    public static void load() {
        try {
            if (!Files.exists(FILE)) return;
            try (Reader r = Files.newBufferedReader(FILE)) {
                Type t = new TypeToken<LinkedHashMap<String, List<String>>>() {}.getType();
                Map<String, List<String>> m = GSON.fromJson(r, t);
                if (m != null) rules = m;
            }
        } catch (Exception e) {
            System.err.println("[chestsorter] load failed: " + e);
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(FILE)) {
            GSON.toJson(rules, w);
        } catch (Exception e) {
            System.err.println("[chestsorter] save failed: " + e);
        }
    }

    public static List<String> get(String key) {
        return rules.getOrDefault(key, List.of());
    }

    public static void set(String key, List<String> list) {
        if (list.isEmpty()) rules.remove(key);
        else rules.put(key, new ArrayList<>(list));
        save();
    }

    /**
     * Форматы правил:
     *  $tools, $farming, $wood, $meat, $blocks - готовые категории
     *  minecraft:iron_ingot  - конкретный предмет
     *  #c:ores               - тег
     *  @farmersdelight       - все предметы мода
     *  ~sword                - часть названия
     */
    public static boolean matches(ItemStack stack, String rule) {
        rule = rule.trim();
        if (rule.isEmpty() || stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (rule.startsWith("$")) {
            Category c = Category.byId(rule.substring(1));
            return c != null && c.matches(stack);
        }
        if (rule.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(rule.substring(1));
            return tag != null && stack.is(TagKey.create(Registries.ITEM, tag));
        }
        if (rule.startsWith("@")) return id.getNamespace().equals(rule.substring(1));
        if (rule.startsWith("~")) return id.getPath().contains(rule.substring(1));
        return id.toString().equals(rule.contains(":") ? rule : "minecraft:" + rule);
    }

    public static boolean matchesAny(ItemStack stack, List<String> list) {
        for (String r : list) if (matches(stack, r)) return true;
        return false;
    }
}
