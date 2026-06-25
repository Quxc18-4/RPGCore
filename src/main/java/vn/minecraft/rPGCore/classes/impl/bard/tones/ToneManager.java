package vn.minecraft.rPGCore.classes.impl.bard.tones;

import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
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

    // ==========================================
    // 1. LẤY DANH SÁCH ÂM HIỆN TẠI TỪ BỘ NHỚ
    // ==========================================
    @SuppressWarnings("unchecked")
    public static List<ToneType> getCurrentTones(Player player) {
        if (player.hasMetadata(META_TONES_LIST)) {
            Object val = player.getMetadata(META_TONES_LIST).get(0).value();
            if (val instanceof List) {
                return (List<ToneType>) val;
            }
        }
        return new ArrayList<>();
    }

    // ==========================================
    // 2. THÊM 1 ÂM VÀO BỘ NHỚ KHI CAST SKILL
    // ==========================================
    public static void addTone(Player player, RPGPlayer rpgPlayer, ToneType newTone) {
        List<ToneType> currentTones = getCurrentTones(player);

        // Kiểm tra Lõi 8 (Đa Thanh) -> Max 3 Âm, mặc định là Max 2 Âm
        int maxTones = hasPerk(rpgPlayer, "CORE_8_POLYPHONY") ? 3 : 2;

        if (currentTones.size() < maxTones) {
            currentTones.add(newTone);
            saveTones(player, rpgPlayer, currentTones);
        } else {
            // Nếu đã đầy, đẩy Âm cũ nhất ra (Cơ chế Queue/Hàng đợi)
            currentTones.remove(0);
            currentTones.add(newTone);
            saveTones(player, rpgPlayer, currentTones);
        }

        // Hiển thị Action Bar dạng: [HIGH] - [ECHO]
        sendToneActionBar(player, currentTones);
    }

    // ==========================================
    // 3. TIÊU THỤ ÂM (KHI GIAI ĐIỆU BÙNG NỔ)
    // ==========================================
    public static void consumeTones(Player player, RPGPlayer rpgPlayer) {
        List<ToneType> currentTones = getCurrentTones(player);
        if (currentTones.isEmpty()) return;

        List<ToneType> nextTones = new ArrayList<>();

        // Xử lý Lõi 8 (Đa Thanh): Giữ lại (Max Âm - 1) = 2 Âm
        if (hasPerk(rpgPlayer, "CORE_8_POLYPHONY") && currentTones.size() == 3) {
            Collections.shuffle(currentTones);
            nextTones.add(currentTones.get(0));
            nextTones.add(currentTones.get(1));
        }
        // Xử lý Lõi 4 (Hợp Âm Dội Lại): Giữ lại 1 Âm
        else if (hasPerk(rpgPlayer, "CORE_4_REVERB")) {
            nextTones.add(currentTones.get(random.nextInt(currentTones.size())));
        }

        // Ghi đè lại bộ nhớ
        saveTones(player, rpgPlayer, nextTones);
    }

    // ==========================================
    // 4. LẤY HỆ SỐ CỘNG DỒN TỪ CÁC ÂM
    // ==========================================
    public static int getToneCount(Player player, ToneType targetType) {
        int count = 0;
        for (ToneType type : getCurrentTones(player)) {
            if (type == targetType) count++;
        }
        return count;
    }

    // ==========================================
    // 5. TASK TỰ ĐỘNG SINH ÂM (GỌI TỪ NỘI TẠI BARD)
    // ==========================================
    public static void startPassiveToneGeneration(Player player, RPGPlayer rpgPlayer) {
        String activeKey = "BARD_AUTO_TONE_TASK";
        if (player.hasMetadata(activeKey)) return;
        player.setMetadata(activeKey, new FixedMetadataValue(RPGCore.getInstance(), true));

        new BukkitRunnable() {
            @Override
            public void run() {
                // Sửa thành:
                if (!player.isOnline() || player.isDead() || rpgPlayer.getCurrentClass() == null || !rpgPlayer.getCurrentClass().getId().equalsIgnoreCase("bard")) {
                    player.removeMetadata(activeKey, RPGCore.getInstance());
                    this.cancel();
                    return;
                }

                // Không tự sinh Âm nếu đang ở trạng thái đầy (Max Tones)
                int maxTones = hasPerk(rpgPlayer, "CORE_8_POLYPHONY") ? 3 : 2;
                List<ToneType> current = getCurrentTones(player);

                if (current.size() < maxTones) {
                    ToneType[] types = ToneType.values();
                    ToneType randomTone = types[random.nextInt(types.length)];
                    addTone(player, rpgPlayer, randomTone);
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 160L, 160L); // 160 ticks = 8 giây
    }

    // ==========================================
    // HÀM PHỤ TRỢ (HELPER)
    // ==========================================
    private static void saveTones(Player player, RPGPlayer rpgPlayer, List<ToneType> tones) {
        player.setMetadata(META_TONES_LIST, new FixedMetadataValue(RPGCore.getInstance(), tones));

        // Cập nhật số lượng Âm vào Meta để hàm RPGClass -> applyPassiveBonusStats đọc được (Cho Lõi 5 - Overtone)
        player.setMetadata(META_TONE_COUNT, new FixedMetadataValue(RPGCore.getInstance(), tones.size()));
        rpgPlayer.updateMoveSpeed(); // Yêu cầu Spigot cập nhật lại Tốc chạy lập tức nếu có Lõi 5
    }

    private static boolean hasPerk(RPGPlayer rpgPlayer, String perkId) {
        // Giả lập hàm kiểm tra Lõi nâng cấp từ bộ đếm bước chân.
        // Thực tế bạn có thể lưu List<String> perks trong PlayerSkillData hoặc RPGPlayer
        return false;
    }

    private static void sendToneActionBar(Player player, List<ToneType> tones) {
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