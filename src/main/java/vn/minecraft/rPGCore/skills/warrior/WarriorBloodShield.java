package vn.minecraft.rPGCore.skills.warrior;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.AbstractEnhancementSkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarriorBloodShield extends AbstractEnhancementSkill {

    public WarriorBloodShield(JavaPlugin plugin) {
        super(plugin);
    }

    @Override public String getId() { return "warrior_blood_shield"; }
    @Override public String getName() { return "§4§lHuyết Giáp Bạo Phá"; }
    @Override public String getRequiredClass() { return "warrior"; }
    @Override public Material getIcon() { return Material.CRIMSON_ROOTS; }

    // [CẬP NHẬT]: Nâng cấp tối đa 10 lần
    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }

    // [CẬP NHẬT]: Mở khóa ở giai đoạn Mid-game (Cấp 16)
    @Override public int getRequiredPlayerLevel() { return 16; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }
    @Override public boolean isPassiveSkill() { return false; }
    @Override public double getManaCost(int level) { return 40.0; } // Yêu cầu 40 Nộ
    @Override public long getCooldown(int level) { return 20000L; } // 20 Giây

    @Override
    public List<String> getDescription(int level) {
        // Cấp 1: 12%, Cấp 10: 30%
        double lifesteal = 12.0 + (level - 1) * 2.0;
        String lsStr = String.format("%.1f", lifesteal);

        return Arrays.asList(
                "§7Thuật cấm kị tích lũy huyết khí thành hộ giáp.",
                "",
                "§4[Kích hoạt: Cuồng Huyết]",
                "§f- Yêu cầu khởi động: §c40 Nộ khí§f.",
                "§f- Tiêu hao §c8 Nộ / giây§f để duy trì.",
                "§f- Tăng §c" + lsStr + "% Tỉ lệ hút máu§f.",
                "",
                "§e[Nội tại: Huyết Giáp]",
                "§f- Khi ngắt chiêu hoặc cạn Nộ: Chuyển hóa §6100%§f",
                "§f  lượng hút máu dư ra thành §eGiáp Ảo§f.",
                "§f- Giáp ảo tối đa bằng §e40% Máu tối đa§f.",
                "§f- Giáp ảo tồn tại trong §a18 giây§f, khi tồn tại",
                "§f  nhận thêm §c8% Tỉ lệ hút máu§f."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        // NẾU ĐANG BẬT -> TẮT CHIÊU
        if (player.hasMetadata("BloodShield_Active")) {
            List<org.bukkit.metadata.MetadataValue> values = player.getMetadata("BloodShield_Active");
            if (!values.isEmpty()) {
                player.sendMessage("§c§l[!] §fNgừng Cuồng Huyết!");
                deactivateSkill(player, rpgPlayer, level);
                return true;
            }
        }

        // NẾU ĐANG TẮT -> BẬT CHIÊU
        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) { // Yêu cầu có 40 Nộ, không trừ lúc bật
            player.sendMessage("§c§l[!] §fCần ít nhất " + (int)cost + " Nộ để khởi động Huyết Giáp!");
            return false;
        }

        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey) && System.currentTimeMillis() < player.getMetadata(cdKey).get(0).asLong()) {
            player.sendMessage("§cKỹ năng đang hồi chiêu!");
            return false;
        }

        // Bật cờ và ép Core cập nhật chỉ số Hút Máu ngay lập tức
        player.setMetadata("BloodShield_Active", new FixedMetadataValue(plugin, level));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.8f);
        rpgPlayer.updateMaxStats();

        // ==========================================
        // TASK 1: TIÊU HAO 8 NỘ MỖI GIÂY (20 Ticks)
        // ==========================================
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !player.hasMetadata("BloodShield_Active")) {
                    this.cancel();
                    return;
                }

                // Tiêu hao 8 Nộ mỗi giây
                if (rpgPlayer.getCurrentResource() >= 8.0) {
                    rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - 8.0);

                    // VFX: Hạt máu rơi tản ra
                    Particle.DustOptions red = new Particle.DustOptions(Color.RED, 1.2f);
                    player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 6, 0.3, 0.3, 0.3, 0, red);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_POINTED_DRIPSTONE_DRIP_LAVA, 1.0f, 0.5f);
                } else {
                    player.sendMessage("§c§l[!] §fĐã cạn Nộ, Huyết Giáp chuẩn bị bạo phá!");
                    deactivateSkill(player, rpgPlayer, level);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        // ==========================================
        // TASK 2: HẤP THỤ MÁU DƯ VÀO HUYẾT BỂ (1 Tick)
        // ==========================================
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !player.hasMetadata("BloodShield_Active")) {
                    this.cancel();
                    return;
                }

                if (player.hasMetadata("Lifesteal_Pool")) {
                    double pool = player.getMetadata("Lifesteal_Pool").get(0).asDouble();
                    if (pool > 0) {
                        double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                        double missingHp = maxHp - player.getHealth();

                        if (pool > missingHp) {
                            double overheal = pool - missingHp;

                            double currentAccumulated = player.hasMetadata("BloodShield_Accumulated_Pool") ?
                                    player.getMetadata("BloodShield_Accumulated_Pool").get(0).asDouble() : 0.0;

                            player.setMetadata("BloodShield_Accumulated_Pool",
                                    new FixedMetadataValue(plugin, currentAccumulated + overheal));

                            player.setMetadata("Lifesteal_Pool", new FixedMetadataValue(plugin, missingHp));

                            if (Math.random() > 0.7) {
                                Particle.DustOptions redDust = new Particle.DustOptions(org.bukkit.Color.RED, 0.8f);
                                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 3, 0.4, 0.4, 0.4, 0, redDust);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void deactivateSkill(Player player, RPGPlayer rpgPlayer, int level) {
        if (!player.hasMetadata("BloodShield_Active")) return;

        // 1. Lấy Huyết Bể (Chuyển hóa 100%)
        double accumulated = player.hasMetadata("BloodShield_Accumulated_Pool") ?
                player.getMetadata("BloodShield_Accumulated_Pool").get(0).asDouble() : 0.0;

        double finalShieldValue = accumulated * 1.0;

        player.removeMetadata("BloodShield_Active", plugin);
        player.removeMetadata("BloodShield_Accumulated_Pool", plugin);

        if (finalShieldValue > 0) {
            double maxHp = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();

            // 2. Cap giáp ảo tối đa bằng 40% Tổng máu
            double maxShieldCap = maxHp * 0.4;
            finalShieldValue = Math.min(finalShieldValue, maxShieldCap);

            long expireTime = System.currentTimeMillis() + (18 * 1000L); // 18 Giây

            // Lưu Metadata chuẩn để WarriorClass đọc được (giúp +8% Hút máu)
            player.setMetadata("RPG_Absorption_Shield", new FixedMetadataValue(plugin, finalShieldValue));
            player.setMetadata("RPG_Absorption_Shield_Expire", new FixedMetadataValue(plugin, expireTime));

            // Hiển thị tim vàng
            player.setAbsorptionAmount(finalShieldValue);

            player.sendMessage("§e§l[!] §fHuyết Bể chuyển hóa thành §6" + (int)finalShieldValue + " Giáp ảo §f(Nhận +8% Hút máu)!");
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1.2f, 0.8f);

            // Ép cập nhật Stats để nhận 8% Hút máu ngay
            rpgPlayer.updateMaxStats();

            // Task tự dọn dẹp sau 18s
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        // Check an toàn lỡ người chơi bật tắt chiêu liên tục
                        if(player.hasMetadata("RPG_Absorption_Shield_Expire") &&
                                System.currentTimeMillis() >= player.getMetadata("RPG_Absorption_Shield_Expire").get(0).asLong() - 100) {
                            player.removeMetadata("RPG_Absorption_Shield", plugin);
                            player.removeMetadata("RPG_Absorption_Shield_Expire", plugin);
                            player.setAbsorptionAmount(0);
                            rpgPlayer.updateMaxStats(); // Gỡ 8% buff
                            player.sendMessage("§8§o(Giáp ảo đã tan biến...)");
                        }
                    }
                }
            }.runTaskLater(plugin, 18 * 20L);
        } else {
            rpgPlayer.updateMaxStats();
        }

        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata("CD_" + getId(), new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
    }

    private void detonateShield(Player player, RPGPlayer rpgPlayer, double shieldAmount) {
        // Xóa hoàn toàn Universal Shield
        player.removeMetadata("RPG_Absorption_Shield", plugin);
        player.removeMetadata("RPG_Absorption_Shield_Max", plugin);
        player.setAbsorptionAmount(0);

        Location center = player.getLocation();

        // VFX: Vụ nổ máu lớn (DUST đỏ đậm)
        Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 2.5f);
        center.getWorld().spawnParticle(Particle.DUST, center.add(0, 1, 0), 60, 2.0, 0.5, 2.0, 0, red);
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 1, 0, 0, 0, 0);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        // Gây sát thương vùng 4x4 (Bán kính 4)
        double explosionDamage = shieldAmount * 2.5; // Nổ 250% lượng giáp còn lại

        for (Entity e : center.getWorld().getNearbyEntities(center, 4, 3, 4)) {
            if (e instanceof LivingEntity && e != player && CombatManager.canTarget(player, e)) {
                CombatManager.applySkillDamage(player, (LivingEntity) e, explosionDamage);
            }
        }
    }
}