package vn.minecraft.rPGCore.skills.archer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class QuadrupleShotSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public QuadrupleShotSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "archer_quad_shot"; }
    @Override public String getName() { return "Tứ Tiễn"; }
    @Override public String getRequiredClass() { return "archer"; }
    @Override public Material getIcon() { return Material.CROSSBOW; }
    @Override public int getMaxLevel() { return 6; } // Cấp tối đa là 6 (Đột phá)

    @Override
    public double getManaCost(int level) {
        double cost = 60.0;
        if (level >= 3) cost += 10.0;
        if (level >= 5) cost += 10.0;
        if (level == 6) cost += 20.0; // Đột phá
        return cost;
    }

    @Override
    public long getCooldown(int level) {
        double cd = 15.8;
        if (level >= 3) cd -= 1.4;
        if (level >= 5) cd -= 1.4;
        if (level == 6) cd -= 1.0; // Đột phá
        return (long) (cd * 1000);
    }

    @Override
    public List<String> getDescription(int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        lore.add("§7Bắn liên tiếp 4 đợt, mỗi đợt 4 mũi tên.");

        double dmg = 25 + (displayLevel - 1) * 8;
        double pen = 35 + (displayLevel - 1) * 2;

        lore.add("§7Sát thương: §c" + (int) dmg + "% §7mỗi mũi");
        lore.add("§7Tỉ lệ xuyên giáp: §e" + (int) pen + "%");

        if (level < 6) {
            lore.add("");
            lore.add("§8§o(Kỹ năng đột phá ở cấp 3, 5)");
            lore.add("§c§l(!) §cĐột phá Tối thượng (Cấp 6): Cần 2 SP");
        } else {
            lore.add("");
            lore.add("§6§l✦ ĐỘT PHÁ TỐI THƯỢNG ✦");
            lore.add("§e22% xác suất mỗi mũi tên mang 1 trong 4");
            lore.add("§ehiệu ứng: §bBăng§e, §eSét§e, §cLửa§e, §2Độc§e.");
        }
        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Mana!");
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);

        long baseCd = getCooldown(level); // Lấy thời gian gốc
        long finalCd = getFinalCooldown(rpgPlayer, baseCd); // Đưa qua máy ép CDR

        if (currentTime - lastUsed < finalCd) {
            double timeLeft = (finalCd - (currentTime - lastUsed)) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);

        startCooldownNotifier(player, finalCd);

        player.sendMessage("§a§l[!] TỨ TIỄN!");

        // 4 Đợt bắn: 0, 5, 10, 15 tick
        shootQuadArrows(player, level);
        new BukkitRunnable() { @Override public void run() { shootQuadArrows(player, level); } }.runTaskLater(plugin, 5L);
        new BukkitRunnable() { @Override public void run() { shootQuadArrows(player, level); } }.runTaskLater(plugin, 10L);
        new BukkitRunnable() { @Override public void run() { shootQuadArrows(player, level); } }.runTaskLater(plugin, 15L);

        return true;
    }

    private void shootQuadArrows(Player player, int skillLevel) {
        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());

        if (right.lengthSquared() > 0) right.normalize();
        else right = new Vector(1, 0, 0);

        // [ĐÃ SỬA]: Bóp hẹp khoảng cách mũi tên để dồn sát thương mục tiêu đơn (Hitbox 0.6)
        // 4 Vị trí cách tâm: -0.2, -0.06, 0.06, 0.2 (Tổng chiều rộng chỉ 0.4 block)
        double[] offsets = {-0.2, -0.06, 0.06, 0.2};

        for (double offset : offsets) {
            Location spawnLoc = eyeLoc.clone().add(right.clone().multiply(offset));
            fireSkillArrow(player, spawnLoc, dir.clone(), skillLevel);
        }
        player.playSound(eyeLoc, org.bukkit.Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.0f);
    }

    private void fireSkillArrow(Player player, Location spawnLoc, Vector velocity, int skillLevel) {
        Arrow arrow = player.getWorld().spawnArrow(spawnLoc, velocity, 3.5f, 0f);
        arrow.setShooter(player);

        double dmgMultiplier = 0.25 + (skillLevel - 1) * 0.08;
        double ignoreChance = 0.35 + (skillLevel - 1) * 0.02;

        arrow.setMetadata("Is_Custom_Skill", new FixedMetadataValue(plugin, true));
        arrow.setMetadata("Skill_DmgMulti", new FixedMetadataValue(plugin, dmgMultiplier));
        arrow.setMetadata("Skill_ArmorPen", new FixedMetadataValue(plugin, ignoreChance));

        // [TÍCH HỢP XUYÊN THẤU]: Bơm thẳng mác Xuyên thấu vào đạn kỹ năng
        if (player.hasMetadata("Archer_Pierce_Buff")) {
            arrow.setMetadata("Is_Piercing_Arrow", new FixedMetadataValue(plugin, true));
            arrow.setMetadata("Pierce_Hit_Count", new FixedMetadataValue(plugin, 0));
            arrow.setMetadata("Pierced_Entities", new FixedMetadataValue(plugin, new java.util.ArrayList<java.util.UUID>()));
        }

        // TẠO HIỆU ỨNG NGẪU NHIÊN NẾU MAX LEVEL (6)
        String effect = "NONE";
        if (skillLevel == 6 && Math.random() <= 0.22) {
            int r = new Random().nextInt(4);
            if (r == 0) effect = "FREEZE";
            else if (r == 1) effect = "LIGHTNING";
            else if (r == 2) effect = "FLAME";
            else if (r == 3) effect = "POISON";
        }
        arrow.setMetadata("Quad_Effect", new FixedMetadataValue(plugin, effect));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isOnGround()) {
                    arrow.remove();
                    this.cancel();
                    return;
                }
                arrow.getWorld().spawnParticle(Particle.CRIT, arrow.getLocation(), 1, 0.0, 0.0, 0.0, 0.0);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    @Override
    public int getUpgradeCost(int currentLevel) {
        // Nếu đang ở cấp 5 (chuẩn bị lên 6) thì tốn 2 SP, còn lại tốn 1 SP
        return (currentLevel == 5) ? 2 : 1;
    }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        return Collections.singletonMap("archer_triple_shot", 10); // Cần Tam Tiễn lv 10
    }
}