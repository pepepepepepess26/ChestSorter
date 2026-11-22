package com.example.chestsorter;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = ChestSorter.MODID, dist = Dist.CLIENT)
public class ChestSorter {
    public static final String MODID = "chestsorter";

    public ChestSorter(IEventBus modBus) {
        RuleStore.load();
    }
}
