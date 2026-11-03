package vn.minecraft.rPGCore.classes.impl.bard;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BardGlobalManager {

    private static boolean isRunning = false;

    private static final Map<UUID, Location> lastLocations = new HashMap<>();
    private static final Map<UUID, Double> pendingDistances = new HashMap<>();
    private static final Map<UUID, Integer> toneTicks = new HashMap<>();

    // 9 MỐC KHOẢNG CÁCH (MILESTONES) TƯƠNG ỨNG VỚI 9 LÕI
    private static final int[] MILESTONES = {
            5000, 50000, 200000, 500000, 1000000, 1800000, 2800000, 4000000, 5500000
    };

    // 9 DANH HIỆU LỊCH SỬ TƯƠNG ỨNG (Theo cấp độ nổi tiếng tăng dần)
    private static final String[] TITLES = {
            "Kẻ Hát Rong Lãng Du",          // Mốc 1
            "Học Giả Âm Thanh",             // Mốc 2
            "Nghệ Nhân Khúc Ca",            // Mốc 3
            "Bậc Thầy Giao Hưởng",          // Mốc 4
            "Nhạc Sư Cung Đình",            // Mốc 5
            "Tiếng Hát Xé Trời",            // Mốc 6
            "Giai Điệu Linh Hồn",           // Mốc 7
            "Huyền Thoại Ngân Vang",        // Mốc 8
            "Thần Đồng Âm Nhạc Vĩ Đại"      // Mốc 9
    };

    public static void startEngine() {
        if (isRunning) return;
        isRunning = true;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());

                    if (rpgPlayer == null || rpgPlayer.getCurrentClass() == null || !rpgPlayer.getCurrentClass().getId().equalsIgnoreCase("bard")) {
                        lastLocations.remove(player.getUniqueId());
                        pendingDistances.remove(player.getUniqueId());
                        toneTicks.remove(player.getUniqueId());
                        continue;
                    }

                    if (player.isDead()) continue;

                    UUID uuid = player.getUniqueId();

                    // 1. GIAO DIỆN ÂM TIẾT
                    if (!player.hasMetadata("Is_Combo_Casting")) {
                        java.util.List<vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType> currentTones = ToneManager.getCurrentTones(player);
                        if (!currentTones.isEmpty()) {
                            ToneManager.sendToneActionBar(player, currentTones);
                        } else {
                            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(" "));
                        }
                    }

                    // 2. CHỈ SỐ NỘI TẠI
                    org.bukkit.attribute.AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
                    if (speedAttr != null) {
                        double currentMs = (speedAttr.getValue() / 0.1) * 100.0;
                        double excessSpeed = Math.max(0.0, currentMs - 200.0);
                        int stacks = (int) Math.floor(excessSpeed / 10.0);

                        //player.setMetadata("BARD_BONUS_CDR", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), Math.min(40.0, stacks * 1.0)));
                        player.setMetadata("BARD_BONUS_DURATION", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), Math.min(80.0, stacks * 2.0)));
                    }

                    if (rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_5") > 0) {
                        player.setMetadata("BARD_OVERTONE_ACTIVE", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
                    }

                    // 3. ĐẾM KHOẢNG CÁCH VÀ KIỂM TRA MỐC
                    Location currentLoc = player.getLocation();
                    Location lastLoc = lastLocations.get(uuid);

                    if (lastLoc != null && currentLoc.getWorld().equals(lastLoc.getWorld())) {
                        double dist = currentLoc.distance(lastLoc);
                        if (dist > 0.1 && dist < 20.0) { // Chống cheat Teleport
                            double pending = pendingDistances.getOrDefault(uuid, 0.0) + dist;
                            if (pending >= 1.0) {
                                int blocks = (int) pending;
                                pending -= blocks;

                                int currentTotal = rpgPlayer.getSkillData().getSkillLevel("BARD_DISTANCE");
                                int newTotal = currentTotal + blocks;
                                rpgPlayer.getSkillData().setSkillLevel("BARD_DISTANCE", newTotal);

                                // GỌI HÀM CHECK MILESTONE ĐÃ ĐƯỢC LÀM LẠI
                                checkMilestones(player, rpgPlayer, newTotal);
                            }
                            pendingDistances.put(uuid, pending);
                        }
                    }
                    lastLocations.put(uuid, currentLoc.clone());

                    // ==========================================
                    // 4. SINH ÂM TỰ ĐỘNG (DÀNH RIÊNG TRẠNG THÁI THƯỜNG)
                    // ==========================================
                    // NẾU LÀ NHẠC TRƯỞNG: Đứng im, để MaestroState tự nhồi đạn!
                    if (player.hasMetadata(vn.minecraft.rPGCore.skills.bard.MaestroState.ACTIVE_META)) {
                        player.setMetadata("BARD_TONE_TIMER", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), 0));
                        continue; // Bỏ qua luồng này
                    }

                    boolean hasCore8 = rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_8") > 0;
                    int maxAutoGenLimit = hasCore8 ? 3 : 2; // Khóa lại 2/3 để chừa slot Combo

                    int ticks = player.hasMetadata("BARD_TONE_TIMER") ? player.getMetadata("BARD_TONE_TIMER").get(0).asInt() : 0;
                    ticks++;

                    if (ticks >= 30) { // Đủ 15s
                        ticks = 0;

                        // Sinh thêm âm nếu chưa chạm ngưỡng Auto-gen
                        if (ToneManager.getCurrentTones(player).size() < maxAutoGenLimit) {
                            vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType[] types = vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType.values();
                            ToneManager.addTone(player, rpgPlayer, types[new java.util.Random().nextInt(types.length)]);
                        }
                    }

                    player.setMetadata("BARD_TONE_TIMER", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), ticks));
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 20L, 10L);
    }

    // ==========================================
    // LOGIC CHECK MỐC ĐÃ ĐƯỢC TỐI ƯU
    // ==========================================
    private static void checkMilestones(Player player, RPGPlayer rpgPlayer, int distance) {
        vn.minecraft.rPGCore.player.data.PlayerSkillData data = rpgPlayer.getSkillData();

        // Vòng lặp check đủ 9 mốc (Index từ 0 đến 8)
        for (int i = 0; i < MILESTONES.length; i++) {
            // "BARD_MILESTONE_1" tương ứng với index 0
            String milestoneKey = "BARD_MILESTONE_" + (i + 1);

            // Nếu đã vượt mốc VÀ chưa từng nhận phần thưởng mốc này
            if (distance >= MILESTONES[i] && data.getSkillLevel(milestoneKey) == 0) {
                // Đánh dấu đã nhận
                data.setSkillLevel(milestoneKey, 1);
                // Kích hoạt nhận thưởng và truyền Danh Hiệu (Tên) vào
                triggerPerkSelection(player, rpgPlayer, TITLES[i]);
            }
        }
    }

    private static void triggerPerkSelection(Player player, RPGPlayer rpgPlayer, String title) {
        player.sendMessage("");
        player.sendMessage("§d§l[Hành Trình Nhạc Sư] §fDanh tiếng của bạn đã vang xa!");
        player.sendMessage("§fĐạt danh hiệu: §e§l" + title);
        player.sendMessage("§a+1 Điểm Nâng Cấp. Bấm phím §b[F] §ađể chọn nâng cấp!");
        player.sendMessage("");

        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        int unspent = rpgPlayer.getSkillData().getSkillLevel("BARD_UNSPENT_CORES");
        rpgPlayer.getSkillData().setSkillLevel("BARD_UNSPENT_CORES", unspent + 1);
    }
}