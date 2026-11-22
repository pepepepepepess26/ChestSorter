package com.example.chestsorter;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Запоминает, какой контейнер игрок открыл последним (клиент не получает координаты от сервера). */
@EventBusSubscriber(modid = ChestSorter.MODID, value = Dist.CLIENT)
public class ClientEvents {
    public static String lastKey = null;

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        Level level = e.getLevel();
        if (!level.isClientSide()) return;
        BlockPos pos = e.getPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof Container)) return;

        BlockState st = level.getBlockState(pos);
        if (st.getBlock() instanceof ChestBlock && st.hasProperty(ChestBlock.TYPE)
                && st.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(st));
            if (other.asLong() < pos.asLong()) pos = other; // обе половинки двойного сундука = один ключ
        }
        lastKey = worldId() + "|" + level.dimension().location() + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static String worldId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) {
            return "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
        }
        if (mc.getCurrentServer() != null) return "mp:" + mc.getCurrentServer().ip;
        return "unknown";
    }
}
