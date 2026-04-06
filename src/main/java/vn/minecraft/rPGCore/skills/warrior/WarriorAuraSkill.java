package vn.minecraft.rPGCore.skills.warrior;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.AbstractHitCounterSkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WarriorAuraSkill extends AbstractHitCounterSkill {

    @Override public String getId() { return "warrior_aura"; }
    @Override public String getName() { return "§c§lKiếm Khí"; }
    @Override public String getRequiredClass() { return "warrior"; }
    @Override public Material getIcon() { return Material.IRON_SWORD; }

    // [KHAI BÁO]: Đây là kỹ năng Nội Tại!
    @Override public boolean isPassiveSkill() { return true; }

    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override public double getManaCost(int level) { return 30; }
    @Override public long getCooldown(int level) { return 0; }

    @Override protected int getRequiredHits() { return 4; }
    @Override protected long getComboTimeout() { return 2500L; }

    @Override
    public java.util.List<String> getDescription(int level) {
        double normalDmg = (0.5 + (level * 0.05)) * 100;
        double empowerDmg = (1.4 + (level * 0.1)) * 100;

        return Arrays.asList(
                "§7Đỉnh cao kỹ thuật của Chiến Binh, vết chém xuyên thấu.",
                "",
                "§e[Kích hoạt: Chuỗi Combo]",
                "§f- Đạt §a4 điểm Combo§f, quét một luồng Kiếm Khí.",
                "§f  gây §e" + (int)normalDmg + "% Sát thương vật lý§f.",
                "",
                "§c[Thức Tỉnh Kỹ Năng]",
                "§f- Có thể §bBật/Tắt §fviệc tự động Thức Tỉnh.",
                "§f- Kiếm Khí hóa Đỏ, quét xa hơn (7 block).",
                "§f  + Gây §c" + (int)empowerDmg + "% Sát thương chuẩn§f.",
                "§f  + Hất tung mọi kẻ địch trong vùng quét."
        );
    }

    @Override
    protected void onMaxStacksReached(Player player, RPGPlayer rpgPlayer, LivingEntity lastTarget, int skillLevel) {

        // ==========================================
        // KIỂM TRA TOGGLE (ON/OFF)
        // Nếu Tắt -> Bỏ qua check Nộ -> Chỉ đánh ra bản Thường
        // ==========================================
        boolean isOff = "OFF".equals(rpgPlayer.getSkillData().getBinds().get("TOGGLE_" + getId()));
        boolean isEmpowered = !isOff && rpgPlayer.getCurrentResource() >= 30;

        if (isEmpowered) {
            rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - 30);
            player.sendMessage("§c§l[THỨC TỈNH] §fKiếm Khí!");
        }

        Location startLoc = player.getLocation().add(0, 1.2, 0);
        Vector direction = startLoc.getDirection().setY(0).normalize();

        player.getWorld().playSound(startLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, isEmpowered ? 0.5f : 1.2f);
        if (isEmpowered) player.getWorld().playSound(startLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);

        int maxDist = isEmpowered ? 7 : 5;

        new BukkitRunnable() {
            // Bắt đầu từ 1.0 để tránh nổ mù mắt người chơi
            double distance = 1.0;

            @Override
            public void run() {
                if (distance > maxDist) { this.cancel(); return; }

                Location currentLoc = startLoc.clone().add(direction.clone().multiply(distance));
                Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();

                // ==========================================
                // VẼ VIỀN NĂNG LƯỢNG CHO BẢN THỨC TỈNH (EMPOWERED)
                // TOÁN HỌC: Hình Nón Cụt (Truncated Cone) CỐ ĐỊNH, MẬT ĐỘ ÉP DÀY ĐẶC
                // ==========================================
                if (isEmpowered) {
                    // 1. Định hình độ rộng của Nón cụt (Truncated Cone boundary)
                    double minWidth = 1.0;
                    double maxWidth = 2.5; // Càng xa càng to
                    // Độ rộng widthAtDistance = Mở rộng dần theo quãng đường
                    double widthAtDistance = Math.max(minWidth, (distance / maxDist) * maxWidth);

                    // 2. ÉP ĐỘ RỘNG CỐ ĐỊNH 1.5 BLOCK ĐỂ TẠO 1 LƯỠI KIẾM DUY NHẤT
                    // (Chúng ta chỉ dùng độ rộng widthAtDistance làm "khung chặn" cho Hitbox thôi)
                    double fixedBladeWidth = 1.5;

                    // 3. VÒNG LẶP VẼ LƯỠI KIẾM BÁN NGUYỆT SẮC LẸM
                    // Khoảng cách w += 0.1 (tăng gấp 4 lần mật độ) giúp các hạt dính chặt vào nhau
                    for (double w = -fixedBladeWidth; w <= fixedBladeWidth; w += 0.1) {

                        // TOÁN HỌC: Vuốt cong mạnh hai mép lùi về sau (Math.abs(w) * 0.4)
                        // Hai đầu sẽ cong về sau giống hiệu ứng motion blur của nhát chém
                        double curveBack = Math.abs(w) * 0.4;

                        Location pLoc = currentLoc.clone()
                                .add(right.clone().multiply(w))
                                .subtract(direction.clone().multiply(curveBack));

                        Particle.DustOptions redDust = new Particle.DustOptions(org.bukkit.Color.RED, 1.2f); // Hạt nhỏ sắc nét
                        // Offset = 0 tuyệt đối để viền kiếm không bị run/nhiễu
                        player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, redDust);
                    }
                    // Thêm 1 hạt Lava ở tâm kiếm làm lõi bùng cháy
                    player.getWorld().spawnParticle(Particle.LAVA, currentLoc, 1, 0, 0, 0, 0);

                } else {
                    // Bản thường dùng vệt Crit lướt đi lả tả
                    player.getWorld().spawnParticle(Particle.CRIT, currentLoc, 1, 0, 0, 0, 0);
                    player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, currentLoc, 1, 0, 0, 0, 0);
                }

                // ==========================================
                // HITBOX: Hình Nón Cụt Mở Rộng dần theo distance
                // widthAtDistance được tính ở trên đảm bảo quái ở xa vẫn bị chém trúng góc rộng
                // ==========================================
                double hitboxWidth = Math.max(1.0, (distance / maxDist) * 2.5);
                for (Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, hitboxWidth, 1.5, hitboxWidth)) {
                    if (e instanceof LivingEntity && !(e instanceof ArmorStand) && CombatManager.canTarget(player, e)) {
                        LivingEntity target = (LivingEntity) e;

                        if (!target.hasMetadata("Aura_Hit_" + player.getUniqueId())) {
                            target.setMetadata("Aura_Hit_" + player.getUniqueId(), new FixedMetadataValue(plugin, true));

                            double multi = isEmpowered ? (1.4 + (skillLevel * 0.1)) : (0.5 + (skillLevel * 0.05));
                            double extraPen = isEmpowered ? 20.0 : 0.0;
                            double auraDmg = CombatManager.calculateDamage(player, rpgPlayer, target, multi, extraPen, false, false);

                            player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                            CombatManager.applySkillDamage(player, target, auraDmg);
                            player.removeMetadata("Ghost_L_Blocker", plugin);

                            if (isEmpowered) target.setVelocity(new Vector(0, 0.5, 0));

                            new BukkitRunnable() {
                                @Override public void run() { target.removeMetadata("Aura_Hit_" + player.getUniqueId(), plugin); }
                            }.runTaskLater(plugin, 5L);
                        }
                    }
                }
                // TỐC ĐỘ BAY ANIME: 2.5 Block/Tick
                distance += 2.5;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}