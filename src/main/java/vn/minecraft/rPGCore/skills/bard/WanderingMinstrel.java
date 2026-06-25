package vn.minecraft.rPGCore.skills.bard;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class WanderingMinstrel implements ISkill {

    private final String SKILL_ID = "bard_wandering_minstrel";
    public static final String META_TRACKED_DISTANCE = "BARD_DISTANCE";

    private static final String META_MILESTONE_5K = "BARD_MILESTONE_5K";
    private static final String META_MILESTONE_15K = "BARD_MILESTONE_15K";
    private static final String META_MILESTONE_30K = "BARD_MILESTONE_30K";

    @Override public String getId() { return SKILL_ID; }
    @Override public String getName() { return "Bước Chân Lãng Du"; }
    @Override public String getRequiredClass() { return "bard"; }
    @Override public Material getIcon() { return Material.LEATHER_BOOTS; }

    @Override public int getMaxLevel() { return 1; }

    // [YÊU CẦU GAME DESIGN]: Tốn 2 SP và yêu cầu Cấp độ 22
    @Override public int getUpgradeCost(int currentLevel) { return 2; }
    @Override public int getRequiredPlayerLevel() { return 22; }

    @Override public double getManaCost(int level) { return 0.0; }
    @Override public long getCooldown(int level) { return 0L; }
    @Override public boolean isPassiveSkill() { return true; }

    @Override public Map<String, Integer> getRequiredSkills() {
        // Yêu cầu phải học Kỹ năng nền tảng trước
        return Map.of("bard_symphonic_lexicon", 1);
    }

    @Override
    public List<String> getDescription(int level) {
        return Arrays.asList(
                "§7Kỹ năng đột phá dành cho Nhạc Sư kỳ cựu.",
                "§7Sự vất vả trên hành trình được đền đáp.",
                "",
                "§e[Nội Tại - Giai Điệu Ngân Vang]",
                "§f- Bắt đầu theo dõi và tích lũy Khoảng cách di chuyển.",
                "§f- Khi đạt mốc, nhận được 1 §dĐiểm Lõi Nâng Cấp§f.",
                "§f- Mở khóa Giao diện chọn Lõi (Tối đa 3 Lõi/nhân vật).",
                "",
                "§a[Các Mốc Hành Trình]",
                "§f- Mốc 1: §65.000§f Blocks.",
                "§f- Mốc 2: §615.000§f Blocks.",
                "§f- Mốc 3: §630.000§f Blocks."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false; // Chưa học thì không chạy Task!

        String activeKey = "PASSIVE_TRACKER_" + SKILL_ID;
        if (player.hasMetadata(activeKey)) return false;

        player.setMetadata(activeKey, new FixedMetadataValue(RPGCore.getInstance(), true));

        if (!player.hasMetadata(META_TRACKED_DISTANCE)) {
            player.setMetadata(META_TRACKED_DISTANCE, new FixedMetadataValue(RPGCore.getInstance(), 0.0));
        }

        new BukkitRunnable() {
            Location lastLocation = player.getLocation();

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || rpgPlayer.getCurrentClass() == null || !rpgPlayer.getCurrentClass().getId().equalsIgnoreCase("bard")) {
                    player.removeMetadata(activeKey, RPGCore.getInstance());
                    this.cancel();
                    return;
                }

                Location currentLocation = player.getLocation();
                if (currentLocation.getWorld().equals(lastLocation.getWorld())) {
                    double distanceMoved = lastLocation.distance(currentLocation);

                    // Thuật toán đo khoảng cách an toàn (Chống Teleport)
                    if (distanceMoved > 0.1 && distanceMoved < 20.0) {
                        double currentTotalDist = player.getMetadata(META_TRACKED_DISTANCE).get(0).asDouble();
                        double newTotalDist = currentTotalDist + distanceMoved;

                        player.setMetadata(META_TRACKED_DISTANCE, new FixedMetadataValue(RPGCore.getInstance(), newTotalDist));
                        checkDistanceMilestones(player, newTotalDist);
                    }
                }
                lastLocation = currentLocation.clone();
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 20L);

        return true;
    }

    private void checkDistanceMilestones(Player player, double distance) {
        if (distance >= 5000.0 && !player.hasMetadata(META_MILESTONE_5K)) {
            player.setMetadata(META_MILESTONE_5K, new FixedMetadataValue(RPGCore.getInstance(), true));
            triggerPerkSelection(player, 1);
        }
        if (distance >= 15000.0 && !player.hasMetadata(META_MILESTONE_15K)) {
            player.setMetadata(META_MILESTONE_15K, new FixedMetadataValue(RPGCore.getInstance(), true));
            triggerPerkSelection(player, 2);
        }
        if (distance >= 30000.0 && !player.hasMetadata(META_MILESTONE_30K)) {
            player.setMetadata(META_MILESTONE_30K, new FixedMetadataValue(RPGCore.getInstance(), true));
            triggerPerkSelection(player, 3);
        }
    }

    private void triggerPerkSelection(Player player, int milestoneTier) {
        player.sendMessage("§d§l[Bước Chân Lãng Du] §fKhoảng cách di chuyển đã đạt mốc §eTier " + milestoneTier + "§f!");
        player.sendMessage("§aBạn có 1 Điểm Lõi Nâng Cấp chưa sử dụng. Mở Menu Kỹ năng để chọn!");
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        int unspent = player.hasMetadata("BARD_UNSPENT_CORES") ? player.getMetadata("BARD_UNSPENT_CORES").get(0).asInt() : 0;
        player.setMetadata("BARD_UNSPENT_CORES", new FixedMetadataValue(RPGCore.getInstance(), unspent + 1));
    }
}