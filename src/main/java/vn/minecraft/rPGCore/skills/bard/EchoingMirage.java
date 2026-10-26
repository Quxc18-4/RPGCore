package vn.minecraft.rPGCore.skills.bard;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.melodies.MelodyUtils;
import vn.minecraft.rPGCore.classes.impl.bard.melodies.MirageMelody;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EchoingMirage implements ISkill {

    @Override
    public String getId() { return "bard_echoing_mirage"; }

    @Override
    public String getName() { return "Cung Đàn Lưu Ảnh"; }

    @Override
    public String getRequiredClass() { return "bard"; }

    @Override
    public Material getIcon() { return Material.PHANTOM_MEMBRANE; }

    @Override
    public int getMaxLevel() { return 5; }

    @Override
    public int getUpgradeCost(int currentLevel) { return 1; }

    @Override
    public int getRequiredPlayerLevel() { return 15; }

    @Override
    public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) { return 35.0 + (level * 5.0); }

    @Override
    public long getCooldown(int level) {
        // Cũ: 14.0 - level (13s -> 9s)
        // Mới: 20.0 - level (19s -> 15s)
        return (long) ((20.0 - level) * 1000);
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLevel = Math.max(1, level);
        int bounceDmg = 60 + (displayLevel * 20);
        int headStompBonus = 60;

        return Arrays.asList(
                "§7Hòa mình vào giai điệu, biến cơ thể",
                "§7thành một quả bom âm thanh nảy liên tục.",
                "",
                "§8▪ §fLoại Âm Tiết: §c[ Âm Sắc ]",
                "§8▪ §fLoại Giai Điệu: §a[ Ảo Ảnh ]",
                "",
                "§c[Thi Triển - Vũ Điệu Lưu Ảnh]",
                "§f- Bật nhảy tối đa §e3 Nhịp§f. Mỗi lần đáp đất gây",
                "§f  §c" + bounceDmg + "% ST Phép§f xung quanh.",
                "§f- Đạp trúng đầu kẻ địch gây thêm §c" + headStompBonus + "% ST Phép§f",
                "§f  đồng thời §eTrói chân (Choáng)§f nạn nhân trong 2s.",
                "§f- §e§l[Tuyệt Kỹ]:§f Nếu nhịp thứ 3 Đạp Đầu thành công,",
                "§f  nhận thêm §bNhịp Nảy thứ 4 §f(Không sát thương).",
                "",
                "§a[Cộng Hưởng Giai Điệu - Ảo Ảnh]",
                "§f- Ở lần đáp đất cuối cùng, bùng nổ đẩy lùi mọi kẻ địch,",
                "§f  Tàng hình tuyệt đối 2s và xóa bỏ Aggro."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;

        int maxCapacity = rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_8") > 0 ? 4 : 3;
        List<ToneType> currentTones = ToneManager.getCurrentTones(player);
        final boolean isMelodyTriggered = (currentTones.size() + 1) >= maxCapacity && MelodyUtils.canTriggerMelody(player);

        if (isMelodyTriggered && rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_2") > 0) {
            if (player.getHealth() / rpgPlayer.getTotalMaxHealth() > 0.90) {
                rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() + getManaCost(level));
                player.sendMessage("§d§l[Hơi Thở Của Gió] §fThi triển không tốn Năng lượng!");
            }
        }

        double highToneMultiplier = ToneModifier.getHighToneMultiplier(player, ToneType.SHARP);
        int sharpToneCount = ToneModifier.getSharpToneCount(player, ToneType.SHARP);

        final double bounceDmgMulti = (0.6 + (level * 0.2)) * highToneMultiplier;
        final double headBonusMulti = 0.6 * highToneMultiplier;
        final double extraMagicPen = sharpToneCount * 10.0;

        player.sendMessage("§a§l[Vũ Điệu Lưu Ảnh] §fBắt đầu nhịp điệu!");
        forceJump(player);

        new BukkitRunnable() {
            int currentBounce = 1;
            int fallTicks = 0;
            boolean isFalling = false;
            boolean wasLastBounceHeadStomp = false;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }

                if (Math.random() > 0.5) {
                    player.getWorld().spawnParticle(Particle.NOTE, player.getLocation().add(0, 1, 0), 0, Math.random(), 0, 0, 1);
                }

                Vector vel = player.getVelocity();
                if (vel.getY() < -0.1) {
                    isFalling = true;
                    fallTicks++;
                } else {
                    fallTicks = 0;
                }

                // [SAFETY NET]: Cứu mạng nếu rơi quá 1.2s
                if (fallTicks > 24) {
                    player.setVelocity(new Vector(vel.getX(), 0.5, vel.getZ()));
                    player.setFallDistance(0);

                    if (currentBounce <= 3) {
                        triggerExplosion(player, rpgPlayer, null, bounceDmgMulti, extraMagicPen, wasLastBounceHeadStomp);
                    }
                    applyFinaleKnockback(player);
                    finalizeSkill(false);
                    return;
                }

                if (isFalling) {
                    LivingEntity stompedTarget = checkHeadStomp(player);
                    boolean isGrounded = ((Entity) player).isOnGround() || isNearWater(player.getLocation());

                    if (stompedTarget != null || isGrounded) {
                        isFalling = false;
                        fallTicks = 0;
                        player.setFallDistance(0);

                        // 3 Nhịp Sát Thương Đầu Tiên
                        if (currentBounce <= 3) {
                            triggerExplosion(player, rpgPlayer, stompedTarget, bounceDmgMulti, extraMagicPen, wasLastBounceHeadStomp);
                            wasLastBounceHeadStomp = (stompedTarget != null);

                            // KIỂM TRA ĐIỀU KIỆN NHỊP 4 (ENCORE)
                            if (currentBounce == 3 && stompedTarget != null) {
                                currentBounce++;
                                forceJump(player);
                                player.sendMessage("§e§l[Tuyệt Kỹ] §fKhúc vĩ thanh! Nhịp nảy an toàn kích hoạt!");
                            }
                            // Nếu nhịp 3 rớt xuống đất -> Hạ Màn
                            else if (currentBounce == 3) {
                                applyFinaleKnockback(player);
                                finalizeSkill(true);
                            }
                            // Nhịp 1, 2 -> Nảy tiếp
                            else {
                                currentBounce++;
                                forceJump(player);
                            }
                        }
                        // Đang ở Nhịp 4 (Nhịp vô hại) -> Hạ Màn
                        else {
                            spawnSafeLandingVFX(player.getLocation());
                            applyFinaleKnockback(player);
                            finalizeSkill(true);
                        }
                    }
                }
            }

            // Bên trong BukkitRunnable...
            private void finalizeSkill(boolean normalEnd) {
                if (isMelodyTriggered) {
                    new MirageMelody().cast(player, rpgPlayer, player.getLocation());
                    ToneManager.consumeTones(player, rpgPlayer);
                    MelodyUtils.setMelodyCooldown(player);
                } else {
                    ToneManager.addTone(player, rpgPlayer, ToneType.SHARP);
                }

                if (normalEnd) player.sendMessage("§d§l[!] §fNhịp điệu kết thúc!");

                // [DI DỜI]: Bắt đầu tính Cooldown ngay tại thời điểm kết thúc kỹ năng
                applyGlobalCooldown(player, rpgPlayer, level);

                this.cancel();
            }

        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);

        return true;
    }

    private void forceJump(Player player) {
        Vector dir = player.getLocation().getDirection().setY(0).normalize();
        player.setVelocity(dir.multiply(0.7).setY(0.85));
        StatusEffectManager.grantTemporarySafeFall(player, 40);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.5f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 10, 0.2, 0.1, 0.2, 0.1);
    }

    private boolean isNearWater(Location loc) {
        Material m = loc.getBlock().getType();
        Material below = loc.clone().subtract(0, 0.5, 0).getBlock().getType();
        return m == Material.WATER || below == Material.WATER;
    }

    private LivingEntity checkHeadStomp(Player player) {
        Location below = player.getLocation().subtract(0, 0.5, 0);
        for (Entity e : below.getWorld().getNearbyEntities(below, 1.0, 0.8, 1.0)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                return (LivingEntity) e;
            }
        }
        return null;
    }

    // ==========================================
    // HÀM: ĐẨY LÙI KHI HẠ MÀN (KNOCKBACK)
    // ==========================================
    private void applyFinaleKnockback(Player player) {
        Location pLoc = player.getLocation();
        pLoc.getWorld().playSound(pLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.8f, 1.2f);
        pLoc.getWorld().spawnParticle(Particle.EXPLOSION, pLoc.clone().add(0, 1, 0), 2);

        for (Entity e : pLoc.getWorld().getNearbyEntities(pLoc, 4, 3, 4)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                Vector pushDir = e.getLocation().toVector().subtract(pLoc.toVector()).normalize();
                pushDir.setY(0.4); // Hất lên một chút để tránh kẹt block
                e.setVelocity(pushDir.multiply(1.2)); // Đẩy mạnh ra xa
            }
        }
    }

    // ==========================================
    // HÀM: VẼ HÌNH XOẮN ỐC NỐT NHẠC (HELIX VFX) & TRÓI CHÂN
    // ==========================================
    private void applyHeadStompStunAndVFX(LivingEntity target) {
        // [TÍCH HỢP]: Khóa cứng mục tiêu trong 2 giây (40 Ticks)[cite: 17]
        StatusEffectManager.applyAbsoluteRoot(target, 40);

        // Vẽ Helix Nốt Nhạc bao quanh nạn nhân
        new BukkitRunnable() {
            int ticks = 0;
            double yOffset = 0;

            @Override
            public void run() {
                if (ticks >= 40 || target.isDead() || !target.isValid()) {
                    this.cancel();
                    return;
                }

                // Toán học quỹ đạo tròn (Bán kính 1.0)
                double x = 1.0 * Math.cos(ticks * 0.5);
                double z = 1.0 * Math.sin(ticks * 0.5);
                Location particleLoc = target.getLocation().add(x, yOffset, z);

                particleLoc.getWorld().spawnParticle(Particle.NOTE, particleLoc, 0, Math.random(), 0, 0, 1);

                yOffset += 0.08;
                if (yOffset > 2.0) yOffset = 0;

                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void triggerExplosion(Player player, RPGPlayer rpgPlayer, LivingEntity stompedTarget, double baseMulti, double pen, boolean wasPrevHeadStomp) {
        Location pLoc = player.getLocation();
        boolean isHeadStomp = (stompedTarget != null);

        pLoc.getWorld().playSound(pLoc, Sound.BLOCK_BELL_RESONATE, 1.0f, 0.8f);
        pLoc.getWorld().playSound(pLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 2.0f);
        pLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, pLoc.clone().add(0, 0.2, 0), 1);

        for (double i = 0; i <= 2 * Math.PI; i += Math.PI / 8) {
            Location noteLoc = pLoc.clone().add(Math.cos(i) * 2.5, 0.2, Math.sin(i) * 2.5);
            pLoc.getWorld().spawnParticle(Particle.NOTE, noteLoc, 0, Math.random(), 0, 0, 1);
        }

        if (isHeadStomp) {
            pLoc.getWorld().playSound(pLoc, Sound.BLOCK_ANVIL_LAND, 0.8f, 1.2f);
            pLoc.getWorld().spawnParticle(Particle.CRIT, pLoc.clone().add(0, 0.5, 0), 30, 0.5, 0.5, 0.5, 0.2);

            // Kích hoạt Khống chế và Đồ họa Xoắn ốc
            applyHeadStompStunAndVFX(stompedTarget);
        }

        for (Entity e : pLoc.getWorld().getNearbyEntities(pLoc, 4, 3, 4)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                LivingEntity target = (LivingEntity) e;
                double finalMulti = baseMulti;

                if (isHeadStomp && target.equals(stompedTarget)) {
                    finalMulti += 0.6;
                } else if (!isHeadStomp && wasPrevHeadStomp) {
                    finalMulti *= 0.5;
                }

                double dmg = CombatManager.calculateDamage(player, rpgPlayer, target, finalMulti, pen, true, false);

                player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                CombatManager.applySkillDamage(player, target, dmg);
                player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
            }
        }
    }

    private void spawnSafeLandingVFX(Location pLoc) {
        pLoc.getWorld().playSound(pLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);
        for (double i = 0; i <= 2 * Math.PI; i += Math.PI / 6) {
            Location noteLoc = pLoc.clone().add(Math.cos(i) * 1.5, 0.2, Math.sin(i) * 1.5);
            pLoc.getWorld().spawnParticle(Particle.NOTE, noteLoc, 0, Math.random(), 0, 0, 1);
        }
    }
}