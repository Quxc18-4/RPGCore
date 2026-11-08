package vn.minecraft.rPGCore.skills.reaper;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BloodAegisSkill extends AbstractEnhancementSkill {

    public BloodAegisSkill(JavaPlugin plugin) {
        super(plugin);
    }

    @Override public String getId() { return "reaper_blood_aegis"; }
    @Override public String getName() { return "Huyết Thuẫn"; }
    @Override public String getRequiredClass() { return "reaper"; }
    @Override public Material getIcon() { return Material.RED_DYE; }
    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) { return 40.0 + (level * 2.0); }

    @Override
    public long getCooldown(int level) {
        double cd = 20.0 - (level * 0.5); // 19.5s -> 15s
        return (long) (cd * 1000);
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLvl = Math.max(1, level);
        int baseDmg = 150 + (displayLvl * 10);
        int shieldRatio = 30 + (displayLvl * 2); // 32% -> 50%

        return List.of(
                "§7Cô đặc toàn bộ máu đang cuộn chảy trong",
                "§7Bể Máu (Lifesteal Pool) thành một lớp",
                "§7giáp bảo vệ tuyệt đối.",
                "",
                "§e[Kích Hoạt - Huyết Thuẫn]",
                "§f- Rút §c100%§f lượng máu từ Bể Máu, lập tức",
                "§f  chuyển hóa thành §eGiáp Ảo (Tối đa 50% HP)§f.",
                "",
                "§4[Phản Công - Nổ Huyết Áp]",
                "§f- Nếu lớp Giáp Ảo này bị đánh vỡ hoàn toàn",
                "§f  trong vòng §c5 giây§f, nó sẽ phát nổ 5x5.",
                "§f- Sát thương nổ: §c" + baseDmg + "% ST Vũ khí",
                "§f  kèm §e" + shieldRatio + "% ST Chuẩn §f(Dựa trên lượng Giáp",
                "§f  vừa nhận được lúc bật chiêu)."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey)) {
            if (System.currentTimeMillis() < player.getMetadata(cdKey).get(0).asLong()) {
                double timeLeft = (player.getMetadata(cdKey).get(0).asLong() - System.currentTimeMillis()) / 1000.0;
                player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
                return false;
            }
        }

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực!");
            return false;
        }

        // ==========================================
        // 1. KIỂM TRA BỂ MÁU (LIFESTEAL POOL)
        // ==========================================
        double poolAmount = 0.0;
        if (player.hasMetadata("Lifesteal_Pool")) {
            poolAmount = player.getMetadata("Lifesteal_Pool").get(0).asDouble();
        }

        if (poolAmount <= 10.0) {
            player.sendMessage("§c§l[!] §fBể máu trống rỗng, không thể ngưng tụ Huyết Thuẫn!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        // Trừ Thể lực & Xóa trắng Bể Máu
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        player.removeMetadata("Lifesteal_Pool", plugin);

        long finalCd = getCooldown(level);
        player.setMetadata(cdKey, new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        // ==========================================
        // 2. NẠP GIÁP ẢO (KHÔNG CHẶN TRẦN Ở ĐÂY NỮA)
        // ==========================================
        double maxHp = rpgPlayer.getTotalMaxHealth();
        double currentShield = player.hasMetadata("RPG_Absorption_Shield") ? player.getMetadata("RPG_Absorption_Shield").get(0).asDouble() : 0.0;

        // Đẩy thẳng lượng giáp thô vào hệ thống. RegenTask sẽ tự động gọt phần thừa ở tick tiếp theo.
        double newShieldTotal = currentShield + poolAmount;
        player.setMetadata("RPG_Absorption_Shield", new FixedMetadataValue(plugin, newShieldTotal));

        // [FIX BUKKIT UI]: Ép Client vẽ tim vàng ngay lúc bật chiêu (Phản hồi tức thời)
        if (!player.hasPotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION)) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.ABSORPTION,
                    Integer.MAX_VALUE, 0, false, false, false));
        }

        // Tạm thời vẽ UI kịch khung 20 tim (RegenTask sẽ gọt lại chuẩn xác ngay sau đó)
        double uiAbsorptionHearts = (newShieldTotal / maxHp) * 20.0;
        player.setAbsorptionAmount(Math.min(20.0, uiAbsorptionHearts));

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.5f);
        player.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, player.getLocation().add(0, 1.5, 0), 50, 0.5, 0.8, 0.5, Material.REDSTONE_BLOCK.createBlockData());

        // Báo số lượng giáp bơm vào (dù sau đó RegenTask có vứt phần thừa đi)
        player.sendMessage("§4§l[HUYẾT THUẪN] §fBơm §e" + (int)poolAmount + " Huyết Khí§f vào khiên!");

        // ==========================================
        // 3. CƠ CHẾ PHẢN CÔNG (NỔ SAU KHI VỠ GIÁP TRONG 5S)
        // ==========================================
        // Lượng giáp dùng để tính sát thương nổ bây giờ chính là số máu rút từ Bể
        startRetaliationTask(player, rpgPlayer, level, poolAmount);

        return true;
    }

    private void startRetaliationTask(Player player, RPGPlayer rpgPlayer, int level, double shieldGained) {
        // Khóa định danh Task để chống đè
        long taskId = System.currentTimeMillis();
        player.setMetadata("Reaper_Aegis_Task", new FixedMetadataValue(plugin, taskId));

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 100; // 5 giây = 100 ticks

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }

                // Nếu người chơi dùng chiêu lại (override), hủy task cũ
                if (player.hasMetadata("Reaper_Aegis_Task") && player.getMetadata("Reaper_Aegis_Task").get(0).asLong() != taskId) {
                    this.cancel();
                    return;
                }

                if (ticks >= maxTicks) {
                    // Hết 5s mà giáp không vỡ -> Hủy bom nổ chậm
                    player.sendMessage("§8§o(Huyết Thuẫn mất trạng thái phản công...)");
                    player.removeMetadata("Reaper_Aegis_Task", plugin);
                    this.cancel();
                    return;
                }

                // Kiểm tra liên tục: Giáp vỡ chưa?
                double currentShield = player.hasMetadata("RPG_Absorption_Shield") ? player.getMetadata("RPG_Absorption_Shield").get(0).asDouble() : 0.0;

                if (currentShield <= 0) {
                    // GIÁP VỠ! KÍCH NỔ!
                    explode(player, rpgPlayer, level, shieldGained);
                    player.removeMetadata("Reaper_Aegis_Task", plugin);
                    this.cancel();
                    return;
                }

                // Hiệu ứng vòng sáng bảo vệ quay quanh người trong 5s
                if (ticks % 5 == 0) {
                    double angle = (ticks * Math.PI / 10);
                    double x = 1.2 * Math.cos(angle);
                    double z = 1.2 * Math.sin(angle);
                    player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(x, 1.0, z), 1, 0,0,0,0, new Particle.DustOptions(org.bukkit.Color.MAROON, 1.5f));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // Chạy 1 tick / lần
    }

    private void explode(Player player, RPGPlayer rpgPlayer, int level, double shieldGained) {
        player.sendMessage("§c§l[!] HUYẾT ÁP BÙNG NỔ!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 1, 0), 3);

        double baseDmgMulti = 1.5 + (level * 0.10); // 150% -> 250%
        double trueDmgRatio = 0.30 + (level * 0.02); // 32% -> 50% ST chuẩn từ giáp nạp vào
        double trueDmgAmount = shieldGained * trueDmgRatio;

        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), 2.5, 2.5, 2.5)) {
            if (e instanceof LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)) {
                LivingEntity victim = (LivingEntity) e;

                double finalPhysDmg = CombatManager.calculateDamage(player, rpgPlayer, victim, baseDmgMulti, 0.0, false, false);
                double totalDmg = finalPhysDmg + trueDmgAmount;

                player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                CombatManager.applySkillDamage(player, victim, totalDmg);
                player.removeMetadata("Ghost_L_Blocker", plugin);
            }
        }
    }
}