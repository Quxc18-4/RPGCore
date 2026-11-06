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

    @Override public String getId() { return "bard_maestro_state"; }
    @Override public String getName() { return "Trạng Thái: Nhạc Trưởng"; }
    @Override public String getRequiredClass() { return "bard"; }
    @Override public Material getIcon() { return Material.NETHER_STAR; }
    @Override public int getMaxLevel() { return 3; }
    @Override public int getUpgradeCost(int currentLevel) { return 2; }
    @Override public int getRequiredPlayerLevel() { return 55; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

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
                "§f  + §cCao (Nội Tại):§f Tăng 150% Sát thương đòn đánh/Âm."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        // Ép buộc người chơi phải đứng vững trên mặt đất
        if (!player.isOnGround()) {
            player.sendMessage("§c§l[!] §fPhải đứng vững trên mặt đất để tấu lên Khúc Nhạc Khải Huyền!");
            return false;
        }
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;

        // Trói chân 5 giây (100 ticks) vận chiêu
        StatusEffectManager.applyAbsoluteRoot(player, 100);

        // ==========================================
        // TÍCH HỢP TONE MODIFIER CHUẨN ĐÚNG LORE
        // ==========================================
        double highMulti = ToneModifier.getHighToneMultiplier(player);
        double radiusMulti = ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        double durationMulti = ToneModifier.getLowDurationMultiplier(player);
        int sharpCount = ToneModifier.getSharpToneCount(player);

        ToneManager.consumeTones(player, rpgPlayer);

        final double radius = 16.0 * radiusMulti;
        final int finalDuration = (int) Math.round((30 + (level * 6) + (level == 3 ? 2 : 0)) * durationMulti);
        final double penBonus = sharpCount * 10.0;
        final double drBuff = (level * 0.04) * highMulti;
        final double dmgBuff = (0.10 + (level * 0.05)) * highMulti;
        final double spdBuff = (0.30 + (level * 0.10)) * highMulti;
        final double critRate = (8 + (level * 4)) * highMulti;
        final double critDmg = (30 + (level * 10)) * highMulti;

        player.sendMessage("§d§l[TỐI THƯỢNG] §fKhúc Nhạc Khải Huyền... Bắt đầu tấu lên!");
        final Location castLoc = player.getLocation().clone();

        // ==========================================
        // KHỞI TẠO 50 HẠT ANTI-CLUMPING (CHỐNG DÍNH CHÙM)
        // ==========================================
        int numNotes = 50;
        double[] nx = new double[numNotes];
        double[] nz = new double[numNotes];
        double[] startY = new double[numNotes];
        double[] fallDist = new double[numNotes];
        Material[] crystalType = new Material[numNotes];

        for (int i = 0; i < numNotes; i++) {
            boolean valid = false;
            int attempts = 0;

            while (!valid && attempts < 30) {
                double randR = random.nextDouble() * (radius * 0.85);
                double angle = random.nextDouble() * 2 * Math.PI;
                double tx = castLoc.getX() + Math.cos(angle) * randR;
                double tz = castLoc.getZ() + Math.sin(angle) * randR;

                valid = true;
                for (int j = 0; j < i; j++) {
                    if (Math.hypot(tx - nx[j], tz - nz[j]) < 1.5) {
                        valid = false;
                        break;
                    }
                }

                if (valid || attempts == 29) {
                    nx[i] = tx;
                    nz[i] = tz;
                    startY[i] = castLoc.getY() + 10.0 + (random.nextDouble() * 6.0);
                    fallDist[i] = 3.0 + (random.nextDouble() * 3.0);
                    crystalType[i] = random.nextBoolean() ? Material.EMERALD_BLOCK : Material.DIAMOND_BLOCK;
                }
                attempts++;
            }
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }

                // ==========================================
                // PHASE 1: VẬN CHIÊU 5 GIÂY TẠI ĐIỂM CAST
                // ==========================================
                if (ticks < 100) {
                    if (ticks % 10 == 0) {
                        float pitch = 0.5f + (ticks / 100.0f) * 1.5f;
                        player.getWorld().playSound(castLoc, Sound.BLOCK_NOTE_BLOCK_HARP, 1.5f, pitch);
                    }
                    if (ticks == 60) {
                        player.getWorld().playSound(castLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 1.0f);
                    }

                    if (ticks % 3 == 0) {
                        Color ringColor = ticks < 60 ? Color.AQUA : Color.LIME;
                        double offset = ticks * 0.1;

                        for (double i = 0; i < 2 * Math.PI; i += Math.PI / 16) {
                            double x = Math.cos(i + offset) * radius;
                            double z = Math.sin(i + offset) * radius;
                            double waveY = Math.sin(i * 4.0 + ticks * 0.15) * 0.4;

                            Location particleLoc = castLoc.clone().add(x, 0.3 + waveY, z);
                            player.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(ringColor, 1.2f));
                        }
                    }

                    if (ticks % 4 == 0) {
                        double progress = Math.min(1.0, (double) ticks / 60.0);
                        double easeOut = 1.0 - Math.pow(1.0 - progress, 3);

                        for (int i = 0; i < numNotes; i++) {
                            double currentY = startY[i] - (fallDist[i] * easeOut);
                            player.getWorld().spawnParticle(Particle.END_ROD, nx[i], currentY, nz[i], 1, 0, 0, 0, 0);
                        }
                    }
                }

                // ==========================================
                // PHASE 2: BÙNG NỔ PHA LÊ & BẬT VÙNG THÁNH ĐỊA CỐ ĐỊNH
                // ==========================================
                else if (ticks == 100) {
                    player.getWorld().playSound(castLoc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 0.8f);
                    player.getWorld().playSound(castLoc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
                    player.getWorld().playSound(castLoc, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.5f);

                    for (int i = 0; i < numNotes; i++) {
                        double finalY = startY[i] - fallDist[i];
                        player.getWorld().spawnParticle(Particle.BLOCK, nx[i], finalY, nz[i], 10, 0.3, 0.3, 0.3, crystalType[i].createBlockData());
                        player.getWorld().spawnParticle(Particle.FIREWORK, nx[i], finalY, nz[i], 3, 0.1, 0.1, 0.1, 0.05);
                    }

                    player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, castLoc.clone().add(0, 1, 0), 300, radius / 1.2, 1.0, radius / 1.2, 0);

                    // TÁCH BIỆT: GỌI HÀM BURST HEAL (CÓ FATIGUE 300s) CHỈ TẠI TICK 100
                    for (Entity e : castLoc.getWorld().getNearbyEntities(castLoc, radius, radius / 2, radius)) {
                        // CHỈ CHẤP NHẬN ĐỒNG MINH (Bao gồm Caster và người cùng Party/Guild)
                        if (e instanceof Player && (e.equals(player) || !CombatManager.canTarget(player, e))) {
                            tryExecuteBurstHeal((Player) e);
                        }
                    }

                    startStaticMaestroZone(player, rpgPlayer, castLoc, finalDuration, radius, drBuff, dmgBuff, spdBuff, critRate, critDmg, penBonus);

                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);

        applyGlobalCooldown(player, rpgPlayer, level);
        return true;
    }

    // =====================================
    // HÀM XỬ LÝ BURST HEAL TẠI CHỖ (BỊ CHẶN BỞI FATIGUE)
    // =====================================
    private void tryExecuteBurstHeal(Player ally) {
        if (ally.hasMetadata(FATIGUE_META)) {
            if (System.currentTimeMillis() < ally.getMetadata(FATIGUE_META).get(0).asLong()) {
                return; // Đang bị Mệt mỏi -> Bỏ qua Hồi máu burst 100%
            }
        }

        AttributeInstance maxHpAttr = ally.getAttribute(Attribute.MAX_HEALTH);
        if (maxHpAttr != null) {
            ally.setHealth(maxHpAttr.getValue());
            ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0, 2, 0), 5, 0.5, 0.5, 0.5, 0);
        }
        StatusEffectManager.cleanseNegativeEffects(ally);

        // Cắm cờ Mệt mỏi 300s
        long fatigueExpireTime = System.currentTimeMillis() + (300 * 1000L);
        ally.setMetadata(FATIGUE_META, new FixedMetadataValue(RPGCore.getInstance(), fatigueExpireTime));
        ally.sendMessage("§a§l[TỐI THƯỢNG] §fKhải huyền giáng lâm! Khởi đầu và Kết Thúc!");
    }

    // =====================================
    // VÙNG THÁNH ĐỊA CỐ ĐỊNH (QUẢN LÝ ZONE AURA TRONG / NGOÀI)
    // =====================================
    private void startStaticMaestroZone(Player bard, RPGPlayer rpgPlayer, Location zoneCenter, int durationSeconds, double radius, double dr, double dmg, double spd, double cRate, double cDmg, double pen) {
        bard.setMetadata(ACTIVE_META, new FixedMetadataValue(RPGCore.getInstance(), true));

        // KHAI BÁO MẢNG LƯU TRỮ ĐỒNG MINH (KHÔNG BAO GIỜ GÁN LẠI = NEW, CHỈ ADD/CLEAR BÊN TRONG RUNNABLE ĐỂ GIỮ EFFECTIVELY FINAL)
        final java.util.Set<java.util.UUID> previousInsidePlayers = new java.util.HashSet<>();

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

                    // KẾT THÚC CHIÊU -> XÓA SẠCH BUFF CỦA NHỮNG ĐỒNG MINH CÒN SÓT TRONG VÒNG
                    for (java.util.UUID uuid : previousInsidePlayers) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null && p.isOnline()) clearAuraBuffs(p);
                    }
                    this.cancel();
                    return;
                }

                if (ticks % 5 == 0) {
                    Location headLoc = bard.getLocation().add(0, 2.2, 0);
                    for (double i = 0; i < 2 * Math.PI; i += Math.PI / 4) {
                        double x = Math.cos(i + (ticks * 0.1)) * 0.6;
                        double z = Math.sin(i + (ticks * 0.1)) * 0.6;
                        headLoc.getWorld().spawnParticle(Particle.END_ROD, headLoc.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                    }
                }

                if (ticks % 4 == 0) {
                    double offset = ticks * 0.1;
                    for (double i = 0; i < 2 * Math.PI; i += Math.PI / 16) {
                        double waveY = Math.sin(i * 4.0 + ticks * 0.2) * 0.3;
                        double x = Math.cos(i) * radius;
                        double z = Math.sin(i) * radius;

                        Location ringLoc = zoneCenter.clone().add(x, 0.3 + waveY, z);
                        zoneCenter.getWorld().spawnParticle(Particle.DUST, ringLoc, 1, new Particle.DustOptions(Color.LIME, 1.2f));

                        if (Math.random() < 0.15) {
                            zoneCenter.getWorld().spawnParticle(Particle.END_ROD, ringLoc.clone().add(0, 0.2, 0), 1, 0, 0.05, 0, 0);
                        }
                    }
                }

                if (ticks % 8 == 0) {
                    for (int j = 0; j < 3; j++) {
                        double randR = random.nextDouble() * radius;
                        double randAngle = random.nextDouble() * 2 * Math.PI;
                        double noteX = zoneCenter.getX() + Math.cos(randAngle) * randR;
                        double noteZ = zoneCenter.getZ() + Math.sin(randAngle) * randR;
                        zoneCenter.getWorld().spawnParticle(Particle.NOTE, noteX, zoneCenter.getY() + 0.2, noteZ, 0, random.nextDouble(), 0.5, 0, 1);
                    }
                }

                // ==============================================
                // QUẢN LÝ AURA ZONE (ĐI VÀO BẬT / ĐI RA TẮT NGAY LẬP TỨC)
                // ==============================================
                if (ticks % 10 == 0) {
                    // Mảng lưu người trong vòng hiện tại
                    java.util.Set<java.util.UUID> currentInsidePlayers = new java.util.HashSet<>();

                    for (Entity e : zoneCenter.getWorld().getNearbyEntities(zoneCenter, radius, radius / 2, radius)) {
                        // LỌC ĐỒNG MINH CHUẨN XÁC[cite: 10]
                        if (e instanceof Player && (e.equals(bard) || !CombatManager.canTarget(bard, e))) {
                            Player ally = (Player) e;
                            currentInsidePlayers.add(ally.getUniqueId());

                            // NẾU BƯỚC VÀO VÒNG LẦN ĐẦU -> CẤP BUFF CHỈ SỐ
                            if (!previousInsidePlayers.contains(ally.getUniqueId())) {
                                applyAuraBuffs(ally, dr, dmg, spd, cRate, cDmg, pen);
                            }

                            // NẾU ĐỨNG TRONG VÒNG -> HỒI 1% MÁU (Chu kỳ 20 ticks = 1 giây)
                            if (ticks % 20 == 0) {
                                AttributeInstance hpAttr = ally.getAttribute(Attribute.MAX_HEALTH);
                                if (hpAttr != null) {
                                    double regen = hpAttr.getValue() * 0.01;
                                    ally.setHealth(Math.min(hpAttr.getValue(), ally.getHealth() + regen));
                                    ally.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, ally.getLocation().add(0, 1, 0), 2, 0.3, 0.5, 0.3, 0);
                                }
                            }
                        }
                    }

                    // KIỂM TRA ĐỒNG MINH ĐÃ RỜI KHỎI VÒNG ĐỂ TƯỚC BỎ AURA
                    for (java.util.UUID uuid : previousInsidePlayers) {
                        if (!currentInsidePlayers.contains(uuid)) {
                            Player leaver = Bukkit.getPlayer(uuid);
                            if (leaver != null && leaver.isOnline()) {
                                clearAuraBuffs(leaver);
                            }
                        }
                    }

                    // [SỬA LỖI EFFECTIVELY FINAL BẰNG CÁCH UPDATE DATA, KHÔNG GÁN LẠI OBJECT MỚI][cite: 11]
                    previousInsidePlayers.clear();
                    previousInsidePlayers.addAll(currentInsidePlayers);
                }

                if (rpgPlayer.getCurrentResource() > 0) rpgPlayer.setCurrentResource(0);

                if (ticks % 90 == 0) {
                    List<ToneType> currentTones = ToneManager.getCurrentTones(bard);
                    if (currentTones.size() < maestroCap) {
                        ToneType[] values = ToneType.values();
                        currentTones.add(values[random.nextInt(values.length)]);

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
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // =====================================
    // HÀM GẮN AURA ZONE (KHÔNG LIÊN QUAN ĐẾN FATIGUE)
    // =====================================
    private void applyAuraBuffs(Player ally, double dr, double dmg, double spd, double cRate, double cDmg, double pen) {
        ally.setMetadata("MAESTRO_DR_BUFF", new FixedMetadataValue(RPGCore.getInstance(), dr));
        ally.setMetadata("MAESTRO_DMG_BUFF", new FixedMetadataValue(RPGCore.getInstance(), dmg));
        ally.setMetadata("MAESTRO_CRIT_BUFF", new FixedMetadataValue(RPGCore.getInstance(), cRate));
        ally.setMetadata("MAESTRO_CDMG_BUFF", new FixedMetadataValue(RPGCore.getInstance(), cDmg));
        if (pen > 0) ally.setMetadata("MAESTRO_PEN_BUFF", new FixedMetadataValue(RPGCore.getInstance(), pen));

        StatusEffectManager.applySpeed(ally, 10000, spd); // Đặt thời gian Speed cực dài, sẽ tự bị xóa bằng clearAuraBuffs khi ra khỏi vòng
        ally.playSound(ally.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 2.0f);
        ally.sendMessage("§a§l[Hệ Thống] §fBạn đã bước vào Giai Điệu Khải Hoàn!");
    }

    // =====================================
    // HÀM TƯỚC BỎ AURA NGAY LẬP TỨC KHI RA KHỎI VÒNG
    // =====================================
    private void clearAuraBuffs(Player ally) {
        ally.removeMetadata("MAESTRO_DR_BUFF", RPGCore.getInstance());
        ally.removeMetadata("MAESTRO_DMG_BUFF", RPGCore.getInstance());
        ally.removeMetadata("MAESTRO_CRIT_BUFF", RPGCore.getInstance());
        ally.removeMetadata("MAESTRO_CDMG_BUFF", RPGCore.getInstance());
        ally.removeMetadata("MAESTRO_PEN_BUFF", RPGCore.getInstance());

        ally.removePotionEffect(org.bukkit.potion.PotionEffectType.SPEED);

        RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(ally.getUniqueId());
        if (rpgPlayer != null) rpgPlayer.updateMoveSpeed();

        ally.playSound(ally.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.5f);
        ally.sendMessage("§8§l[Hệ Thống] §7Bạn đã rời khỏi Giai Điệu Khải Hoàn. Sức mạnh dần tan biến.");
    }
}