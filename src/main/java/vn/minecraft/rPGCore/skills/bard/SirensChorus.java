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

        int tickDmg = 30 + (displayLevel * 10);
        int launchDmg = 60 + (displayLevel * 20);
        int blastDmg = 100 + (displayLevel * 30);

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
                "§f- §e[Hút Hồn]:§f Liên tục hút kẻ địch vào tâm trong",
                "§f  1.5s, gây §c" + tickDmg + "% ST Phép§f mỗi 0.5s.",
                "§f- §e[Bùng Nổ]:§f Đột ngột hất tung mục tiêu lên không,",
                "§f  gây §c" + launchDmg + "% ST Phép§f và giam giữ lơ lửng.",
                "§f- §e[Hạ Màn]:§f Hố đen bùng nổ, hất văng mọi thứ",
                "§f  ra xa và gây §c" + blastDmg + "% ST Phép§f."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false; //[cite: 5, 6]

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

        final double tickMulti = (0.30 + (level * 0.10)) * highToneMultiplier;
        final double launchMulti = (0.60 + (level * 0.20)) * highToneMultiplier;
        final double blastMulti = (1.00 + (level * 0.30)) * highToneMultiplier;

        final double extraMagicPen = sharpToneCount * 10.0;
        final double radius = 6.0 * echoRadiusMultiplier;

        Location startLoc = player.getEyeLocation();
        Vector velocity = startLoc.getDirection().multiply(1.5);
        player.getWorld().playSound(startLoc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.0f, 0.5f);

        new BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            int flightTicks = 0;

            @Override
            public void run() {
                flightTicks++;
                velocity.setY(velocity.getY() - 0.06);
                currentLoc.add(velocity);

                currentLoc.getWorld().spawnParticle(Particle.NOTE, currentLoc, 1, 0.1, 0.1, 0.1, 1);
                currentLoc.getWorld().spawnParticle(Particle.WITCH, currentLoc, 2, 0.1, 0.1, 0.1, 0);

                boolean hitGround = currentLoc.getBlock().getType().isSolid();
                boolean hitEntity = false;

                if (!hitGround) {
                    for (Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                        if (e instanceof LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)) { //[cite: 5, 6]
                            hitEntity = true; break;
                        }
                    }
                }

                if (hitGround || hitEntity || flightTicks > 80) {
                    this.cancel();
                    while (currentLoc.getBlock().getType().isSolid() && currentLoc.getY() < 256) {
                        currentLoc.add(0, 0.5, 0);
                    }
                    startBlackHoleSequence(player, rpgPlayer, currentLoc, radius, tickMulti, launchMulti, blastMulti, extraMagicPen, isMelodyTriggered);
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);

        applyGlobalCooldown(player, rpgPlayer, level); //[cite: 5]
        return true;
    }

    private void startBlackHoleSequence(Player player, RPGPlayer rpgPlayer, Location center, double radius, double tickMulti, double launchMulti, double blastMulti, double pen, boolean isMelodyTriggered) {

        Location groundCenter = center.clone();
        groundCenter.setY(Math.floor(groundCenter.getY()) + 0.15);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                // PHASE 3: HÌNH THÀNH (10 Ticks)
                if (ticks <= 10) {
                    if (ticks == 1) groundCenter.getWorld().playSound(groundCenter, Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 0.5f);
                    MelodyUtils.drawExpandingAura(groundCenter, radius, org.bukkit.Color.AQUA, Particle.ENCHANT);
                }

                // PHASE 4: HÚT THEO PHƯƠNG NGANG (Từ Tick 11 -> 40)
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

                    drawMusicalVortex(groundCenter, radius);

                    for (Entity e : groundCenter.getWorld().getNearbyEntities(groundCenter, radius, 4.0, radius)) {
                        if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) { //[cite: 5, 6]
                            LivingEntity target = (LivingEntity) e;

                            // [FIX] HÚT PHƯƠNG NGANG TUYỆT ĐỐI
                            Vector pullDir = groundCenter.toVector().subtract(target.getLocation().toVector());
                            pullDir.setY(0); // Triệt tiêu trục Y để quái lê trên mặt đất

                            if (pullDir.lengthSquared() > 0.5) {
                                // Nếu chạm đất thì cho nảy cực nhẹ (0.1) để không kẹt viền block, nếu không thì Y = 0
                                double yOffset = target.isOnGround() ? 0.1 : 0.0;
                                target.setVelocity(pullDir.normalize().multiply(0.45).setY(yOffset));
                            }

                            if (ticks % 10 == 0) {
                                triggerDamage(player, rpgPlayer, target, tickMulti, pen);
                            }
                        }
                    }
                }

                // PHASE 5: HẤT TUNG MẠNH DỨT KHOÁT (Tick 41)
                else if (ticks == 41) {
                    groundCenter.getWorld().playSound(groundCenter, Sound.ENTITY_WARDEN_ROAR, 1.0f, 1.5f);
                    groundCenter.getWorld().spawnParticle(Particle.SONIC_BOOM, groundCenter.clone().add(0, 1, 0), 1);

                    for (Entity e : groundCenter.getWorld().getNearbyEntities(groundCenter, radius, 4.0, radius)) {
                        if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) { //[cite: 5, 6]
                            LivingEntity target = (LivingEntity) e;

                            // [FIX] Lực hất 1.0 (Đạt đỉnh trong vòng ~7 Ticks / 0.35s)
                            target.setVelocity(new Vector(0, 1.0, 0));
                            triggerDamage(player, rpgPlayer, target, launchMulti, pen);
                        }
                    }
                }

                // PHASE 6: LƠ LỬNG TRÊN KHÔNG (Tick 48 -> 90)
                // Chờ đúng 7 ticks sau khi hất để quái đạt ĐỈNH, sau đó khóa ngay lập tức!
                else if (ticks >= 48 && ticks <= 90) {
                    for (Entity e : groundCenter.getWorld().getNearbyEntities(groundCenter, radius, 8.0, radius)) {
                        if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) { //[cite: 5, 6]
                            Vector v = e.getVelocity();
                            // [FIX] Y = 0.04 triệt tiêu hoàn toàn trọng lực của Minecraft, quái bị giam đứng yên!
                            e.setVelocity(new Vector(v.getX() * 0.5, 0.04, v.getZ() * 0.5));
                        }
                    }
                }

                // PHASE 7: NỔ VĂNG (Tick 91)
                else if (ticks >= 91) {
                    groundCenter.getWorld().playSound(groundCenter, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
                    groundCenter.getWorld().spawnParticle(Particle.EXPLOSION, groundCenter.clone().add(0, 3, 0), 3, 1, 1, 1, 0.1);

                    for (Entity e : groundCenter.getWorld().getNearbyEntities(groundCenter, radius, 8.0, radius)) {
                        if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) { //[cite: 5, 6]
                            LivingEntity target = (LivingEntity) e;

                            Vector pushOut = target.getLocation().toVector().subtract(groundCenter.toVector()).normalize();
                            target.setVelocity(pushOut.multiply(1.8).setY(0.4));

                            triggerDamage(player, rpgPlayer, target, blastMulti, pen);
                        }
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

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

    private void triggerDamage(Player player, RPGPlayer rpgPlayer, LivingEntity target, double multi, double pen) {
        double dmg = CombatManager.calculateDamage(player, rpgPlayer, target, multi, pen, true, false); //[cite: 6]
        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true)); //[cite: 6]
        CombatManager.applySkillDamage(player, target, dmg); //[cite: 6]
        player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance()); //[cite: 6]
    }
}