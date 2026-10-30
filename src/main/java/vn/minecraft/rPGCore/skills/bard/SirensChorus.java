package vn.minecraft.rPGCore.skills.bard;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.melodies.MelodyUtils;
import vn.minecraft.rPGCore.classes.impl.bard.melodies.SiphonMelody;
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
    public double getManaCost(int level) {
        // Bắt đầu 53 mana (45 + 8), mỗi cấp tăng 8
        return 45.0 + (level * 8.0);
    }

    @Override
    public long getCooldown(int level) { return (long) ((20.0 - level) * 1000); }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLevel = Math.max(1, level);

        // Slow: Khởi điểm 60%, mỗi cấp +10%
        int slowPercent = 50 + (displayLevel * 10);
        // ST Hất tung: Max 340%, mỗi cấp 15% -> Lvl 1 là 280%
        int launchDmg = 265 + (displayLevel * 15);
        // ST Dập xuống: Max 440%, mỗi cấp 15% -> Lvl 1 là 380%
        int blastDmg = 365 + (displayLevel * 15);

        return Arrays.asList(
                "§7Phóng một hợp âm nén có trọng lực nén,",
                "§7tạo ra một Dòng Chảy Âm Nhạc khi va chạm.",
                "",
                "§8▪ §fLoại Âm Tiết: §6[ Âm Trầm ]",
                "§8▪ §fLoại Giai Điệu: §a[ Hút Hồn ]",
                "§8▪ §fPhạm vi: §e6 Block",
                "",
                "§c[Thi Triển - Khúc Ca Dẫn Hồn]",
                "§f- §e[Dòng Chảy]:§f Tạo vùng làm chậm §c" + slowPercent + "%§f",
                "§f  tốc độ di chuyển của kẻ địch trong 1.5s.",
                "§f- §e[Cao Trào]:§f Đột ngột hất tung mục tiêu lên trời,",
                "§f  gây §c" + launchDmg + "% ST Phép§f và lở lửng trong 1.5s.",
                "§f- §e[Hạ Màn]:§f Đập mạnh kẻ địch xuống đất, phá nát",
                "§f  mặt đất và gây §c" + blastDmg + "% ST Phép§f."
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

        double highToneMultiplier = ToneModifier.getHighToneMultiplier(player, ToneType.LOW);
        double echoRadiusMultiplier = ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer, ToneType.LOW);
        int sharpToneCount = ToneModifier.getSharpToneCount(player, ToneType.LOW);

        // HỆ SỐ MỚI
        final double launchMulti = (2.65 + (level * 0.15)) * highToneMultiplier;
        final double blastMulti = (3.65 + (level * 0.15)) * highToneMultiplier;

        final double slowRate = 0.50 + (level * 0.10); // Khởi điểm 60% (0.6), max 100% (1.0)
        final int extraMagicPen = (int)(sharpToneCount * 10.0);
        final double radius = 6.0 * echoRadiusMultiplier; // Vùng 6x6

        Location startLoc = player.getEyeLocation();
        Vector velocity = startLoc.getDirection().multiply(1.5);
        player.getWorld().playSound(startLoc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.0f, 0.5f);

        new BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            int flightTicks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                flightTicks++;
                velocity.setY(velocity.getY() - 0.06);
                currentLoc.add(velocity);

                currentLoc.getWorld().spawnParticle(Particle.NOTE, currentLoc, 1, 0.1, 0.1, 0.1, 1);
                currentLoc.getWorld().spawnParticle(Particle.WITCH, currentLoc, 2, 0.1, 0.1, 0.1, 0);

                boolean hitGround = currentLoc.getBlock().getType().isSolid();
                boolean hitEntity = false;

                if (!hitGround) {
                    for (Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                        if (e instanceof LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)) {
                            hitEntity = true; break;
                        }
                    }
                }

                if (hitGround || hitEntity || flightTicks > 100) {
                    this.cancel();
                    while (currentLoc.getBlock().getType().isSolid() && currentLoc.getY() < 256) {
                        currentLoc.add(0, 0.5, 0);
                    }
                    startGravitySequence(player, rpgPlayer, currentLoc, radius, launchMulti, blastMulti, extraMagicPen, isMelodyTriggered, slowRate);
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);

        applyGlobalCooldown(player, rpgPlayer, level);
        return true;
    }

    private void startGravitySequence(Player player, RPGPlayer rpgPlayer, Location center, double radius, double launchMulti, double blastMulti, double pen, boolean isMelodyTriggered, double slowRate) {

        Location groundCenter = center.clone();
        groundCenter.setY(Math.floor(groundCenter.getY()) + 0.15);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                ticks++;

                // =====================================
                // PHASE 3: HÌNH THÀNH (Tick 1 -> 10 ~ 0.5s)
                // =====================================
                if (ticks <= 10) {
                    if (ticks == 1) groundCenter.getWorld().playSound(groundCenter, Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 0.5f);
                    MelodyUtils.drawExpandingAura(groundCenter, radius, org.bukkit.Color.AQUA, Particle.ENCHANT);
                }

                // =====================================
                // PHASE 4: ĐẦM LẦY TRỌNG LỰC LÀM CHẬM (Tick 11 -> 40 ~ 1.5s)
                // =====================================
                else if (ticks > 10 && ticks <= 40) {
                    if (ticks == 11) {
                        groundCenter.getWorld().playSound(groundCenter, Sound.BLOCK_BEACON_AMBIENT, 2.0f, 1.5f);
                        if (isMelodyTriggered) {
                            new SiphonMelody().cast(player, rpgPlayer, groundCenter);
                            ToneManager.consumeTones(player, rpgPlayer);
                            MelodyUtils.setMelodyCooldown(player);
                        } else {
                            ToneManager.addTone(player, rpgPlayer, ToneType.LOW);
                        }
                    }

                    drawGravityVortex(groundCenter, radius);

                    for (Entity e : groundCenter.getWorld().getNearbyEntities(groundCenter, radius, 4.0, radius)) {
                        if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                            LivingEntity target = (LivingEntity) e;

                            // [PHÂN LUỒNG MỚI]: Player nhận slowRate, Quái nhận Potion (Amplifier 4 = -80%)
                            if (target instanceof Player) {
                                StatusEffectManager.applySlow(target, 20, slowRate, 0);
                            } else {
                                StatusEffectManager.applySlow(target, 20, 1.0, 4);
                            }
                        }
                    }
                }

                // =====================================
                // PHASE 5.1: KÍCH HOẠT HẤT TUNG (Tick 41) - Không Sát Thương
                // =====================================
                else if (ticks == 41) {
                    groundCenter.getWorld().playSound(groundCenter, Sound.ENTITY_WARDEN_ROAR, 1.0f, 1.5f);
                    groundCenter.getWorld().spawnParticle(Particle.SONIC_BOOM, groundCenter.clone().add(0, 1, 0), 1);

                    for (Entity e : groundCenter.getWorld().getNearbyEntities(groundCenter, radius, 4.0, radius)) {
                        if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                            LivingEntity target = (LivingEntity) e;
                            // Ép vận tốc bốc thẳng lên trời (Y=0.75)
                            target.setVelocity(new Vector(0, 0.75, 0));
                        }
                    }
                }

                // =====================================
                // PHASE 5.2: SÁT THƯƠNG HẤT TUNG LẦN 1 (Tick 44)
                // =====================================
                else if (ticks == 44) {
                    for (Entity e : groundCenter.getWorld().getNearbyEntities(groundCenter, radius, 6.0, radius)) {
                        if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                            LivingEntity target = (LivingEntity) e;
                            triggerDamage(player, rpgPlayer, target, launchMulti, pen);

                            // [THÊM MỚI]: Ép Potion Buồn Nôn (Say nhạc - Tipsy) trong 1.5s (30 ticks)
                            target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 40, 1, false, false, false));
                        }
                    }
                }

                // =====================================
                // PHASE 6: LƠ LỬNG AIRBORNE (Tick 48 -> 78 ~ 1.5s)
                // =====================================
                else if (ticks >= 48 && ticks <= 78) {
                    for (Entity e : groundCenter.getWorld().getNearbyEntities(groundCenter, radius, 8.0, radius)) {
                        if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                            Vector v = e.getVelocity();
                            // Treo lơ lửng, triệt tiêu trọng lực rơi tự do
                            e.setVelocity(new Vector(v.getX() * 0.5, 0.04, v.getZ() * 0.5));
                        }
                    }
                }

                // =====================================
                // PHASE 7: ĐẬP MẠNH XUỐNG ĐẤT (Tick 79)
                // =====================================
                else if (ticks == 79) {
                    groundCenter.getWorld().playSound(groundCenter, Sound.ENTITY_IRON_GOLEM_DEATH, 2.0f, 0.5f);

                    for (Entity e : groundCenter.getWorld().getNearbyEntities(groundCenter, radius, 8.0, radius)) {
                        if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                            LivingEntity target = (LivingEntity) e;

                            // 1. Phá nát mặt đất (Block Crack + Khói bụi) ngay dưới chân mục tiêu
                            Location smashLoc = target.getLocation();
                            smashLoc.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, smashLoc, 30, 0.5, 0.1, 0.5, 0, Material.DIRT.createBlockData());
                            smashLoc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, smashLoc, 10, 0.5, 0.1, 0.5, 0.05);

                            // 2. Ép rơi mạnh xuống đất
                            target.setVelocity(new Vector(0, -1.5, 0));

                            // 3. GÂY SÁT THƯƠNG DẬP XUỐNG (LẦN 2)
                            triggerDamage(player, rpgPlayer, target, blastMulti, pen);
                        }
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void drawGravityVortex(Location center, double radius) {
        double angle = (System.currentTimeMillis() / 50.0) % (2 * Math.PI);
        for (double r = radius; r > 0; r -= 1.0) {
            double x = Math.cos(angle + r) * r;
            double z = Math.sin(angle + r) * r;
            Location pLoc = center.clone().add(x, 0, z);

            pLoc.getWorld().spawnParticle(Particle.SQUID_INK, pLoc, 1, 0, 0.2, 0, 0.05);
            if (Math.random() > 0.7) {
                pLoc.getWorld().spawnParticle(Particle.NOTE, pLoc.clone().add(0, 0.5, 0), 0, 0.8, 0, 0, 1);
            }
        }
    }

    private void triggerDamage(Player player, RPGPlayer rpgPlayer, LivingEntity target, double multi, double pen) {
        double dmg = CombatManager.calculateDamage(player, rpgPlayer, target, multi, pen, true, false);
        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
        CombatManager.applySkillDamage(player, target, dmg);
        player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
    }
}