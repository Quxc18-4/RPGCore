package vn.minecraft.rPGCore.classes.impl.bard.tones;

import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class ToneModifier {

    // ==========================================
    // 1. ÂM CAO (HIGH): +25% Hệ số Buff/Sát thương
    // ==========================================
    public static double getHighToneMultiplier(Player player) {
        int count = ToneManager.getToneCount(player, ToneType.HIGH);
        // Trả về hệ số nhân (VD: 2 Âm = 1.0 + 0.5 = 1.5)
        return 1.0 + (count * 0.25);
    }

    // ==========================================
    // 2. ÂM VANG (ECHO): Tăng Bán Kính/Diện Tích
    // ==========================================
    public static double getEchoRadiusMultiplier(Player player, RPGPlayer rpgPlayer) {
        int count = ToneManager.getToneCount(player, ToneType.ECHO);

        // Kiểm tra xem người chơi có Lõi Nâng Cấp số 3 (Crescendo) không
        // Giả lập ID Lõi là BARD_CORE_3. Sẽ khớp với hệ thống Nâng cấp sau.
        boolean hasCrescendo = rpgPlayer.hasSetBonus("BARD_CORE_3", 1);

        double bonusPerStack = hasCrescendo ? 0.80 : 0.50;
        return 1.0 + (count * bonusPerStack);
    }

    // ==========================================
    // 3. ÂM TRẦM (LOW): Tăng Thời Gian Duy Trì (Duration)
    // ==========================================
    public static double getLowDurationMultiplier(Player player) {
        int count = ToneManager.getToneCount(player, ToneType.LOW);
        double lowBonus = count * 0.30;

        // SỬA LỖI: Dùng trực tiếp chuỗi định danh Metadata thay vì gọi qua Class
        double microBonus = 0.0;
        if (player.hasMetadata("BARD_BONUS_DURATION")) {
            microBonus = player.getMetadata("BARD_BONUS_DURATION").get(0).asDouble() / 100.0;
        }

        return 1.0 + lowBonus + microBonus;
    }

    // ==========================================
    // 4. ÂM SẮC (SHARP): Trả về số lượng để quyết định Buff
    // ==========================================
    public static int getSharpToneCount(Player player) {
        return ToneManager.getToneCount(player, ToneType.SHARP);
    }
}