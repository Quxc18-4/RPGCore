package vn.minecraft.rPGCore.classes.impl.bard;

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

    // KHOÁ CHỐNG LAG KHI DÙNG LỆNH /RELOAD
    private static boolean isRunning = false;

    private static final Map<UUID, Location> lastLocations = new HashMap<>();
    private static final Map<UUID, Double> pendingDistances = new HashMap<>();
    private static final Map<UUID, Integer> toneTicks = new HashMap<>();

    public static void startEngine() {
        if (isRunning) return; // Nếu đã chạy rồi thì khóa lại, chống nhân bản Task
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

                    // ==========================================
                    // 1. HIỂN THỊ ACTION BAR (UI) THÔNG MINH
                    // ==========================================
                    if (!player.hasMetadata("Is_Combo_Casting")) {
                        java.util.List<vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType> currentTones = ToneManager.getCurrentTones(player);
                        if (!currentTones.isEmpty()) {
                            ToneManager.sendToneActionBar(player, currentTones);
                        } else {
                            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(" "));
                        }
                    }

                    // ==========================================
                    // 2. NỘI TẠI: QUY ĐỔI TỐC ĐỘ (SIÊU TỐI ƯU CPU)
                    // Lấy trực tiếp từ RAM, phớt lờ hoàn toàn mớ logic getTotalStatsMap nặng nề
                    // ==========================================
                    org.bukkit.attribute.AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
                    if (speedAttr != null) {
                        double currentMs = (speedAttr.getValue() / 0.1) * 100.0;
                        double excessSpeed = Math.max(0.0, currentMs - 200.0);
                        int stacks = (int) Math.floor(excessSpeed / 10.0);

                        player.setMetadata("BARD_BONUS_CDR", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), Math.min(40.0, stacks * 1.0)));
                        player.setMetadata("BARD_BONUS_DURATION", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), Math.min(80.0, stacks * 2.0)));
                    }

                    if (rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_5") > 0) {
                        player.setMetadata("BARD_OVERTONE_ACTIVE", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
                    }

                    // ==========================================
                    // 3. ĐẾM KHOẢNG CÁCH (NERF MỐC MỚI)
                    // ==========================================
                    Location currentLoc = player.getLocation();
                    Location lastLoc = lastLocations.get(uuid);

                    if (lastLoc != null && currentLoc.getWorld().equals(lastLoc.getWorld())) {
                        double dist = currentLoc.distance(lastLoc);
                        if (dist > 0.1 && dist < 20.0) {
                            double pending = pendingDistances.getOrDefault(uuid, 0.0) + dist;
                            if (pending >= 1.0) {
                                int blocks = (int) pending;
                                pending -= blocks;

                                int currentTotal = rpgPlayer.getSkillData().getSkillLevel("BARD_DISTANCE");
                                int newTotal = currentTotal + blocks;
                                rpgPlayer.getSkillData().setSkillLevel("BARD_DISTANCE", newTotal);

                                checkMilestones(player, rpgPlayer, newTotal);
                            }
                            pendingDistances.put(uuid, pending);
                        }
                    }
                    lastLocations.put(uuid, currentLoc.clone());

                    // ==========================================
                    // 4. SINH ÂM TỰ ĐỘNG (NERF LÊN 15 GIÂY)
                    // ==========================================
                    // 15 giây = 30 chu kỳ (vì Task chạy 10 ticks = 0.5s/lần)
                    int ticks = player.hasMetadata("BARD_TONE_TIMER") ? player.getMetadata("BARD_TONE_TIMER").get(0).asInt() : 0;
                    ticks++;
                    if (ticks >= 30) {
                        ticks = 0;
                        int maxTones = rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_8") > 0 ? 3 : 2;
                        if (ToneManager.getCurrentTones(player).size() < maxTones) {
                            vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType[] types = vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType.values();
                            ToneManager.addTone(player, rpgPlayer, types[new java.util.Random().nextInt(types.length)]);
                        }
                    }
                    player.setMetadata("BARD_TONE_TIMER", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), ticks));
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 20L, 10L); // 10 Ticks = 0.5s
    }

    private static void checkMilestones(Player player, RPGPlayer rpgPlayer, int distance) {
        vn.minecraft.rPGCore.player.data.PlayerSkillData data = rpgPlayer.getSkillData();
        if (distance >= 5000 && data.getSkillLevel("BARD_MILESTONE_1") == 0) {
            data.setSkillLevel("BARD_MILESTONE_1", 1); triggerPerkSelection(player, rpgPlayer, 1);
        }
        if (distance >= 50000 && data.getSkillLevel("BARD_MILESTONE_2") == 0) {
            data.setSkillLevel("BARD_MILESTONE_2", 1); triggerPerkSelection(player, rpgPlayer, 2);
        }
        if (distance >= 200000 && data.getSkillLevel("BARD_MILESTONE_3") == 0) {
            data.setSkillLevel("BARD_MILESTONE_3", 1); triggerPerkSelection(player, rpgPlayer, 3);
        }
    }

    private static void triggerPerkSelection(Player player, RPGPlayer rpgPlayer, int tier) {
        player.sendMessage("§d§l[Hành Trình Nhạc Sư] §fĐã đạt mốc §eTier " + tier + "§f!");
        player.sendMessage("§aBạn có 1 Điểm Lõi Nâng Cấp. Bấm phím §b[F]§a để mở Menu Lõi!");
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        int unspent = rpgPlayer.getSkillData().getSkillLevel("BARD_UNSPENT_CORES");
        rpgPlayer.getSkillData().setSkillLevel("BARD_UNSPENT_CORES", unspent + 1);
    }
}