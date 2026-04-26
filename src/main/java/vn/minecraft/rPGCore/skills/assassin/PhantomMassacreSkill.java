package vn.minecraft.rPGCore.skills.assassin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class PhantomMassacreSkill implements ISkill {

    private final JavaPlugin plugin;

    public PhantomMassacreSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "assassin_phantom_massacre"; }
    @Override public String getName() { return "§4§l[TUYỆT KỸ: TÀN ẢNH ĐỒ SÁT]"; }
    @Override public String getRequiredClass() { return "assassin"; }
    @Override public Material getIcon() { return Material.NETHERITE_SWORD; }

    @Override public int getMaxLevel() { return 3; }
    @Override public int getRequiredPlayerLevel() { return 50; }

    // ==========================================
    // BỔ SUNG CÁC HÀM TRÁNH LỖI ABSTRACT
    // ==========================================
    @Override public int getUpgradeCost(int currentLevel) { return 2; } // Cần 2 SP để nâng
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override public double getManaCost(int level) { return 180.0 + (level * 20.0); }
    @Override public long getCooldown(int level) { return (60 - (level * 5)) * 1000L; } // 55s -> 45s

    // [CÂN BẰNG]: 800% - 1000% - 1200%
    private double getBaseDamageMultiplier(int level) {
        return 6.0 + (level * 2.0);
    }

    @Override
    public List<String> getDescription(int level) {
        int dmgPercent = (int) (getBaseDamageMultiplier(level) * 100);
        return Arrays.asList(
                "§4§o\"Năm bước một mạng người, vạn dặm không lưu vết...\"",
                "",
                "§c§l[THÔNG SỐ TUYỆT KỸ]",
                "§7Khóa mục tiêu vào §c5 kẻ địch §7có HP thấp nhất xung quanh.",
                "§7Dịch chuyển ra sau lưng mục tiêu đã chọn và kết liễu mục tiêu.",
                "§7Sát thương kích sát mục tiêu: §c" + dmgPercent + "% §fSát thương",
                "",
                "§8[Nội tại - Cường Hóa]:",
                "§7- Dịch chuyển tới mục tiêu tiếp theo.",
                "§7- Đòn chém tiếp theo tăng thêm §c200% Sát thương§7.",
                "",
                "§8[Nội tại - Hạ Màn]:",
                "§7- Thi thể kẻ địch nổ tung gây §e300% Sát thương§7",
                "§7  diện rộng cho những kẻ địch xung quanh.",
                "",
                "§8§o*Lưu ý: Chuỗi chiêu kết thúc nếu mục tiêu không bị kết liễu."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        // 1. TÌM MỤC TIÊU TRƯỚC
        List<LivingEntity> targets = new ArrayList<>();
        for (org.bukkit.entity.Entity e : player.getNearbyEntities(12, 8, 12)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                targets.add((LivingEntity) e);
            }
        }
        if (targets.isEmpty()) {
            player.sendMessage("§cKhông tìm thấy con mồi nào xung quanh!");
            return false;
        }

        // ==========================================
        // [ĐÃ SỬA]: SCALE THEO LEVEL CỦA FATAL BLOW
        // ==========================================
        double fbMultiplier = 1.0;
        if (player.hasMetadata("Assassin_FatalBlow_Level")) {
            int fbLevel = player.getMetadata("Assassin_FatalBlow_Level").get(0).asInt();
            player.removeMetadata("Assassin_FatalBlow_Level", plugin);

            // Công thức chuẩn từ FatalBlowSkill: 150% + 10%/level
            // Cấp 1 = 1.5x, Cấp 6 = 2.0x
            fbMultiplier = 1.5 + ((fbLevel - 1) * 0.1);

            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 30, 0.3, 0.3, 0.3, 0.2);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.5f);
            player.sendMessage("§c§l[!] §fTuyệt kỹ được cường hóa bởi Sát Khí rực lửa!");
        }

        // 2. TRỪ MANA VÀ COOLDOWN
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;
        applyGlobalCooldown(player, rpgPlayer, level);

        // 3. TÌM VÀ SẮP XẾP TẤT CẢ MỤC TIÊU (Không cắt xuống 5 nữa!)
        targets.sort(Comparator.comparingDouble(LivingEntity::getHealth));

        // [ĐÃ SỬA]: Giữ nguyên toàn bộ danh sách, coi như "Quỹ đạo dự phòng"
        final List<LivingEntity> allTargets = targets;

        // Phá tàng hình
        ISkill stealthSkill = vn.minecraft.rPGCore.RPGCore.getInstance().getSkillManager().getSkill("assassin_smoke_bomb");
        if (stealthSkill instanceof vn.minecraft.rPGCore.skills.assassin.SmokeBombSkill) {
            ((vn.minecraft.rPGCore.skills.assassin.SmokeBombSkill) stealthSkill).breakStealth(player);
        }

        player.setMetadata("Is_Casting", new FixedMetadataValue(plugin, true));
        player.setMetadata("RPGCore_SafeFall", new FixedMetadataValue(plugin, true));

        player.sendMessage("§4§l[!] TÀN ẢNH ĐỒ SÁT!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.2f);

        final double finalFbMulti = fbMultiplier;

        new BukkitRunnable() {
            int listIndex = 0;         // Con trỏ duyệt danh sách tổng
            int successfulStrikes = 0; // Đếm số nhát chém thực tế (Mục tiêu 5)
            double bonusDmg = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    stop("§4§l[!] ĐỒ SÁT BỊ NGẮT!");
                    return;
                }

                // ==========================================
                // [TUA NHANH]: Lướt qua ngay lập tức lũ đã chết, không delay!
                // ==========================================
                LivingEntity target = null;
                while (listIndex < allTargets.size()) {
                    LivingEntity checkTarget = allTargets.get(listIndex);
                    if (!checkTarget.isDead() && checkTarget.isValid()) {
                        target = checkTarget; // Tóm được con mồi còn sống!
                        break;
                    }
                    // Nếu con này đã bị ai đó giết trước, bỏ qua ngay lập tức sang con sau
                    listIndex++;
                }

                // Nếu đã chém đủ 5 nhát, HOẶC đã duyệt sạch danh sách mà không còn ai sống
                if (successfulStrikes >= 5 || target == null) {
                    stop("§4§l[!] ĐỒ SÁT HOÀN TẤT!");
                    return;
                }

                // Tiến hành dịch chuyển và chém mục tiêu hợp lệ
                teleportBehind(player, target);

                double finalDmgMulti = (getBaseDamageMultiplier(level) + bonusDmg) * finalFbMulti;

                player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                double damage = CombatManager.calculateDamage(player, rpgPlayer, target, finalDmgMulti, 0, 100.0, 0, false, false);
                CombatManager.applySkillDamage(player, target, damage);
                player.removeMetadata("Ghost_L_Blocker", plugin);

                final LivingEntity currentTarget = target; // Bọc lại cho inner Runnable

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (currentTarget.isDead() || currentTarget.getHealth() <= 0) {

                            successfulStrikes++; // Tăng số lượng nhát chém thành công
                            listIndex++;         // Chuyển sang con tiếp theo cho vòng lặp tới

                            // Kiểm tra xem đây có phải nhát chốt hạ không (Đủ 5 nhát hoặc hết quái dự phòng)
                            boolean isFinalStrike = (successfulStrikes >= 5 || listIndex >= allTargets.size());

                            spawnBloodExplosion(currentTarget.getLocation(), player, rpgPlayer, isFinalStrike);

                            if (!isFinalStrike) {
                                bonusDmg += 2.0;
                                player.sendMessage("§a§l+200% Sát thương kích sát!");
                            } else {
                                player.sendMessage("§4§l[!] TỬ THẦN ĐOẠT MỆNH KẾT THÚC!");
                            }

                        } else {
                            stop("§c§l[!] Chuỗi kích sát bị chặn lại bởi kẻ thù cứng đầu!");
                        }
                    }
                }.runTaskLater(plugin, 5L);
            }

            private void stop(String msg) {
                player.removeMetadata("Is_Casting", plugin);
                player.sendMessage(msg);
                player.setFallDistance(0);
                this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 12L);

        // Cấp Stack Sát Ý
        if (rpgPlayer.getCurrentClass() instanceof vn.minecraft.rPGCore.classes.impl.AssassinClass) {
            ((vn.minecraft.rPGCore.classes.impl.AssassinClass) rpgPlayer.getCurrentClass()).addSkillStack(player);
        }

        return true;
    }

    private void teleportBehind(Player p, LivingEntity t) {
        Vector dir = t.getLocation().getDirection().setY(0).normalize();
        if (dir.lengthSquared() == 0) dir = new Vector(1, 0, 0);

        Location behind = t.getLocation().clone().subtract(dir.multiply(1.2)).add(0, 0.2, 0);
        behind.setDirection(dir);
        p.teleport(behind);

        p.getWorld().playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
        p.getWorld().playSound(behind, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.5f);
        p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, t.getLocation().add(0, 1, 0), 3);
    }

    private void spawnBloodExplosion(Location loc, Player source, RPGPlayer rpgSource, boolean isFinalStrike) {

        if (!isFinalStrike) {
            // Hiệu ứng nổ bình thường cho các mạng lót đường
            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
            loc.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, loc.add(0, 1, 0), 80, 0.5, 0.5, 0.5, Material.REDSTONE_BLOCK.createBlockData());
            loc.getWorld().spawnParticle(Particle.LAVA, loc, 15, 0.5, 0.5, 0.5, 0.1);
        } else {
            // ==========================================
            // GRAND FINALE: HIỆU ỨNG CHỐT HẠ KHỦNG KHIẾP
            // ==========================================
            loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 2.0f); // Tiếng sét xé tai
            loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.2f); // Tiếng rít của vực thẳm
            loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f); // Kính vỡ

            // 1. Cột linh hồn đen bốc lên trời
            loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1, 0), 200, 1.0, 3.0, 1.0, 0.05);
            loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0), 100, 1.5, 2.0, 1.5, 0.1);
            loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 1, 0), 2);

            // 2. Vẽ chữ X đẫm máu (Vết chém tử thần)
            Particle.DustOptions bloodDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(150, 0, 0), 2.5f);
            Vector dir = source.getLocation().getDirection().setY(0).normalize();
            Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize(); // Vector ngang
            Vector up = new Vector(0, 1, 0); // Vector dọc

            Location center = loc.clone().add(0, 1.5, 0);
            for (double t = -2.0; t <= 2.0; t += 0.2) {
                // Đường chéo 1
                Location p1 = center.clone().add(right.clone().multiply(t)).add(up.clone().multiply(t));
                loc.getWorld().spawnParticle(Particle.DUST, p1, 2, 0, 0, 0, 0, bloodDust);
                loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, p1, 1);

                // Đường chéo 2
                Location p2 = center.clone().add(right.clone().multiply(t)).add(up.clone().multiply(-t));
                loc.getWorld().spawnParticle(Particle.DUST, p2, 2, 0, 0, 0, 0, bloodDust);
            }
        }

        // ==========================================
        // [ĐÃ SỬA]: VÙNG NỔ CHÍNH XÁC 3x3x3 BLOCK (Bán kính 1.5)
        // Sát thương chỉ chạm tới những con quái đang đứng dính sát vào nạn nhân.
        // ==========================================
        for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 2, 2, 2)) {
            if (e instanceof LivingEntity && e != source && CombatManager.canTarget(source, e)) {
                source.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                double splashDmg = CombatManager.calculateDamage(source, rpgSource, (LivingEntity)e, 3.0, 0, 0, 0, false, false);
                CombatManager.applySkillDamage(source, (LivingEntity)e, splashDmg);
                source.removeMetadata("Ghost_L_Blocker", plugin);
            }
        }
    }
}