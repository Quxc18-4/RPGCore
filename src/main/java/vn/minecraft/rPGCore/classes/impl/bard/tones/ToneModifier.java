package vn.minecraft.rPGCore.classes.impl.bard.tones;

import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class ToneModifier {

    // ==========================================
    // 1. ÂM CAO (HIGH): +15% Hệ số Sát thương/Hồi máu cho Kỹ năng
    // Tích hợp cơ chế tính gộp "Âm kích nổ" (Trigger Tone)
    // ==========================================
    public static double getHighToneMultiplier(Player player, ToneType triggerTone) {
        int count = ToneManager.getToneCount(player, ToneType.HIGH);
        if (triggerTone == ToneType.HIGH) count++;
        // Trả về hệ số nhân (VD: 2 Âm = 1.0 + 0.3 = 1.3)
        return 1.0 + (count * 0.15);
    }

    public static double getHighToneMultiplier(Player player) {
        return getHighToneMultiplier(player, null);
    }

    // ==========================================
    // 2. ÂM VANG (ECHO): Tăng Bán Kính/Diện Tích Giai Điệu
    // ==========================================
    public static double getEchoRadiusMultiplier(Player player, RPGPlayer rpgPlayer, ToneType triggerTone) {
        int count = ToneManager.getToneCount(player, ToneType.ECHO);
        if (triggerTone == ToneType.ECHO) count++;

        boolean hasCrescendo = rpgPlayer.hasSetBonus("BARD_CORE_3", 1);
        double bonusPerStack = hasCrescendo ? 0.80 : 0.50;
        return 1.0 + (count * bonusPerStack);
    }

    public static double getEchoRadiusMultiplier(Player player, RPGPlayer rpgPlayer) {
        return getEchoRadiusMultiplier(player, rpgPlayer, null);
    }

    // ==========================================
    // 3. ÂM TRẦM (LOW): Tăng Thời Gian Duy Trì (Duration)
    // ==========================================
    public static double getLowDurationMultiplier(Player player, ToneType triggerTone) {
        int count = ToneManager.getToneCount(player, ToneType.LOW);
        if (triggerTone == ToneType.LOW) count++;

        double lowBonus = count * 0.30;
        double microBonus = 0.0;

        if (player.hasMetadata("BARD_BONUS_DURATION")) {
            microBonus = player.getMetadata("BARD_BONUS_DURATION").get(0).asDouble() / 100.0;
        }

        return 1.0 + lowBonus + microBonus; // Tốc chạy dư thừa + Âm trầm cộng dồn 100%
    }

    public static double getLowDurationMultiplier(Player player) {
        return getLowDurationMultiplier(player, null);
    }

    // ==========================================
    // 4. ÂM SẮC (SHARP): Xuyên Giáp/Kháng Phép cho Kỹ năng
    // ==========================================
    public static int getSharpToneCount(Player player, ToneType triggerTone) {
        int count = ToneManager.getToneCount(player, ToneType.SHARP);
        if (triggerTone == ToneType.SHARP) count++;
        return count;
    }

    public static int getSharpToneCount(Player player) {
        return getSharpToneCount(player, null);
    }
}