package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;
import vn.minecraft.rPGCore.skills.mage.blink.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlinkSkill implements ISkill {

    private final JavaPlugin plugin;

    public BlinkSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "mage_blink"; }
    @Override public String getName() { return "Luân Chuyển Nguyên Tố"; }
    @Override public String getRequiredClass() { return "mage"; }
    @Override public Material getIcon() { return Material.ENDER_PEARL; }

    @Override public int getMaxLevel() { return 6; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }

    @Override public int getRequiredPlayerLevel() { return 8; }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        Map<String, Integer> req = new HashMap<>();
        req.put("mage_focus", 6);
        return req;
    }

    @Override
    public double getManaCost(int level) {
        return 50.0 + ((level - 1) * 5.0);
    }

    @Override
    public long getCooldown(int level) {
        double cd = 16.0 - ((level - 1) * 0.4);
        return (long) (cd * 1000);
    }

    @Override
    public List<String> getDescription(int level) {
        return getDescription(null, level);
    }

    @Override
    public List<String> getDescription(Player player, int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        MageElement element = player != null ? ElementalSwapSkill.getActiveElement(player) : MageElement.FIRE;

        lore.add("§7Hòa mình vào sự luân chuyển của ma pháp");
        lore.add("§7để thích nghi với tình cảnh chiến đấu.");
        lore.add("");
        lore.add("§8▪ §fNguyên tố hiện tại: " + element.getDisplayName());
        lore.add("§8▪ §fVận sức: §e0.5s");
        lore.add("");

        switch (element) {
            case FIRE:
                int fireDmg = 150 + (displayLevel * 30);
                lore.add("§c[Thi Triển: Thiêu Thân]");
                lore.add("§f- Dịch chuyển lên phía trước.");
                lore.add("§f- Phát nổ gây §c" + fireDmg + "% Sát Thương Vũ khí §fcho kẻ");
                lore.add("§f  địch xung quanh trong phạm vi nhỏ.");
                break;
            case WATER:
                int waterTime = 3 + (displayLevel / 2);
                lore.add("§b[Thi Triển: Sương Mù]");
                lore.add("§f- Dịch chuyển lùi về phía sau.");
                lore.add("§f- Để lại bãi sương mù tại vị trí cũ");
                lore.add("§f  tồn tại trong §a" + waterTime + "s§f.");
                lore.add("§f- Kẻ địch đi vào sương bị §8Che Mắt§f.");
                break;
            case WIND:
                int windTime = 2 + (displayLevel / 2);
                lore.add("§a[Thi Triển: Vũ Điệu Gió]");
                lore.add("§f- Dịch chuyển giật lùi về phía sau trên không.");
                lore.add("§f- Nhận trạng thái lơ lửng trên không §a" + windTime + "s§f.");
                lore.add("§f- §oNhấn [Shift] để chủ động rơi xuống.");
                break;
            case NATURE:
                int natTime = 6 + (displayLevel / 2); // Cấp 1 = 6s, Cấp 6 = 9s
                int slowPct = 15 + (displayLevel * 5);
                lore.add("§2[Thi Triển: Hạt Giống Vĩnh Hằng]");
                lore.add("§f- Tạo Kết giới Sinh Mệnh bao bọc bản thân trong §a" + natTime + "s§f.");
                lore.add("§f- Đồng minh và Bản thân trong vùng: Hồi §a0.5% Máu/s§f.");
                lore.add("§f- Kẻ địch trong vùng: Giảm §e" + slowPct + "% §fTốc chạy.");
                break;
            case DARK:
                int darkTime = 3 + (displayLevel / 2);
                lore.add("§5[Thi Triển: Sương Đen]");
                lore.add("§f- Dịch chuyển lùi về phía sau.");
                lore.add("§f- Để lại bãi khói đen tại vị trí ban đầu");
                lore.add("§f  tồn tại trong §a" + darkTime + "s§f.");
                lore.add("§f- Kẻ địch đi vào sương sẽ bị §8Che Mắt§f");
                break;
            case LIGHT:
                lore.add("§e[Thi Triển: Bước Ánh Sáng]");
                lore.add("§f- Dịch chuyển lên phía trước khá xa.");
                lore.add("§f- Đặc biệt: Có thể tích trữ tối đa §e2 Lượt§f");
                lore.add("§f  dùng liên tiếp (Tiêu hao Mana mỗi lần thi triển).");
                break;
        }

        lore.add("");
        lore.add("§8§o* Kỹ năng này không gây Áp Nguyên");
        lore.add("§8§o  tố lên kẻ địch trúng chiêu.");

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        // Chặn click spam trong lúc đang vận sức 0.5s
        if (player.hasMetadata("Is_Blink_Casting")) return false;

        // 1. Kiểm tra CD & Trừ Mana (Chưa khóa Cooldown vội)
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;

        MageElement element = ElementalSwapSkill.getActiveElement(player);
        boolean isLight = (element == MageElement.LIGHT);
        boolean inLightComboWindow = player.hasMetadata("Light_Blink_Combo");

        // 2. LOGIC CỬA SỔ COMBO (COMBO WINDOW)
        if (isLight && !inLightComboWindow) {
            // ĐÂY LÀ LƯỚT ÁNH SÁNG LẦN 1 -> Đánh dấu mở cửa sổ Combo 3s, KHÔNG kích hoạt Hồi chiêu
            player.setMetadata("Light_Blink_Combo", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
            startLightComboTask(player, rpgPlayer, level);
        } else {
            // ĐÂY LÀ LƯỚT ÁNH SÁNG LẦN 2, HOẶC LÀ CÁC HỆ KHÁC -> Xóa Combo & Kích hoạt Hồi chiêu
            if (player.hasMetadata("Light_Blink_Combo")) {
                player.removeMetadata("Light_Blink_Combo", plugin);
            }
            applyGlobalCooldown(player, rpgPlayer, level);
        }

        // Khóa không cho bấm chiêu trong 0.5s vận sức
        player.setMetadata("Is_Blink_Casting", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

        // [VFX CHUNG TRƯỚC KHI LƯỚT]
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.5f);
        player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);

        // [DELAY 0.5 GIÂY = 10 TICKS]
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.hasMetadata("Is_Blink_Casting")) player.removeMetadata("Is_Blink_Casting", plugin);
                if (!player.isOnline() || player.isDead()) return;

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);

                switch (element) {
                    case FIRE: new BlinkFire().executeBlink(player, rpgPlayer, level); break;
                    case WATER: new BlinkWater().executeBlink(player, rpgPlayer, level); break;
                    case WIND: new BlinkWind().executeBlink(player, rpgPlayer, level); break;
                    case NATURE: new BlinkNature().executeBlink(player, rpgPlayer, level); break;
                    case DARK: new BlinkDark().executeBlink(player, rpgPlayer, level); break;
                    case LIGHT: new BlinkLight().executeBlink(player, rpgPlayer, level); break;
                }
            }
        }.runTaskLater(plugin, 10L);

        return true;
    }

    // ==========================================
    // TASK THEO DÕI COMBO 3 GIÂY & CHẶN ĐỔI HỆ (HỆ ÁNH SÁNG)
    // Dán hàm này vào cuối file BlinkSkill.java
    // ==========================================
    private void startLightComboTask(Player player, RPGPlayer rpgPlayer, int level) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                // Nếu người chơi mất mạng, hoặc đã lướt lần 2 (Meta bị xóa ở hàm execute)
                if (!player.isOnline() || !player.hasMetadata("Light_Blink_Combo")) {
                    this.cancel();
                    return;
                }

                // [TRỪNG PHẠT]: Nếu đổi nguyên tố trong lúc đang mở cửa sổ Combo 3s
                if (ElementalSwapSkill.getActiveElement(player) != MageElement.LIGHT) {
                    player.removeMetadata("Light_Blink_Combo", plugin);
                    applyGlobalCooldown(player, rpgPlayer, level); // Ép ăn Cooldown ngay lập tức
                    this.cancel();
                    return;
                }

                // Hết 3 giây (60 ticks) mà không bấm lướt lần 2 -> Ép ăn Cooldown
                if (ticks >= 60) {
                    player.removeMetadata("Light_Blink_Combo", plugin);
                    applyGlobalCooldown(player, rpgPlayer, level);
                    this.cancel();
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}