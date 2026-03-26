package vn.minecraft.rPGCore.skills.assassin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class ShadowStepSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public ShadowStepSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "assassin_shadow_step"; }
    @Override public String getName() { return "Gia Tốc Bóng Đêm"; }
    @Override public String getRequiredClass() { return "assassin"; }
    @Override public Material getIcon() { return Material.RABBIT_FOOT; }
    @Override public int getMaxLevel() { return 6; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        Map<String, Integer> req = new HashMap<>();
        req.put("assassin_smoke_bomb", 4);
        return req;
    }

    @Override
    public int getRequiredPlayerLevel() {
        return 8; // Yêu cầu Level 35
    }

    @Override
    public double getManaCost(int level) {
        return 35.0 + ((level - 1) * 5.0);
    }

    @Override
    public long getCooldown(int level) {
        double cd = 18.0;
        if (level >= 3) cd -= 1.0;
        if (level >= 6) cd -= 2.0;
        return (long) (cd * 1000);
    }

    private double getSpeedBonus(int level) {
        return 35.0 + ((level - 1) * 15.0);
    }

    private double getJumpBonus(int level) {
        return 0.50 + ((level - 1) * 0.10);
    }

    private int getBaseDuration(int level) {
        return 5 + level;
    }

    @Override
    public List<String> getDescription(int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        double speed = getSpeedBonus(displayLevel);
        double jump = getJumpBonus(displayLevel) * 100;
        int duration = getBaseDuration(displayLevel);

        lore.add("§7Vận khí nhẹ bước, gia tăng độ cơ động");
        lore.add("§7trong một khoảng thời gian.");
        lore.add("");
        lore.add("§7Tốc độ di chuyển: §b+" + (int)speed + "%");
        lore.add("§7Sức bật nhảy: §a+" + (int)jump + "%");
        lore.add("§7Hiệu ứng thêm: §fTự động vượt địa hình 1 block");
        lore.add("§7Thời gian hiệu lực: §e" + duration + " giây");
        lore.add("");
        lore.add("§8[Nội tại - Tương tác Ảo Ảnh]:");
        lore.add("§7Nếu thi triển khi §8Bom Khói Ảo Ảnh§7 đang");
        lore.add("§7hoạt động, nhận thêm §e+3 giây§7 hiệu lực.");

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực (Stamina)!");
            return false;
        }

        // 2. KIỂM TRA HỒI CHIÊU (Áp dụng ý tưởng của bạn)
        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);

        long baseCd = getCooldown(level); // Lấy thời gian gốc
        long finalCd = getFinalCooldown(rpgPlayer, baseCd); // Đưa qua máy ép CDR

        if (currentTime - lastUsed < finalCd) {
            double timeLeft = (finalCd - (currentTime - lastUsed)) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        boolean hasSmokeSynergy = player.hasMetadata("Assassin_Smoke_Bomb_Active");

        int duration = getBaseDuration(level);
        if (hasSmokeSynergy) {
            duration += 3;
            player.sendMessage("§8§l[!] §7Gia tốc ẩn vào màn khói... §e(+3s)");
        }

        // 3. TRỪ NĂNG LƯỢNG VÀ LƯU COOLDOWN
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);

        startCooldownNotifier(player, finalCd);

        player.sendMessage("§b§l[!] GIA TỐC BÓNG ĐÊM!");
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.5f);

        Location loc = player.getLocation();
        for (int i = 0; i < 20; i++) {
            double angle = 2 * Math.PI * Math.random();
            double r = 1.0;
            loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                    loc.clone().add(r * Math.cos(angle), Math.random() * 2, r * Math.sin(angle)), 1, 0, 0, 0, 0.05);
        }

        applyMobilityBuffs(player, rpgPlayer, getSpeedBonus(level), getJumpBonus(level), duration);

        if (rpgPlayer.getCurrentClass() instanceof vn.minecraft.rPGCore.classes.impl.AssassinClass) {
            ((vn.minecraft.rPGCore.classes.impl.AssassinClass) rpgPlayer.getCurrentClass()).addSkillStack(player);
        }

        return true;
    }

    private void applyMobilityBuffs(Player player, RPGPlayer rpgPlayer, double speedBonusPct, double jumpBonusScalar, int durationSeconds) {

        // 1. BUFF TỐC ĐỘ DI CHUYỂN
        player.setMetadata("Skill_MoveSpeed_Bonus", new FixedMetadataValue(plugin, speedBonusPct));
        rpgPlayer.updateMoveSpeed();

        // 2. BUFF LỰC NHẢY
        AttributeInstance jumpAttr = player.getAttribute(Attribute.JUMP_STRENGTH);
        org.bukkit.NamespacedKey jumpKey = new org.bukkit.NamespacedKey(plugin, "assassin_swift_jump");

        if (jumpAttr != null) {
            for (AttributeModifier mod : jumpAttr.getModifiers()) {
                if (mod.getKey().equals(jumpKey)) jumpAttr.removeModifier(mod);
            }
            AttributeModifier newJump = new AttributeModifier(jumpKey, jumpBonusScalar, AttributeModifier.Operation.ADD_SCALAR);
            jumpAttr.addModifier(newJump);
        }

        // ==========================================
        // 3. [MỚI]: BUFF BƯỚC CHÂN (Leo 1 block không cần nhảy)
        // ==========================================
        AttributeInstance stepAttr = player.getAttribute(Attribute.STEP_HEIGHT);
        org.bukkit.NamespacedKey stepKey = new org.bukkit.NamespacedKey(plugin, "assassin_swift_step");

        if (stepAttr != null) {
            for (AttributeModifier mod : stepAttr.getModifiers()) {
                if (mod.getKey().equals(stepKey)) stepAttr.removeModifier(mod);
            }
            // Thêm 0.5 vào Base (0.6) = 1.1 block
            AttributeModifier newStep = new AttributeModifier(stepKey, 0.5, AttributeModifier.Operation.ADD_NUMBER);
            stepAttr.addModifier(newStep);
        }

        player.setMetadata("Assassin_Swift_Step_Active", new FixedMetadataValue(plugin, true));

        // Hiệu ứng khói dưới chân
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !player.hasMetadata("Assassin_Swift_Step_Active")) {
                    this.cancel();
                    return;
                }
                if (player.getVelocity().lengthSquared() > 0.02) {
                    player.getWorld().spawnParticle(Particle.SQUID_INK, player.getLocation().add(0, 0.1, 0), 2, 0.2, 0.0, 0.2, 0.02);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        // Hẹn giờ hủy Buff
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    // Trả lại Jump
                    if (jumpAttr != null) {
                        for (AttributeModifier mod : jumpAttr.getModifiers()) {
                            if (mod.getKey().equals(jumpKey)) jumpAttr.removeModifier(mod);
                        }
                    }

                    // Trả lại Step Height
                    if (stepAttr != null) {
                        for (AttributeModifier mod : stepAttr.getModifiers()) {
                            if (mod.getKey().equals(stepKey)) stepAttr.removeModifier(mod);
                        }
                    }

                    // Trả lại Speed
                    if (player.hasMetadata("Skill_MoveSpeed_Bonus")) {
                        player.removeMetadata("Skill_MoveSpeed_Bonus", plugin);
                        rpgPlayer.updateMoveSpeed();
                    }

                    player.removeMetadata("Assassin_Swift_Step_Active", plugin);
                    player.sendMessage("§7§o[!] Đã hết hiệu lực Gia Tốc Bóng Đêm.");
                }
            }
        }.runTaskLater(plugin, durationSeconds * 20L);
    }
}