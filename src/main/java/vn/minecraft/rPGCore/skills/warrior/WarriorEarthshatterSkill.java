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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarriorEarthshatterSkill implements ISkill {

    @Override public String getId() { return "warrior_earthshatter"; }
    @Override public String getName() { return "§6§lĐại Địa Chấn"; }
    @Override public String getRequiredClass() { return "warrior"; }
    @Override public Material getIcon() { return Material.ANVIL; }

    @Override public int getMaxLevel() { return 6; } // Đã nâng lên 6 cấp
    @Override public int getUpgradeCost(int currentLevel) { return 2; }
    @Override public int getRequiredPlayerLevel() { return 5; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override public boolean isPassiveSkill() { return false; }

    @Override public double getManaCost(int level) { return 45; } // Tiêu hao 45 Nộ

    // Giảm dần 1 giây mỗi cấp (Cấp 1 = 14s, Cấp 6 = 9s)
    @Override public long getCooldown(int level) { return 15000L - (level * 1000L); }

    @Override
    public List<String> getDescription(int level) {
        int normalDmg = (int) ((1.0 + (level * 0.25)) * 100);
        int awakenDmg = (int) ((2.0 + (level * 0.6)) * 100);

        return Arrays.asList(
                "§7Tuyệt kỹ dọn dẹp chiến trường của Chiến Binh.",
                "§7Dùng sức mạnh cơ bắp nghiền nát mặt đất.",
                "",
                "§e[Chủ động: Gán phím R-L-R]",
                "§f- Dậm mạnh xuống đất, gây §e" + normalDmg + "% ST Vật lý",
                "§f  lên mọi kẻ địch xung quanh (Bán kính 3m).",
                "",
                "§c[Thức Tỉnh Kỹ Năng]",
                "§f- Nhảy vọt lên và giáng đòn hủy diệt.",
                "§f- Sát thương tăng vọt lên §c" + awakenDmg + "% ST Vật lý§f.",
                "§f- Kẻ địch bị §6Hất tung§f và §8Choáng§f trong 1.5s."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        // 1. Kiểm tra Cooldown
        if (player.hasMetadata("CD_" + getId())) {
            long cdEnd = player.getMetadata("CD_" + getId()).get(0).asLong();
            if (System.currentTimeMillis() < cdEnd) {
                player.sendMessage("§cKỹ năng đang hồi chiêu!");
                return false;
            }
        }

        double cost = getManaCost(level);
        boolean isEmpowered = rpgPlayer.getCurrentResource() >= cost;

        // 2. Phân nhánh Kỹ năng (Soft Block)
        if (isEmpowered) {
            // [CÓ NỘ] -> Nhảy lên
            rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
            player.sendMessage("§c§l[THỨC TỈNH] §fĐại Địa Chấn!");

            Vector leapDir = player.getLocation().getDirection().setY(0).normalize().multiply(0.5).setY(0.7);
            player.setVelocity(leapDir);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);

            player.setMetadata("Is_Casting", new FixedMetadataValue(RPGCore.getInstance(), true));

            // Chờ chạm đất để kích nổ bản Thức Tỉnh
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    ticks++;
                    if (ticks > 40 || !player.isOnline() || player.isDead()) {
                        player.removeMetadata("Is_Casting", RPGCore.getInstance());
                        this.cancel();
                        return;
                    }
                    if (ticks > 5 && ((Entity) player).isOnGround()) {
                        player.removeMetadata("Is_Casting", RPGCore.getInstance());
                        triggerEarthshatter(player, rpgPlayer, level, true);
                        this.cancel();
                    }
                }
            }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);

        } else {
            // [KHÔNG ĐỦ NỘ] -> Đứng im dậm ngay lập tức
            triggerEarthshatter(player, rpgPlayer, level, false);
        }

        // 3. Cài đặt Cooldown
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata("CD_" + getId(), new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        return true;
    }

    // ==========================================
    // LOGIC KÍCH NỔ MẶT ĐẤT
    // ==========================================
    private void triggerEarthshatter(Player player, RPGPlayer rpgPlayer, int level, boolean isEmpowered) {
        Location hitLoc = player.getLocation();

        // 1. SFX & VFX tùy theo trạng thái
        if (isEmpowered) {
            player.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
            player.getWorld().playSound(hitLoc, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.5f);
            player.getWorld().spawnParticle(Particle.EXPLOSION, hitLoc.add(0, 0.5, 0), 2, 0.5, 0.5, 0.5, 0);
            player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, hitLoc, 30, 2.0, 0.2, 2.0, 0.05);
        } else {
            player.getWorld().playSound(hitLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.5f);
            player.getWorld().spawnParticle(Particle.EXPLOSION, hitLoc.add(0, 0.5, 0), 10, 1.0, 0.1, 1.0, 0.1);
        }

        // Vẽ vòng tròn địa chấn (Đỏ nếu Thức Tỉnh, Trắng nếu Thường)
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
            Location pLoc = hitLoc.clone().add(Math.cos(angle) * 3.0, 0.1, Math.sin(angle) * 3.0);
            if (isEmpowered) {
                Particle.DustOptions redDust = new Particle.DustOptions(org.bukkit.Color.RED, 1.5f);
                player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, redDust);
            } else {
                player.getWorld().spawnParticle(Particle.CRIT, pLoc, 1, 0, 0, 0, 0);
            }
        }

        // 2. HITBOX: Bán kính cố định 3.0 block
        double multiplier = isEmpowered ? (2.0 + (level * 0.6)) : (1.0 + (level * 0.25));

        for (Entity e : hitLoc.getWorld().getNearbyEntities(hitLoc, 3.0, 3.0, 3.0)) {
            if (e instanceof LivingEntity && !(e instanceof ArmorStand) && CombatManager.canTarget(player, e)) {
                LivingEntity target = (LivingEntity) e;

                // Tắt hút máu AOE để tránh hồi full bình
                double dmg = CombatManager.calculateDamage(player, rpgPlayer, target, multiplier, 0.0, false, false);

                player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                CombatManager.applySkillDamage(player, target, dmg);
                player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                // 3. HIỆU ỨNG KHỐNG CHẾ (Chỉ áp dụng khi Thức Tỉnh)
                if (isEmpowered) {
                    // Hất tung (Knock up)
                    target.setVelocity(new Vector(0, 0.8, 0));

                    // Làm choáng cứng 1.5s = 30 ticks (Dùng SLOWNESS cường độ cực cao để khóa di chuyển)
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 10, false, false));
                }
            }
        }
    }
}