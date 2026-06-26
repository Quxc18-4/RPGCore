package vn.minecraft.rPGCore.classes.impl.bard.tones;

import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ToneManager {

    public static final String META_TONES_LIST = "BARD_TONES_LIST";
    public static final String META_TONE_COUNT = "BARD_TONE_COUNT";
    private static final Random random = new Random();

    public static List<ToneType> getCurrentTones(Player player) {
        List<ToneType> tones = new ArrayList<>();
        if (player.hasMetadata(META_TONES_LIST)) {
            Object val = player.getMetadata(META_TONES_LIST).get(0).value();
            if (val instanceof String) {
                String strVal = (String) val;
                if (!strVal.isEmpty()) {
                    for (String s : strVal.split(",")) {
                        try { tones.add(ToneType.valueOf(s)); } catch (Exception ignored) {}
                    }
                }
            } else {
                player.removeMetadata(META_TONES_LIST, RPGCore.getInstance());
            }
        }
        return tones;
    }

    public static void addTone(Player player, RPGPlayer rpgPlayer, ToneType newTone) {
        List<ToneType> currentTones = getCurrentTones(player);
        int maxTones = rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_8") > 0 ? 3 : 2;

        if (currentTones.size() < maxTones) {
            currentTones.add(newTone);
        } else {
            currentTones.remove(0);
            currentTones.add(newTone);
        }
        saveTones(player, rpgPlayer, currentTones);
        sendToneActionBar(player, currentTones);
    }

    public static void consumeTones(Player player, RPGPlayer rpgPlayer) {
        List<ToneType> currentTones = getCurrentTones(player);
        if (currentTones.isEmpty()) return;

        List<ToneType> consumedTones = new ArrayList<>(currentTones);
        List<ToneType> nextTones = new ArrayList<>();

        // [CHUẨN HÓA GDD]: Lõi 8 bị loại bỏ tính năng hoàn trả. Chỉ còn Lõi 4 hoàn trả 1 Âm.
        if (rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_4") > 0) {
            Collections.shuffle(consumedTones);
            nextTones.add(consumedTones.get(0));
            player.sendMessage("§d§l[Hợp Âm Dội Lại] §fHoàn trả 1 Âm sắc ngẫu nhiên!");
        }

        saveTones(player, rpgPlayer, nextTones);
        player.setMetadata("BARD_TONE_TIMER", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), 0));
    }

    public static int getToneCount(Player player, ToneType targetType) {
        int count = 0;
        for (ToneType type : getCurrentTones(player)) {
            if (type == targetType) count++;
        }
        return count;
    }

    private static void saveTones(Player player, RPGPlayer rpgPlayer, List<ToneType> tones) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tones.size(); i++) {
            sb.append(tones.get(i).name());
            if (i < tones.size() - 1) sb.append(",");
        }

        player.setMetadata(META_TONES_LIST, new FixedMetadataValue(RPGCore.getInstance(), sb.toString()));
        player.setMetadata(META_TONE_COUNT, new FixedMetadataValue(RPGCore.getInstance(), tones.size()));
        rpgPlayer.updateMoveSpeed();
    }

    public static void sendToneActionBar(Player player, List<ToneType> tones) {
        StringBuilder sb = new StringBuilder("§d§l♫ ");
        for (int i = 0; i < tones.size(); i++) {
            switch (tones.get(i)) {
                case HIGH: sb.append("§e[Cao]"); break;
                case ECHO: sb.append("§b[Vang]"); break;
                case LOW: sb.append("§6[Trầm]"); break;
                case SHARP: sb.append("§c[Sắc]"); break;
            }
            if (i < tones.size() - 1) sb.append(" §f- ");
        }
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(sb.toString()));
    }
}