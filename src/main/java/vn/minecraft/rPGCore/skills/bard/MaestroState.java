package vn.minecraft.rPGCore.skills.bard;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MaestroState implements ISkill {

    private static boolean isModifierRegistered = false;
    private final String FATIGUE_META = "BARD_MAESTRO_FATIGUE";
    public static final String ACTIVE_META = "BARD_MAESTRO_ACTIVE";
    private final Random random = new Random();

    public MaestroState() {
        if (!isModifierRegistered) {
            CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
                @Override
                public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                    if (victim.hasMetadata("MAESTRO_DR_BUFF")) {
                        return victim.getMetadata("MAESTRO_DR_BUFF").get(0).asDouble();
                    }
                    return 0.0;
                }
                @Override
                public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity victim) {
                    if (attacker.hasMetadata("MAESTRO_DMG_BUFF")) {
                        return attacker.getMetadata("MAESTRO_DMG_BUFF").get(0).asDouble();
                    }
                    return 0.0;
                }
            });

            CombatManager.registerStatModifier(new CombatManager.IStatModifier() {
                @Override
                public void applyBonusStats(Player player, Map<String, Double> stats) {
                    if (player.hasMetadata("MAESTRO_CRIT_BUFF")) {
                        stats.put("Tỉ lệ chí mạng", stats.getOrDefault("Tỉ lệ chí mạng", 0.0) + player.getMetadata("MAESTRO_CRIT_BUFF").get(0).asDouble());
                    }
                    if (player.hasMetadata("MAESTRO_CDMG_BUFF")) {
                        stats.put("Sát thương chí mạng", stats.getOrDefault("Sát thương chí mạng", 0.0) + player.getMetadata("MAESTRO_CDMG_BUFF").get(0).asDouble());
                    }
                    if (player.hasMetadata("MAESTRO_PEN_BUFF")) {
                        double pen = player.getMetadata("MAESTRO_PEN_BUFF").get(0).asDouble();
                        stats.put("Tỉ lệ xuyên giáp", stats.getOrDefault("Tỉ lệ xuyên giáp", 0.0) + pen);
                        stats.put("Tỉ lệ xuyên kháng phép", stats.getOrDefault("Tỉ lệ xuyên kháng phép", 0.0) + pen);
                    }
                }
            });
            isModifierRegistered = true;
        }
    }

    @Override
    public String getId() { return "bard_maestro_state"; }

    @Override
    public String getName() { return "Trạng Thái: Nhạc Trưởng"; }

    @Override
    public String getRequiredClass() { return "bard"; }

    @Override
    public Material getIcon() { return Material.NETHER_STAR; }

    @Override
    public int getMaxLevel() { return 3; }

    @Override
    public int getUpgradeCost(int currentLevel) { return 2; }

    @Override
    public int getRequiredPlayerLevel() { return 60; }

    @Override
    public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) {
        return 220.0 + (level * 30.0);
    }

    @Override
    public long getCooldown(int level) {
        return (long) ((380.0 - (level * 20.0)) * 1000);
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLevel = Math.max(1, level);

        int baseDr = displayLevel * 4;
        int baseDmg = 10 + (displayLevel * 5);
        int baseSpd = 30 + (displayLevel * 10);
        int baseCrit = 8 + (displayLevel * 4);
        int baseCdmg = 30 + (displayLevel * 10);
        int duration = 30 + (displayLevel * 6) + (displayLevel == 3 ? 2 : 0);

        return Arrays.asList(
                "§7Kỹ năng Tối Thượng.",
                "§7Hy sinh toàn bộ Năng lượng để mở ra một",
                "§7bản giao hưởng rực rỡ nhất.",
                "",
                "§8▪ §fLoại Kỹ Năng: §d[ Tối Thượng ]",
                "§8▪ §fPhạm vi cơ bản: §e16 Block",
                "",
                "§c[Thi Triển - Nhạc Trưởng]",
                "§f- Hồi 100% HP và Xóa Debuff cho đồng minh",
                "§f  (Và bản thân) trong phạm vi.",
                "§f- Trong §e" + duration + "s§f, lượng Mana của bạn bị §cÉp về 0§f.",
                "§f- Đồng minh nhận Buff sẽ bị Khóa không thể",
                "§f  nhận Hào Quang Nhạc Trưởng lần nữa trong §e300s§f.",
                "§f- §a-" + baseDr + "% ST Nhận Vào§f | §c+" + baseDmg + "% ST Gây Ra",
                "§f  §e+" + baseSpd + "% Tốc chạy§f | §c+" + baseCrit + "% Tỉ lệ Crit",
                "§f  §c+" + baseCdmg + "% ST Crit§f | §aHồi 1% Max HP/s",
                "",
                "§d[Đòn Đánh Tấu Nhạc]",
                "§f- Mỗi §e4.5s§f nhận ngẫu nhiên 1 Âm. Đánh thường",
                "§f  tiêu hao 1 Âm để kích hoạt Kỹ Năng Chìm:",
                "§f  + §bVang:§f Sóng âm 360 độ | + §eSắc:§f Laser xuyên thấu.",
                "§f  + §8Trầm:§f Hố đen hút quái & Stun.",
                "§f  + §cCao (Nội Tại):§f Tăng 150% Sát thương đòn đánh/Âm.",
                "",
                "§d[Tương Tác Thi Triển] (Tiêu hao toàn bộ Âm lúc kích hoạt)",
                "§f- §cCao:§f +15% Buff/Âm | §bVang:§f +4 Block/Âm",
                "§f- §8Trầm:§f +30% Thời gian/Âm | §eSắc:§f +10% Xuyên/Âm."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;

        // [MỚI]: Trói chân (Absolute Root) Nhạc Trưởng trong đúng 3 giây đầu
        StatusEffectManager.applyAbsoluteRoot(player, 60);

        List<ToneType> tones = ToneManager.getCurrentTones(player);
        int highCount = 0, echoCount = 0, lowCount = 0, sharpCount = 0;

        for (ToneType t : tones) {
            switch (t) {
                case HIGH: highCount++; break;
                case ECHO: echoCount++; break;
                case LOW: lowCount++; break;
                case SHARP: sharpCount++; break;
            }
        }
        ToneManager.consumeTones(player, rpgPlayer);

        double highMulti = 1.0 + (highCount * 0.15);
        double lowMulti = 1.0 + (lowCount * 0.30);

        final double radius = 16.0 + (echoCount * 4.0);
        final int finalDuration = (int) Math.round((30 + (level * 6) + (level == 3 ? 2 : 0)) * lowMulti);
        final double penBonus = sharpCount * 10.0;
        final double drBuff = (level * 0.04) * highMulti;
        final double dmgBuff = (0.10 + (level * 0.05)) * highMulti;
        final double spdBuff = (0.30 + (level * 0.10)) * highMulti;
        final double critRate = (8 + (level * 4)) * highMulti;
        final double critDmg = (30 + (level * 10)) * highMulti;

        // [CẬP NHẬT MESSAGE THEO YÊU CẦU]
        player.sendMessage("§d§l[TỐI THƯỢNG] §fKhúc Nhạc Khải Huyền... Bắt đầu tấu lên!");
        final org.bukkit.Location castLoc = player.getLocation().clone();

        int numNotes = 25;
        double[] nx = new double[numNotes];
        double[] nz = new double[numNotes];
        double[] startY = new double[numNotes]; // Điểm bắt đầu rơi
        double[] fallDist = new double[numNotes]; // Quãng đường sẽ rơi (3 đến 6 block)

        for (int i = 0; i < numNotes; i++) {
            nx[i] = castLoc.getX() + (random.nextDouble() - 0.5) * (radius * 1.5);
            nz[i] = castLoc.getZ() + (random.nextDouble() - 0.5) * (radius * 1.5);

            // Bắt đầu từ trên cao: Cách mặt đất 10 -> 12 block
            startY[i] = castLoc.getY() + 10.0 + (random.nextDouble() * 2.0);

            // Quãng đường rơi: Từ 3 -> 6 block.
            // => Điểm dừng chắc chắn sẽ lơ lửng ở độ cao từ (10-6=4) đến (12-3=9) block so với mặt đất!
            fallDist[i] = 3.0 + (random.nextDouble() * 3.0);
        }

        // Không dùng object thừa để tránh Memory Leak[cite: 19]
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }

                // ==========================================
                // PHASE 1: RỚT & CHỜ NỔ (Tổng 5 giây = 100 ticks)
                // ==========================================
                if (ticks < 100) {

                    // 1. Âm thanh đếm nhịp
                    if (ticks % 10 == 0) {
                        float pitch = 0.5f + (ticks / 100.0f) * 1.5f;
                        player.getWorld().playSound(castLoc, org.bukkit.Sound.BLOCK_NOTE_BLOCK_HARP, 1.5f, pitch);
                    }
                    if (ticks == 60) {
                        // Tiếng nạp năng lượng ở 2 giây cuối
                        player.getWorld().playSound(castLoc, org.bukkit.Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 1.0f);
                    }

                    // 2. Vòng tròn sáng pha lê dưới mặt đất (Quay tít)
                    if (ticks % 4 == 0) {
                        org.bukkit.Color ringColor = ticks < 60 ? org.bukkit.Color.AQUA : org.bukkit.Color.WHITE;
                        double offset = (ticks * Math.PI) / 30;

                        for (double i = 0; i < 2 * Math.PI; i += Math.PI / 8) {
                            double x = Math.cos(i + offset) * radius;
                            double z = Math.sin(i + offset) * radius;
                            player.getWorld().spawnParticle(org.bukkit.Particle.DUST, castLoc.clone().add(x, 0.2, z), 1, new org.bukkit.Particle.DustOptions(ringColor, 1.2f));
                        }
                    }

                    // 3. VẬT LÝ HẠT (TOÁN HỌC CUBIC EASE-OUT)
                    // Render mỗi 4 tick để hạt không tạo thành vệt dài
                    if (ticks % 4 == 0) {
                        // Tính % thời gian rơi (Chỉ rơi trong 60 tick đầu, max là 1.0)
                        double progress = Math.min(1.0, (double) ticks / 60.0);

                        // Công thức hãm phanh: Lúc đầu nhanh, lúc sau mượt
                        double easeOut = 1.0 - Math.pow(1.0 - progress, 3);

                        for (int i = 0; i < numNotes; i++) {
                            // Tọa độ Y hiện tại = Điểm bắt đầu - (Quãng đường * % hoàn thành)
                            double currentY = startY[i] - (fallDist[i] * easeOut);

                            // Render hạt End_Rod chói sáng tại vị trí lơ lửng
                            player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, nx[i], currentY, nz[i], 1, 0, 0, 0, 0);
                        }
                    }
                }

                // ==========================================
                // PHASE 2: BÙNG NỔ PHA LÊ HỒI PHỤC (Tick 100)
                // ==========================================
                else if (ticks == 100) {
                    player.getWorld().playSound(castLoc, org.bukkit.Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 0.8f);
                    player.getWorld().playSound(castLoc, org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.5f);

                    for (int i = 0; i < numNotes; i++) {
                        // Lấy lại điểm Y cuối cùng (khi easeOut = 1.0) để phát nổ
                        double finalY = startY[i] - fallDist[i];

                        player.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, nx[i], finalY, nz[i], 8, 0.2, 0.2, 0.2, org.bukkit.Material.AMETHYST_BLOCK.createBlockData());
                        player.getWorld().spawnParticle(org.bukkit.Particle.FIREWORK, nx[i], finalY, nz[i], 3, 0.1, 0.1, 0.1, 0.05);
                    }

                    player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, castLoc.clone().add(0, 1, 0), 200, radius/1.5, 1.0, radius/1.5, 0);
                    player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, castLoc.clone().add(0, 1, 0), 100, radius/2, 1.0, radius/2, 0.1);

                    // Quét đồng minh & buff (Check CombatManager chuẩn rule)[cite: 19]
                    for (org.bukkit.entity.Entity e : castLoc.getWorld().getNearbyEntities(castLoc, radius, radius/2, radius)) {
                        if (e instanceof Player && !e.equals(player) && !CombatManager.canTarget(player, e)) {
                            Player ally = (Player) e;
                            if (canReceiveMaestroBuff(ally)) {
                                applyHeal(ally);
                                applyBuff(player, ally, finalDuration, drBuff, dmgBuff, spdBuff, critRate, critDmg, penBonus);
                            }
                        }
                    }

                    // Buff cho chính Bard[cite: 18]
                    if (canReceiveMaestroBuff(player)) {
                        applyHeal(player);
                        applyBuff(player, player, finalDuration, drBuff, dmgBuff, spdBuff, critRate, critDmg, penBonus);
                    }

                    startMaestroState(player, rpgPlayer, finalDuration, radius, drBuff, dmgBuff, spdBuff, critRate, critDmg, penBonus);

                    this.cancel();
                }

                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);

        applyGlobalCooldown(player, rpgPlayer, level);
        return true;
    }

    // =====================================
    // HÀM KIỂM TRA CHỐNG SPAM (FATIGUE)
    // =====================================
    private boolean canReceiveMaestroBuff(Player p) {
        if (!p.hasMetadata(FATIGUE_META)) return true;
        long expire = p.getMetadata(FATIGUE_META).get(0).asLong();
        return System.currentTimeMillis() >= expire;
    }

    // =====================================
    // HÀM HỒI MÁU (CHỈ XẢY RA 1 LẦN CỐ ĐỊNH LÚC CAST)
    // =====================================
    private void applyHeal(Player ally) {
        AttributeInstance maxHpAttr = ally.getAttribute(Attribute.MAX_HEALTH);
        if (maxHpAttr != null) {
            ally.setHealth(maxHpAttr.getValue());
            ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0, 2, 0), 5, 0.5, 0.5, 0.5, 0);
        }
        StatusEffectManager.cleanseNegativeEffects(ally);
    }

    // =====================================
    // HÀM CẤP BUFF (KÈM THEO ÁP FATIGUE 300S)
    // =====================================
    private void applyBuff(Player bard, Player ally, int durationSeconds, double dr, double dmg, double spd, double cRate, double cDmg, double pen) {
        long fatigueExpireTime = System.currentTimeMillis() + (300 * 1000L); // Khóa không nhận lần nữa trong 300s
        ally.setMetadata(FATIGUE_META, new FixedMetadataValue(RPGCore.getInstance(), fatigueExpireTime));

        ally.setMetadata("MAESTRO_DR_BUFF", new FixedMetadataValue(RPGCore.getInstance(), dr));
        ally.setMetadata("MAESTRO_DMG_BUFF", new FixedMetadataValue(RPGCore.getInstance(), dmg));
        ally.setMetadata("MAESTRO_CRIT_BUFF", new FixedMetadataValue(RPGCore.getInstance(), cRate));
        ally.setMetadata("MAESTRO_CDMG_BUFF", new FixedMetadataValue(RPGCore.getInstance(), cDmg));
        if (pen > 0) ally.setMetadata("MAESTRO_PEN_BUFF", new FixedMetadataValue(RPGCore.getInstance(), pen));

        StatusEffectManager.applySpeed(ally, durationSeconds * 20, spd);
        ally.sendMessage("§a§l[Maestro] §fTâm hồn rực cháy! Toàn bộ chỉ số được thăng hoa!");
        ally.playSound(ally.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        // Hồi 1% máu mỗi giây cho người nhận buff
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = durationSeconds * 20;

            @Override
            public void run() {
                if (!ally.isOnline() || ally.isDead() || ticks >= maxTicks) {
                    clearBuffs(ally);
                    this.cancel();
                    return;
                }
                ticks++;
                if (ticks % 20 == 0) {
                    AttributeInstance hpAttr = ally.getAttribute(Attribute.MAX_HEALTH);
                    if (hpAttr != null) {
                        double regen = hpAttr.getValue() * 0.01;
                        ally.setHealth(Math.min(hpAttr.getValue(), ally.getHealth() + regen));
                        ally.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, ally.getLocation().add(0, 1, 0), 2, 0.3, 0.5, 0.3, 0);
                    }
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void clearBuffs(Player ally) {
        if (ally.isOnline()) {
            ally.removeMetadata("MAESTRO_DR_BUFF", RPGCore.getInstance());
            ally.removeMetadata("MAESTRO_DMG_BUFF", RPGCore.getInstance());
            ally.removeMetadata("MAESTRO_CRIT_BUFF", RPGCore.getInstance());
            ally.removeMetadata("MAESTRO_CDMG_BUFF", RPGCore.getInstance());
            ally.removeMetadata("MAESTRO_PEN_BUFF", RPGCore.getInstance());

            RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(ally.getUniqueId());
            if (rpgPlayer != null) rpgPlayer.updateMoveSpeed();
            ally.sendMessage("§8§l[Maestro] §7Bản giao hưởng đã kết thúc.");
        }
    }

    // =====================================
    // BẬT TRẠNG THÁI NHẠC TRƯỞNG & VÒNG BUFF DI ĐỘNG
    // =====================================
    private void startMaestroState(Player bard, RPGPlayer rpgPlayer, int durationSeconds, double radius, double dr, double dmg, double spd, double cRate, double cDmg, double pen) {
        bard.setMetadata(ACTIVE_META, new FixedMetadataValue(RPGCore.getInstance(), true));

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = durationSeconds * 20;
            final boolean hasCore8 = rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_8") > 0;
            final int maestroCap = hasCore8 ? 4 : 3;

            @Override
            public void run() {
                if (!bard.isOnline() || bard.isDead() || ticks >= maxTicks) {
                    bard.removeMetadata(ACTIVE_META, RPGCore.getInstance());
                    bard.removeMetadata(ToneManager.META_TONES_LIST, RPGCore.getInstance());
                    bard.setMetadata(ToneManager.META_TONE_COUNT, new FixedMetadataValue(RPGCore.getInstance(), 0));
                    rpgPlayer.updateMoveSpeed();
                    ToneManager.sendToneActionBar(bard, new java.util.ArrayList<>());

                    this.cancel();
                    return;
                }

                // TÍNH NĂNG MỚI: VÒNG BUFF DI ĐỘNG THEO NGƯỜI (Cập nhật mỗi 0.5s)
                if (ticks % 10 == 0) {
                    Location currentLoc = bard.getLocation();

                    // Vẽ vòng sáng chạy theo chân Bard
                    for (double i = 0; i < 2 * Math.PI; i += Math.PI / 8) { // Mật độ hạt thưa hơn để tránh lag
                        double x = Math.cos(i) * radius;
                        double z = Math.sin(i) * radius;
                        bard.getWorld().spawnParticle(Particle.DUST, currentLoc.clone().add(x, 0.2, z), 1, new Particle.DustOptions(Color.AQUA, 1.0f));
                    }

                    // Tự động cấp Buff cho những đồng minh chạy vào vùng di động
                    for (Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, radius, radius/2, radius)) {
                        if (e instanceof Player && !e.equals(bard) && !CombatManager.canTarget(bard, e)) {
                            Player ally = (Player) e;
                            if (canReceiveMaestroBuff(ally)) {
                                // Chỉ cấp Buff, KHÔNG Hồi Máu (vì hồi máu đã cố định ở tâm cast rồi)
                                applyBuff(bard, ally, durationSeconds, dr, dmg, spd, cRate, cDmg, pen);
                            }
                        }
                    }
                }

                ticks++;
                if (rpgPlayer.getCurrentResource() > 0) {
                    rpgPlayer.setCurrentResource(0);
                }

                // Sạc đạn Nhạc Trưởng mỗi 4.5s
                if (ticks % 90 == 0) {
                    List<ToneType> currentTones = ToneManager.getCurrentTones(bard);
                    if (currentTones.size() < maestroCap) {
                        ToneType[] values = ToneType.values();
                        ToneType randomTone = values[random.nextInt(values.length)];

                        currentTones.add(randomTone);
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < currentTones.size(); i++) {
                            sb.append(currentTones.get(i).name());
                            if (i < currentTones.size() - 1) sb.append(",");
                        }
                        bard.setMetadata(ToneManager.META_TONES_LIST, new FixedMetadataValue(RPGCore.getInstance(), sb.toString()));
                        bard.setMetadata(ToneManager.META_TONE_COUNT, new FixedMetadataValue(RPGCore.getInstance(), currentTones.size()));
                        rpgPlayer.updateMoveSpeed();
                        ToneManager.sendToneActionBar(bard, currentTones);

                        bard.playSound(bard.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
                    }
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}