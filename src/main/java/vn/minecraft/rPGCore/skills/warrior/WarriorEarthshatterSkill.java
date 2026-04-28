package vn.minecraft.rPGCore.skills.warrior;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
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
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WarriorEarthshatterSkill implements ISkill {

    // Khởi tạo Random để dùng cho hiệu ứng gạch đá bay lộn xộn
    private final Random random = new Random();

    @Override public String getId() { return "warrior_earthshatter"; }
    @Override public String getName() { return "§6§lĐịa Chấn"; }
    @Override public String getRequiredClass() { return "warrior"; }
    @Override public Material getIcon() { return Material.ANVIL; }

    @Override public int getMaxLevel() { return 6; }
    @Override public int getUpgradeCost(int currentLevel) { return 2; }
    @Override public int getRequiredPlayerLevel() { return 5; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override public boolean isPassiveSkill() { return false; }

    @Override public double getManaCost(int level) { return 45; }
    @Override public long getCooldown(int level) { return 15000L - (level * 1000L); }

    @Override
    public List<String> getDescription(int level) {
        int normalDmg = (int) ((1.0 + (level * 0.25)) * 100);
        int awakenDmg = (int) ((2.0 + (level * 0.6)) * 100);

        return Arrays.asList(
                "§7Tuyệt kỹ dọn dẹp chiến trường của Chiến Binh.",
                "§7Dùng sức mạnh cơ bắp nghiền nát mặt đất.",
                "",
                "§e[Kích hoạt: Dậm Đất]",
                "§f- Dậm mạnh xuống đất, gây §e" + normalDmg + "% ST Vật lý",
                "§f  lên mọi kẻ địch xung quanh (Bán kính 4m).",
                "",
                "§c[Thức Tỉnh Kỹ Năng]",
                "§f- Nhảy vọt lên và giáng đòn hủy diệt.",
                "§f- Sát thương tăng vọt lên §c" + awakenDmg + "% ST Vật lý§f.",
                "§f- Kẻ địch bị §6Hất tung§f, văng gạch đá và §8Choáng§f.",
                "§f  trong 1.5 giây."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey)) {
            List<org.bukkit.metadata.MetadataValue> metaList = player.getMetadata(cdKey);

            // Phải đảm bảo danh sách không rỗng trước khi get(0)
            if (!metaList.isEmpty()) {
                long cdEnd = metaList.get(0).asLong();
                if (System.currentTimeMillis() < cdEnd) {
                    player.sendMessage("§cKỹ năng đang hồi chiêu!");
                    return false;
                }
            } else {
                // Nếu danh sách rỗng (Lỗi rác), xóa cái key đó đi cho sạch
                player.removeMetadata(cdKey, RPGCore.getInstance());
            }
        }

        double cost = getManaCost(level);
        boolean isEmpowered = rpgPlayer.getCurrentResource() >= cost;

        if (isEmpowered) {
            rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
            player.sendMessage("§c§l[THỨC TỈNH] §fĐịa Chấn!");

            // Bật nhảy mạnh hơn một chút để tạo độ Impact (0.7 -> 0.8 Y)
            Vector leapDir = player.getLocation().getDirection().setY(0).normalize().multiply(0.4).setY(0.8);
            player.setVelocity(leapDir);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);

            player.setMetadata("Is_Casting", new FixedMetadataValue(RPGCore.getInstance(), true));

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
            // [KHÔNG ĐỦ NỘ] -> trigger luôn
            triggerEarthshatter(player, rpgPlayer, level, false);
        }

        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata("CD_" + getId(), new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        return true;
    }

    private void triggerEarthshatter(Player player, RPGPlayer rpgPlayer, int level, boolean isEmpowered) {
        Location hitLoc = player.getLocation();
        double radius = 4.0;

        // ==========================================
        // VFX & SFX MỚI: BÙNG NỔ VÀ VỠ GẠCH TUNG TÓE
        // ==========================================
        if (isEmpowered) {
            // 1. Âm thanh bùng nổ hỗn hợp cực lớn
            player.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.6f);
            player.getWorld().playSound(hitLoc, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.5f);
            player.getWorld().playSound(hitLoc, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.8f);

            // 2. Vụ nổ trung tâm
            player.getWorld().spawnParticle(Particle.EXPLOSION, hitLoc.add(0, 0.3, 0), 3, 0.5, 0.1, 0.5, 0);

            // 3. Khói bụi mù trời (Nhiều khói hơn)
            player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, hitLoc, 50, radius, 0.3, radius, 0.05);

            // 4. [MỚI - QUAN TRỌNG]: HIỆU ỨNG VỠ GẠCH (BLOCK CRUMBLE)
            // Lấy khối đất/đá người chơi đang đứng để vẽ màu cho gạch vỡ
            Material groundMat = hitLoc.clone().subtract(0, 1, 0).getBlock().getType();
            if (!groundMat.isSolid()) groundMat = Material.STONE; // Backup nếu đứng trên không
            BlockData blockData = groundMat.createBlockData();

            // Vẽ hiệu ứng nổ gạch tung tóe (Dùng vòng lặp toán học để vẽ trong bán kính 3m)
            for (int i = 0; i < 60; i++) { // Bắn 60 mảnh gạch
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = random.nextDouble() * radius; // Trong bán kính 3m

                Location pLoc = hitLoc.clone().add(Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist);

                // Spawn gạch vỡ và cho nó bay lộn xộn một chút (offset X, Y, Z)
                player.getWorld().spawnParticle(
                        Particle.BLOCK, // Hạt loại Khối vỡ
                        pLoc, // Vị trí
                        1, // Số lượng hạt mỗi mảnh
                        0.2, random.nextDouble() * 0.5, 0.2, // Offset (độ tơi)
                        0.1, // Tốc độ bay
                        blockData // Dữ liệu khối để Client vẽ màu
                );
            }

        } else {
            // [BẢN THƯỜNG]: CHỈ KHÓI VÀ BỤI
            player.getWorld().playSound(hitLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.2f, 0.8f);
            player.getWorld().playSound(hitLoc, Sound.BLOCK_GRASS_BREAK, 1.0f, 1.2f);

            // Spawn khói thường ( Particle.EXPLOSION_NORMAL bản chất là khói trắng)
            player.getWorld().spawnParticle(Particle.EXPLOSION, hitLoc.add(0, 0.5, 0), 25, 1.0, 0.1, 1.0, 0.1);
            // Thêm chút bụi bay lả tả
            player.getWorld().spawnParticle(Particle.SMOKE, hitLoc, 20, 1.5, 0.1, 1.5, 0.01);
        }

        // 2. HITBOX SÁT THƯƠNG
        double multiplier = isEmpowered ? (2.0 + (level * 0.6)) : (1.0 + (level * 0.25));

        for (Entity e : hitLoc.getWorld().getNearbyEntities(hitLoc, radius, radius, radius)) {
            if (e instanceof LivingEntity && !(e instanceof ArmorStand) && CombatManager.canTarget(player, e)) {
                LivingEntity target = (LivingEntity) e;

                double dmg = CombatManager.calculateDamage(player, rpgPlayer, target, multiplier, 0.0, false, false);

                player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                CombatManager.applySkillDamage(player, target, dmg);
                player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                if (isEmpowered) {
                    // HIỆU ỨNG KHỐNG CHẾ
                    target.setVelocity(new Vector(0, 0.8, 0)); // Hất tung
                    StatusEffectManager.applySlow(target, 30, 1.0, 10); // Choáng 1.5s (Trói 100%)
                }
            }
        }
    }
}