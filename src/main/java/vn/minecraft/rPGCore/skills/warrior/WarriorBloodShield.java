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
import org.bukkit.util.Vector;
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
    @Override public String getName() { return "§4§lCuồng Huyết"; }
    @Override public String getRequiredClass() { return "warrior"; }
    @Override public Material getIcon() { return Material.CRIMSON_ROOTS; }

    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public int getRequiredPlayerLevel() { return 16; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }
    @Override public boolean isPassiveSkill() { return false; }
    @Override public double getManaCost(int level) { return 40.0; } // Yêu cầu 40 Nộ
    @Override public long getCooldown(int level) { return 20000L; } // 20 Giây

    @Override
    public List<String> getDescription(int level) {
        // Hút máu: Cấp 1 = 12%, tăng 2% mỗi cấp
        double lifesteal = 12.0 + (level - 1) * 2.0;
        String lsStr = String.format("%.1f", lifesteal);

        // Sát thương nổ: Cấp 1 = 420%, tăng 30% mỗi cấp
        int explosionDmg = 420 + (level - 1) * 30;

        return Arrays.asList(
                "§7Thuật cấm kỵ tích lũy huyết khí thành hộ giáp.",
                "",
                "§4[Kích hoạt: Cuồng Huyết]",
                "§f- Yêu cầu khởi động: §c40 Nộ khí§f.",
                "§f- Tiêu hao §c15 Nộ / giây§f để duy trì.",
                "§f- Tăng §c" + lsStr + "% Tỉ lệ hút máu§f.",
                "",
                "§e[Nội tại: Huyết Giáp & Bạo Phá]",
                "§f- Khi ngắt chiêu: Chuyển hóa §6100%§f lượng máu dư",
                "§f  thành §eGiáp Ảo (Tối đa 160% Máu tối đa)§f.",
                "§f- Giáp ảo tồn tại §a18 giây§f, nhận thêm §c8% Hút máu§f.",
                "",
                "§c[Thức Tỉnh Kỹ Năng]",
                "§f- Nếu Giáp Ảo bị phá vỡ hoàn toàn bởi sát thương",
                "§f  trước khi hết hạn, nó sẽ phát nổ, gây lượng Sát",
                "§f  thương vùng 4x4 bằng §c" + explosionDmg + "% ST Vũ khí§f."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        // TẮT CHIÊU
        if (player.hasMetadata("BloodShield_Active")) {
            List<org.bukkit.metadata.MetadataValue> values = player.getMetadata("BloodShield_Active");
            if (!values.isEmpty()) {
                player.sendMessage("§c§l[!] §fNgừng Cuồng Huyết!");
                deactivateSkill(player, rpgPlayer, level);
                return true;
            }
        }

        // BẬT CHIÊU
        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§c§l[!] §fCần ít nhất " + (int)cost + " Nộ để khởi động Cuồng Huyết!");
            return false;
        }

        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey) && System.currentTimeMillis() < player.getMetadata(cdKey).get(0).asLong()) {
            player.sendMessage("§cKỹ năng đang hồi chiêu!");
            return false;
        }

        player.setMetadata("BloodShield_Active", new FixedMetadataValue(plugin, level));

        // [VFX CẢI TIẾN]: Nhịp tim đập bùng nổ khi bật chiêu
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 2.0f, 0.5f);
        Particle.DustOptions darkRed = new Particle.DustOptions(Color.fromRGB(139, 0, 0), 2.0f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 40, 1.0, 1.0, 1.0, 0.1, darkRed);

        rpgPlayer.updateMaxStats();

        // ==========================================
        // TASK 1: TIÊU HAO 15 NỘ MỖI GIÂY & VFX NHỊP TIM
        // ==========================================
        new BukkitRunnable() {
            int tickCounter = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !player.hasMetadata("BloodShield_Active")) {
                    this.cancel();
                    return;
                }

                // Chạy mỗi 20 ticks (1s) để trừ Nộ
                if (tickCounter % 20 == 0) {
                    if (rpgPlayer.getCurrentResource() >= 15.0) { // [ĐÃ SỬA]: Tiêu hao 15 Nộ/s
                        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - 15.0);
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_POINTED_DRIPSTONE_DRIP_LAVA, 1.0f, 0.5f);
                    } else {
                        player.sendMessage("§c§l[!] §fĐã cạn Nộ, Huyết Giáp bắt đầu ngưng tụ!");
                        deactivateSkill(player, rpgPlayer, level);
                        this.cancel();
                        return;
                    }
                }

                // [VFX CẢI TIẾN]: Tạo vòng máu xoáy liên tục nhẹ nhàng quanh người
                if (tickCounter % 3 == 0) {
                    double angle = Math.toRadians(tickCounter * 15 % 360);
                    Location pLoc = player.getLocation().add(Math.cos(angle) * 1.2, 0.1 + (tickCounter % 40) * 0.05, Math.sin(angle) * 1.2);
                    Particle.DustOptions red = new Particle.DustOptions(Color.RED, 1.0f);
                    player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, red);
                }

                tickCounter++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // ==========================================
        // TASK 2: HẤP THỤ MÁU DƯ VÀO HUYẾT BỂ
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

                            player.setMetadata("BloodShield_Accumulated_Pool", new FixedMetadataValue(plugin, currentAccumulated + overheal));
                            player.setMetadata("Lifesteal_Pool", new FixedMetadataValue(plugin, missingHp));

                            // [VFX CẢI TIẾN]: Hút hạt linh hồn đỏ vào người khi chém dư máu
                            Location pLoc = player.getLocation().add(0, 1, 0);
                            for (int i = 0; i < 3; i++) {
                                Location dropLoc = pLoc.clone().add((Math.random()-0.5)*3, Math.random()*2, (Math.random()-0.5)*3);
                                Vector dir = pLoc.toVector().subtract(dropLoc.toVector()).normalize().multiply(0.2);
                                player.getWorld().spawnParticle(Particle.DUST, dropLoc, 0, dir.getX(), dir.getY(), dir.getZ(), 1.0, new Particle.DustOptions(Color.RED, 1.2f));
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

        double accumulated = player.hasMetadata("BloodShield_Accumulated_Pool") ?
                player.getMetadata("BloodShield_Accumulated_Pool").get(0).asDouble() : 0.0;

        double finalShieldValue = accumulated * 1.0; // 100% Chuyển hóa

        player.removeMetadata("BloodShield_Active", plugin);
        player.removeMetadata("BloodShield_Accumulated_Pool", plugin);

        if (finalShieldValue > 0) {
            double maxHp = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();

            // [ĐÃ SỬA]: Cap giáp ảo tối đa bằng 160% (1.6) Tổng máu
            double maxShieldCap = maxHp * 1.6;
            finalShieldValue = Math.min(finalShieldValue, maxShieldCap);

            long expireTime = System.currentTimeMillis() + (18 * 1000L); // 18 Giây

            player.setMetadata("RPG_Absorption_Shield", new FixedMetadataValue(plugin, finalShieldValue));
            player.setMetadata("RPG_Absorption_Shield_Max", new FixedMetadataValue(plugin, finalShieldValue));
            player.setMetadata("RPG_Absorption_Shield_Expire", new FixedMetadataValue(plugin, expireTime));

            // Đánh dấu người chơi đang có Khiên Huyết Giáp để theo dõi Shatter
            player.setMetadata("BloodShield_Shatter_Check", new FixedMetadataValue(plugin, level));

            // Hiệu ứng Tim Vàng
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION, 18 * 20, 4, false, false, false));
            player.setAbsorptionAmount(20.0); // Full 1 thanh máu vàng hiển thị

            player.sendMessage("§e§l[!] §fNhận §6" + (int)finalShieldValue + " Giáp ảo §f(+8% Hút máu)!");
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1.5f, 0.5f);

            rpgPlayer.updateMaxStats();

            // ==========================================
            // TASK SHATTER (GIÁM SÁT KHIÊN VỠ)
            // ==========================================
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || !player.hasMetadata("BloodShield_Shatter_Check")) {
                        this.cancel();
                        return;
                    }

                    // Nếu metadata Khiên bị mất (Do CombatManager trừ sát thương chạm đáy 0)
                    if (!player.hasMetadata("RPG_Absorption_Shield")) {
                        triggerShatterExplosion(player, rpgPlayer, level);

                        player.removeMetadata("BloodShield_Shatter_Check", plugin);
                        player.removePotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION);
                        rpgPlayer.updateMaxStats(); // Xóa 8% Buff
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 1L, 1L);

            // ==========================================
            // TASK HẾT HẠN (18 GIÂY)
            // ==========================================
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        // Nếu vẫn còn thời hạn (chưa bị vỡ)
                        if (player.hasMetadata("RPG_Absorption_Shield_Expire") &&
                                System.currentTimeMillis() >= player.getMetadata("RPG_Absorption_Shield_Expire").get(0).asLong() - 100) {

                            player.removeMetadata("RPG_Absorption_Shield", plugin);
                            player.removeMetadata("RPG_Absorption_Shield_Max", plugin);
                            player.removeMetadata("RPG_Absorption_Shield_Expire", plugin);
                            player.removeMetadata("BloodShield_Shatter_Check", plugin); // Hủy cờ Shatter để không nổ

                            player.removePotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION);
                            player.setAbsorptionAmount(0);

                            rpgPlayer.updateMaxStats();
                            player.sendMessage("§8§o(Lớp giáp máu đã hết thời gian và tiêu tán...)");
                        }
                    }
                }
            }.runTaskLater(plugin, 18 * 20L);

        } else {
            rpgPlayer.updateMaxStats();
        }

        // Tính CD
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata("CD_" + getId(), new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
    }

    // ==========================================
    // HÀM KÍCH NỔ VÒNG TRÒN (SHATTER EXPLOSION)
    // ==========================================
    private void triggerShatterExplosion(Player player, RPGPlayer rpgPlayer, int level) {
        Location center = player.getLocation().add(0, 0.5, 0);
        player.sendMessage("§4§l[BẠO PHÁ] §cHuyết giáp vỡ vụn, kích nổ sát thương!");
        player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
        player.getWorld().playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 1.5f);

        // [Sát thương 420% + 30% mỗi cấp]
        double dmgMulti = 4.2 + ((level - 1) * 0.3);

        // Hiệu ứng vòng tròn lan rộng (Expanding Circle)
        new BukkitRunnable() {
            double radius = 0.5;
            final Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 2.5f);

            @Override
            public void run() {
                if (radius > 4.5) { // Phạm vi ~ 4 blocks
                    this.cancel();
                    return;
                }
                // Vẽ vòng tròn
                for (int i = 0; i < 360; i += 15) {
                    double rad = Math.toRadians(i);
                    Location edge = center.clone().add(Math.cos(rad) * radius, 0, Math.sin(rad) * radius);
                    center.getWorld().spawnParticle(Particle.DUST, edge, 1, 0.1, 0.1, 0.1, 0, red);

                    if (Math.random() > 0.6) { // Thêm vết chém lan
                        center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, edge, 1, 0, 0, 0, 0);
                    }
                }
                radius += 1.0; // Tốc độ nổ lan
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Quét và gây sát thương ngay lập tức cho các mục tiêu trong phạm vi
        for (Entity e : center.getWorld().getNearbyEntities(center, 4.5, 3.0, 4.5)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, (LivingEntity) e, dmgMulti, 0.0, false, false);
                CombatManager.applySkillDamage(player, (LivingEntity) e, finalDmg);

                // Hiệu ứng máu văng ra từ quái bị nổ
                e.getWorld().spawnParticle(Particle.BLOCK, e.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0, Material.NETHER_WART_BLOCK.createBlockData());
            }
        }
    }
}