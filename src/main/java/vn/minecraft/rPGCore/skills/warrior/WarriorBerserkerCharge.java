package vn.minecraft.rPGCore.skills.warrior;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WarriorBerserkerCharge implements ISkill {

    private final JavaPlugin plugin;

    public WarriorBerserkerCharge(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "warrior_berserker_charge"; }
    @Override public String getName() { return "§c§lCuồng Nộ Xung Phong"; }
    @Override public String getRequiredClass() { return "warrior"; }
    @Override public Material getIcon() { return Material.MINECART; } // Biểu tượng như cỗ xe tăng

    @Override public int getMaxLevel() { return 5; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public int getRequiredPlayerLevel() { return 8; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override public boolean isPassiveSkill() { return false; }
    @Override public double getManaCost(int level) { return 25.0; } // 25 Nộ
    @Override public long getCooldown(int level) { return 12000L; } // 12 Giây

    @Override
    public List<String> getDescription(int level) {
        int slamDmg = 200 + (level * 50); // 250% -> 450%
        return Arrays.asList(
                "§7Hóa thân thành cỗ xe tăng ủi thẳng về phía trước.",
                "§7Khóa cứng hướng lướt, không thể bẻ lái.",
                "",
                "§e[Lướt Thường (Không Nộ)]",
                "§f- Lướt đi §a5 blocks§f.",
                "§f- Kẻ địch trúng đòn chịu §c100% ST Vũ khí§f.",
                "",
                "§c[Thức Tỉnh (Tiêu hao 25 Nộ)]",
                "§f- Lướt đi xa hơn (§a10 blocks§f) và càn quét mọi",
                "§f  kẻ địch trên đường, cuốn chúng đi theo.",
                "§f- §cÉp Vách:§f Nếu ủi quái đập vào tường, gây §c" + slamDmg + "% ST§f",
                "§f  và Làm choáng chúng trong §a1.5s§f.",
                "§f- §eBật Lùi:§f Nếu đường lướt kết thúc mà không trúng tường,",
                "§f  bạn sẽ tự động nảy bật về sau để giữ an toàn."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey) && System.currentTimeMillis() < player.getMetadata(cdKey).get(0).asLong()) {
            player.sendMessage("§cKỹ năng đang hồi chiêu!");
            return false;
        }

        double cost = getManaCost(level);
        boolean isEmpowered = rpgPlayer.getCurrentResource() >= cost;

        // [CƠ CHẾ LÕI]: Tính toán Vector cố định ban đầu, bỏ qua trục Y (Không cho bay lên trời)
        Vector fixedDirection = player.getLocation().getDirection().setY(0).normalize();

        if (isEmpowered) {
            rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
            executeEmpoweredCharge(player, rpgPlayer, level, fixedDirection);
        } else {
            executeNormalCharge(player, rpgPlayer, fixedDirection);
        }

        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata(cdKey, new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
        // Gọi thẳng hàm từ ISkill
        startCooldownNotifier(player, finalCd);
        return true;
    }

    // ==========================================
    // 1. LƯỚT THƯỜNG (KHÔNG NỘ) - 5 BLOCKS
    // ==========================================
    private void executeNormalCharge(Player player, RPGPlayer rpgPlayer, Vector direction) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);

        // Đẩy 1 cục duy nhất đi ~5 block
        Vector push = direction.clone().multiply(1.5).setY(0.2);
        player.setVelocity(push);

        // Quét sát thương 100% trong 5 tick
        new BukkitRunnable() {
            int ticks = 0;
            final Set<UUID> hitTargets = new HashSet<>();

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 5) {
                    this.cancel();
                    return;
                }

                // VFX Lướt gió
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.05);

                for (Entity e : player.getNearbyEntities(1.5, 1.5, 1.5)) {
                    if (e instanceof LivingEntity && !hitTargets.contains(e.getUniqueId()) && CombatManager.canTarget(player, e)) {
                        hitTargets.add(e.getUniqueId());
                        double dmg = CombatManager.calculateDamage(player, rpgPlayer, (LivingEntity) e, 1.0, 0.0, false, false);
                        CombatManager.applySkillDamage(player, (LivingEntity) e, dmg);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // 2. LƯỚT THỨC TỈNH (CÓ NỘ) - ỦI QUÁI / ÉP TƯỜNG / NẢY BẬT
    // ==========================================
    private void executeEmpoweredCharge(Player player, RPGPlayer rpgPlayer, int level, Vector direction) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.5f, 1.2f);
        player.sendMessage("§c§l[!] CUỒNG NỘ XUNG PHONG!");

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 12; // Khoảng 10-12 blocks
            final Set<LivingEntity> draggedEnemies = new HashSet<>();
            final Particle.DustOptions red = new Particle.DustOptions(Color.RED, 1.5f);

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }

                Location currentLoc = player.getLocation();
                Location nextLoc = currentLoc.clone().add(direction.clone().multiply(1.5)); // Check block trước mặt
                Block nextBlock = nextLoc.getBlock();

                // 1. CHECK ÉP VÁCH (Tông Tường)
                if (nextBlock.getType().isSolid()) {
                    triggerWallSlam(player, rpgPlayer, level, draggedEnemies, nextBlock.getLocation());
                    this.cancel();
                    return;
                }

                // 2. DI CHUYỂN NGƯỜI CHƠI (Ép force theo Vector cố định mỗi tick, vô hiệu hóa bẻ lái)
                player.setVelocity(direction.clone().multiply(1.2)); // Tốc độ lướt
                player.getWorld().spawnParticle(Particle.DUST, currentLoc.add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0, red);
                player.getWorld().spawnParticle(Particle.FLAME, currentLoc, 2, 0.2, 0.1, 0.2, 0.05);

                // 3. GOM QUÁI (Kéo xệch đi theo)
                for (Entity e : player.getNearbyEntities(2.0, 2.0, 2.0)) {
                    if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                        draggedEnemies.add((LivingEntity) e);
                    }
                }

                // Ép bọn quái bị bắt bay cùng vận tốc với Player
                for (LivingEntity dragged : draggedEnemies) {
                    if (dragged.isValid() && !dragged.isDead()) {
                        dragged.setVelocity(direction.clone().multiply(1.2));
                        // Gây sát thương ma sát nhỏ (Nhịp 1)
                        if (ticks % 3 == 0) {
                            CombatManager.applySkillDamage(player, dragged, 10.0); // Sát thương fix kéo xệch
                        }
                    }
                }

                ticks++;

                // 4. KẾT THÚC ĐƯỜNG LƯỚT MÀ KHÔNG TRÚNG TƯỜNG -> NẢY BẬT VỀ SAU
                if (ticks >= maxTicks) {
                    // Đẩy quái văng nhẹ về phía trước
                    for (LivingEntity dragged : draggedEnemies) {
                        if (dragged.isValid()) dragged.setVelocity(direction.clone().multiply(0.8));
                    }

                    // Người chơi nảy bật về sau (Rebound)
                    Vector bounceBack = direction.clone().multiply(-1.2).setY(0.4);
                    player.setVelocity(bounceBack);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 0.8f);
                    player.sendMessage("§e§o(Nảy bật an toàn!)");

                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // 3. CÚ NỔ ÉP VÁCH (WALL SLAM)
    // ==========================================
    private void triggerWallSlam(Player player, RPGPlayer rpgPlayer, int level, Set<LivingEntity> enemies, Location wallLoc) {
        player.setVelocity(new Vector(0, 0, 0)); // Phanh gấp
        player.getWorld().playSound(wallLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.5f, 0.5f);
        player.getWorld().playSound(wallLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        // VFX Đá văng tung tóe
        player.getWorld().spawnParticle(Particle.BLOCK, wallLoc, 40, 1.0, 1.0, 1.0, 0, Material.COBBLESTONE.createBlockData());
        player.getWorld().spawnParticle(Particle.EXPLOSION, wallLoc, 1, 0, 0, 0, 0);

        // Tính sát thương nhịp 2
        double multiplier = 2.0 + (level * 0.5); // 250% -> 450%

        if (enemies.isEmpty()) {
            player.sendMessage("§7§o(Tông vào tường nhưng không bắt được ai...)");
            return;
        }

        player.sendMessage("§4§l[ÉP VÁCH] §cNgươi không có lối thoát!");

        for (LivingEntity target : enemies) {
            if (target.isValid() && !target.isDead()) {
                double slamDmg = CombatManager.calculateDamage(player, rpgPlayer, target, multiplier, 0.0, false, false);
                CombatManager.applySkillDamage(player, target, slamDmg);

                // Hiệu ứng Choáng (Stun = Root 100% tốc chạy)
                StatusEffectManager.applySlow(target, 30, 1.0, 10);
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 30, 10, false, false, false));
                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 2, 0), 15, 0.5, 0.2, 0.5, 0.1);
            }
        }
    }
}