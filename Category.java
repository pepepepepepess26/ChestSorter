package com.example.chestsorter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Готовые категории. В правилах сундука хранятся как "$tools", "$farming" и т.д. */
public enum Category {
    TOOLS("tools", "Инструменты и оружие", Items.IRON_SWORD),
    FARMING("farming", "Фермерство", Items.WHEAT),
    WOOD("wood", "Дерево", Items.OAK_LOG),
    MEAT("meat", "Мясо", Items.COOKED_BEEF),
    BLOCKS("blocks", "Блоки", Items.DIRT);

    public final String id;
    public final String title;
    public final Item icon;

    Category(String id, String title, Item icon) {
        this.id = id;
        this.title = title;
        this.icon = icon;
    }

    public String rule() {
        return "$" + id;
    }

    public static Category byId(String id) {
        for (Category c : values()) if (c.id.equals(id)) return c;
        return null;
    }

    public boolean matches(ItemStack s) {
        if (s.isEmpty()) return false;
        return switch (this) {
            case TOOLS -> isTool(s);
            case FARMING -> isFarming(s);
            case WOOD -> isWood(s);
            case MEAT -> isMeat(s);
            // блоки: всё, что ставится, кроме того, что относится к другим категориям
            case BLOCKS -> s.getItem() instanceof BlockItem
                    && !isTool(s) && !isFarming(s) && !isWood(s) && !isMeat(s);
        };
    }

    // ---------- helpers ----------

    private static List<TagKey<Item>> tags(String... ids) {
        return Arrays.stream(ids)
                .map(i -> TagKey.create(Registries.ITEM, ResourceLocation.parse(i)))
                .toList();
    }

    private static boolean inAny(ItemStack s, List<TagKey<Item>> list) {
        for (TagKey<Item> t : list) if (s.is(t)) return true;
        return false;
    }

    private static String path(ItemStack s) {
        return BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
    }

    private static final List<TagKey<Item>> TOOL_TAGS = tags(
            "minecraft:swords", "minecraft:pickaxes", "minecraft:axes", "minecraft:shovels", "minecraft:hoes",
            "c:tools", "c:tools/sword", "c:tools/pickaxe", "c:tools/axe", "c:tools/shovel", "c:tools/hoe",
            "c:tools/bow", "c:tools/crossbow", "c:tools/shield", "c:tools/fishing_rod", "c:tools/shear", "c:tools/spear");

    private static boolean isTool(ItemStack s) {
        Item i = s.getItem();
        return i instanceof TieredItem || i instanceof ShearsItem || i instanceof FishingRodItem
                || i instanceof BowItem || i instanceof CrossbowItem || i instanceof TridentItem
                || i instanceof ShieldItem || inAny(s, TOOL_TAGS);
    }

    private static final List<TagKey<Item>> FARM_TAGS = tags(
            "c:crops", "c:seeds", "c:eggs", "c:vegetables", "c:fruits", "c:grains",
            "c:foods/vegetable", "c:foods/fruit", "c:foods/berry", "c:foods/grain", "c:foods/dough",
            "c:fertilizers", "minecraft:villager_plantable_seeds", "minecraft:hoes");
    private static final Set<String> FARM_IDS = Set.of(
            "wheat", "hay_block", "bone_meal", "sugar_cane", "sugar", "melon_slice", "melon", "pumpkin",
            "carrot", "potato", "beetroot", "apple", "sweet_berries", "glow_berries", "cocoa_beans",
            "nether_wart", "bamboo", "kelp", "egg", "milk_bucket");

    private static boolean isFarming(ItemStack s) {
        if (isTool(s) && !s.is(TagKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:hoes")))) return false;
        Item i = s.getItem();
        if (inAny(s, FARM_TAGS) || FARM_IDS.contains(path(s))) return true;
        if (i instanceof BlockItem bi) {
            Block b = bi.getBlock();
            return b instanceof CropBlock || b instanceof StemBlock || b instanceof NetherWartBlock
                    || b instanceof SweetBerryBushBlock || b instanceof CocoaBlock;
        }
        return path(s).contains("seed");
    }

    private static final List<TagKey<Item>> WOOD_TAGS = tags(
            "minecraft:logs", "minecraft:planks", "minecraft:wooden_stairs", "minecraft:wooden_slabs",
            "minecraft:wooden_fences", "minecraft:fence_gates", "minecraft:wooden_doors",
            "minecraft:wooden_trapdoors", "minecraft:wooden_buttons", "minecraft:wooden_pressure_plates",
            "minecraft:saplings", "minecraft:leaves", "minecraft:signs", "minecraft:hanging_signs",
            "minecraft:boats", "c:rods/wooden", "c:strippables/log");

    private static boolean isWood(ItemStack s) {
        if (isTool(s)) return false;
        String p = path(s);
        return inAny(s, WOOD_TAGS) || p.equals("stick") || p.contains("planks")
                || p.contains("_log") || p.contains("_wood") || p.startsWith("stripped_");
    }

    private static final List<TagKey<Item>> MEAT_TAGS = tags(
            "minecraft:meat", "minecraft:fishes",
            "c:foods/raw_meat", "c:foods/cooked_meat", "c:foods/raw_fish", "c:foods/cooked_fish",
            "c:raw_meats", "c:cooked_meats", "c:raw_fishes", "c:cooked_fishes");
    private static final List<String> MEAT_WORDS = List.of(
            "meat", "beef", "pork", "steak", "bacon", "sausage", "mutton", "chicken", "jerky");

    private static boolean isMeat(ItemStack s) {
        if (inAny(s, MEAT_TAGS)) return true;
        String p = path(s);
        if (p.contains("egg") || p.contains("spawn")) return false;
        for (String w : MEAT_WORDS) if (p.contains(w)) return true;
        return false;
    }
}
