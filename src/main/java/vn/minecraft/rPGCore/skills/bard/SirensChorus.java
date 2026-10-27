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
import vn.minecraft.rPGCore.classes.impl.bard.melodies.SiphonMelody;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SirensChorus implements ISkill {

    @Override
    public String getId() { return "bard_sirens_chorus"; }

    @Override
    public String getName() { return "Khúc Ca Dẫn Hồn"; }

    @Override
    public String getRequiredClass() { return "bard"; }

    @Override
    public Material getIcon() { return Material.SCULK_SHRIEKER; }

    @Override
    public int getMaxLevel() { return 5; }

    @Override
    public int getUpgradeCost(int currentLevel) { return 1; }

    @Override
    public int getRequiredPlayerLevel() { return 25; }

    @Override
    public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) { return 45.0 + (level * 5.0); }

    @Override
    public long getCooldown(int level) { return (long) ((20.0 - level) * 1000); }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLevel = Math.max(1, level);

        int tickDmg = 15 + (displayLevel * 5); // ST Hút (x3 lần)
        int launchDmg = 30 + (displayLevel * 10); // ST Hất tung
        int blastDmg = 50 + (displayLevel * 15); // ST Nổ

        return Arrays.asList(
                "§7Phóng một hợp âm nén có trọng lượng,",
                "§7tạo ra một Hố Đen Âm Nhạc khi va chạm.",
                "",
                "§8▪ §fLoại Âm Tiết: §6[ Âm Trầm ]",
                "§8▪ §fLoại Giai Điệu: §a[ Hút Hồn ]",
                "§8▪ §fPhạm vi: §e6 Block",
                "",
                "§c[Thi Triển - Khúc Ca Dẫn Hồn]",
                "§f- Hố đen hình thành sau 0.5s chờ.",
                "§f- §eGiai đoạn 1 (1.5s):§f Hút mạnh kẻ địch vào tâm, gây",
                "§f  §c" + tickDmg + "% ST Phép§f mỗi 0.5s.",
                "§f- §eGiai đoạn 2 (1.0s):§f Hất tung mục tiêu lên cao,",
                "§f  gây tức thì §c" + launchDmg + "% ST Phép§f.",
                "§f- §eGiai đoạn 3 (1.5s):§f Kẻ địch lơ lửng trên không.",
                "§f- §eGiai đoạn 4:§f Hố đen phát nổ, hất văng mọi thứ",
                "§f  ra xa và gây §c" + blastDmg + "% ST Phép§f.",
                "",
                "§a[Cộng Hưởng Giai Điệu - Hút Hồn]",
                "§f- Tâm kích hoạt Giai điệu được dời đến vị trí Hố đen.",
                "§f- Khắc dấu ấn Hút Hồn: Đồng minh tấn công mục tiêu",
                "§f  bị đánh dấu sẽ hồi phục 0.5% Máu Tối Đa."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        // [SIÊU HÀM API]: Tự động xử lý Mana, CDR, Check Metadata[cite: 9, 19]
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;

        // CHECK ĐIỀU KIỆN KÍCH NỔ GIAI ĐIỆU
        int maxCapacity = rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_8") > 0 ? 4 : 3;
        List<ToneType> currentTones = ToneManager.getCurrentTones(player);
        final boolean isMelodyTriggered = (currentTones.size() + 1) >= maxCapacity && MelodyUtils.canTriggerMelody(player);

        if (isMelodyTriggered && rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_2") > 0) {
            if (player.getHealth() / rpgPlayer.getTotalMaxHealth() > 0.90) {
                rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() + getManaCost(level));
                player.sendMessage("§d§l[Hơi Thở Của Gió] §fThi triển không tốn Năng lượng!");
            }
        }

        // TÍNH TOÁN CHỈ SỐ (Effectively Final cho Runnable)
        double highToneMultiplier = ToneModifier.getHighToneMultiplier(player, ToneType.LOW);
        double echoRadiusMultiplier = ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer, ToneType.LOW);
        int sharpToneCount = ToneModifier.getSharpToneCount(player, ToneType.LOW);

        final double tickMulti = (0.15 + (level * 0.05)) * highToneMultiplier;
        final double launchMulti = (0.30 + (level * 0.10)) * highToneMultiplier;
        final double blastMulti = (0.50 + (level * 0.15)) * highToneMultiplier;

        final double extraMagicPen = sharpToneCount * 10.0;
        final double radius = 6.0 * echoRadiusMultiplier;

        // PHASE 1: BẮN ĐẠN ĐẠO CÓ TRỌNG LỰC (GRAVITY PROJECTILE)
        Location startLoc = player.getEyeLocation();
        Vector velocity = startLoc.getDirection().multiply(1.5); // Vận tốc ban đầu
        player.getWorld().playSound(startLoc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.0f, 0.5f);

        new BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            int flightTicks = 0;

            @Override
            public void run() {
                flightTicks++;

                // Áp dụng Trọng lực (Kéo trục Y xuống mỗi tick)
                velocity.setY(velocity.getY() - 0.06);
                currentLoc.add(velocity);

                // VFX Quả cầu Âm nhạc bay
                currentLoc.getWorld().spawnParticle(Particle.NOTE, currentLoc, 1, 0.1, 0.1, 0.1, 1);
                currentLoc.getWorld().spawnParticle(Particle.WITCH, currentLoc, 2, 0.1, 0.1, 0.1, 0);

                // Kiểm tra va chạm (Collision): Chạm đất hoặc bay quá 40 ticks (~2 giây)
                boolean hitGround = currentLoc.getBlock().getType().isSolid();
                boolean hitEntity = false;

                if (!hitGround) {
                    for (Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                        if (e instanceof LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)) {
                            hitEntity = true; break;
                        }
                    }
                }

                if (hitGround || hitEntity || flightTicks > 40) {
                    this.cancel();
                    // Đưa tâm hố đen lên mặt đất (nếu chìm)
                    while (currentLoc.getBlock().getType().isSolid() && currentLoc.getY() < 256) {
                        currentLoc.add(0, 0.5, 0);
                    }
                    startBlackHoleSequence(player, rpgPlayer, currentLoc, radius, tickMulti, launchMulti, blastMulti, extraMagicPen, isMelodyTriggered);
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);

        // Khóa Cooldown ngay khi bấm[cite: 9]
        applyGlobalCooldown(player, rpgPlayer, level);
        return true;
    }

    // MÁY TRẠNG THÁI (STATE MACHINE) CHO CÁC PHASE TỪ 3 -> 7
    private void startBlackHoleSequence(Player player, RPGPlayer rpgPlayer, Location center, double radius, double tickMulti, double launchMulti, double blastMulti, double pen, boolean isMelodyTriggered) {

        // Cố định tọa độ Y sát mặt đất để VFX không bay lơ lửng
        Location groundCenter = center.clone();
        groundCenter.setY(Math.floor(groundCenter.getY()) + 0.15);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                // =====================================
                // PHASE 3: HÌNH THÀNH (0.5s = 10 Ticks)
                // =====================================
                if (ticks <= 10) {
                    if (ticks == 1) groundCenter.getWorld().playSound(groundCenter, Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 0.5f);
                    // Vẽ vòng Telegraph báo trước nguy hiểm
                    MelodyUtils.drawExpandingAura(groundCenter, radius, org.bukkit.Color.AQUA, Particle.ENCHANT);
                }

                // =====================================
                // PHASE 4: HÚT MỌI THỨ (1.5s = 30 Ticks, từ 11 -> 40)
                // =====================================
                else if (ticks > 10 && ticks <= 40) {
                    if (ticks == 11) {
                        groundCenter.getWorld().playSound(groundCenter, Sound.BLOCK_BEACON_AMBIENT, 2.0f, 1.5f);
                        // KÍCH NỔ GIAI ĐIỆU Ở ĐÂY ĐỂ ĐỒNG MINH CÓ THỂ ĐÁNH HÚT MÁU NGAY
                        if (isMelodyTriggered) {
                            new SiphonMelody().cast(player, rpgPlayer, groundCenter);
                            ToneManager.consumeTones(player, rpgPlayer);
                            MelodyUtils.setMelodyCooldown(player);
                        } else {
                            ToneManager.addTone(player, rpgPlayer, ToneType.LOW);
                        }
                    }

                    // VFX Vòng xoáy Nốt nhạc sát mặt đất
                    drawMusicalVortex(groundCenter, radius);

                    // Xử lý Lực hút & Sát thương
                    for (Entity e : groundCenter.getWorld().getNearbyEntities(groundCenter, radius, 4.0, radius)) {
                        if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                            LivingEntity target = (LivingEntity) e;

                            // Lực Hút rất mạnh
                            Vector pullDir = groundCenter.toVector().subtract(target.getLocation().toVector());
                            if (pullDir.lengthSquared() > 0.5) {
                                target.setVelocity(pullDir.normalize().multiply(0.6).setY(0.15));
                            }

                            // Gây sát thương mỗi 10 ticks (0.5s) -> Tổng 3 lần
                            if (ticks % 10 == 0) {
                                triggerDamage(player, rpgPlayer, target, tickMulti, pen);
                            }
                        }
                    }
                }

                // =====================================
                // PHASE 5: HẤT TUNG (1.0s = 20 Ticks, từ 41 -> 60)
                // =====================================
                else if (ticks > 40 && ticks <= 60) {
                    if (ticks == 41) {
                        groundCenter.getWorld().playSound(groundCenter, Sound.ENTITY_WARDEN_ROAR, 1.0f, 1.5f);
                        groundCenter.getWorld().spawnParticle(Particle.SONIC_BOOM, groundCenter.clone().add(0, 1, 0), 1);

                        for (Entity e : groundCenter.getWorld().getNearbyEntities(groundCenter, radius, 4.0, radius)) {
                            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                                LivingEntity target = (LivingEntity) e;
                                // Hất bổng cực mạnh lên trời
                                target.setVelocity(new Vector(0, 1.5, 0));
                                // Gây sát thương hất tung 1 lần
                                triggerDamage(player, rpgPlayer, target, launchMulti, pen);
                            }
                        }
                    }
                }

                // =====================================
                // PHASE 6: LƠ LỬNG AIRBORNE (1.5s = 30 Ticks, từ 61 -> 90)
                // =====================================
                else if (ticks > 60 && ticks <= 90) {
                    for (Entity e : groundCenter.getWorld().getNearbyEntities(groundCenter, radius, 8.0, radius)) {
                        if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                            // Treo giò mục tiêu trên không bằng cách set Vector Y rất nhỏ
                            Vector v = e.getVelocity();
                            e.setVelocity(new Vector(v.getX(), -0.05, v.getZ()));
                        }
                    }
                }

                // =====================================
                // PHASE 7: NỔ VĂNG (Tick 91)
                // =====================================
                else if (ticks >= 91) {
                    groundCenter.getWorld().playSound(groundCenter, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
                    groundCenter.getWorld().spawnParticle(Particle.EXPLOSION, groundCenter.clone().add(0, 3, 0), 3, 1, 1, 1, 0.1);

                    for (Entity e : groundCenter.getWorld().getNearbyEntities(groundCenter, radius, 8.0, radius)) {
                        if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                            LivingEntity target = (LivingEntity) e;

                            // Đẩy văng mạnh ra xung quanh
                            Vector pushOut = target.getLocation().toVector().subtract(groundCenter.toVector()).normalize();
                            target.setVelocity(pushOut.multiply(1.8).setY(0.4));

                            // Sát thương dứt điểm
                            triggerDamage(player, rpgPlayer, target, blastMulti, pen);
                        }
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // Hàm tiện ích: Vẽ vòng xoáy Nhạc sát mặt đất
    private void drawMusicalVortex(Location center, double radius) {
        double angle = (System.currentTimeMillis() / 50.0) % (2 * Math.PI);
        for (double r = radius; r > 0; r -= 1.0) {
            double x = Math.cos(angle + r) * r;
            double z = Math.sin(angle + r) * r;
            Location pLoc = center.clone().add(x, 0, z);

            pLoc.getWorld().spawnParticle(Particle.ENCHANT, pLoc, 1, 0, 0.2, 0, 0.5);
            if (Math.random() > 0.7) {
                pLoc.getWorld().spawnParticle(Particle.NOTE, pLoc.clone().add(0, 0.5, 0), 0, Math.random(), 0, 0, 1);
            }
        }
    }

    // Hàm tiện ích: Đóng gói việc tính toán và gây sát thương chuẩn Core
    private void triggerDamage(Player player, RPGPlayer rpgPlayer, LivingEntity target, double multi, double pen) {
        double dmg = CombatManager.calculateDamage(player, rpgPlayer, target, multi, pen, true, false);
        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
        // applySkillDamage tự động kích hoạt tính năng Hút máu chuẩn của hệ thống
        CombatManager.applySkillDamage(player, target, dmg);
        player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
    }
}