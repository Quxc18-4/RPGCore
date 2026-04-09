package vn.minecraft.rPGCore.skills.warrior;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.AbstractEnhancementSkill;

import java.util.*;

public class WarriorJudgmentDomain extends AbstractEnhancementSkill implements Listener {

    public static final Map<UUID, DomainData> activeDomains = new HashMap<>();
    private final List<InheritanceOrbData> activeOrbs = new ArrayList<>();

    public WarriorJudgmentDomain(JavaPlugin plugin) {
        super(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startOrbPickupTask();

        // [CHUẨN SOLID]: TỰ ĐĂNG KÝ CÁCH ĐỌC BUFF CỦA RIÊNG KỸ NĂNG NÀY VÀO CORE
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity victim) {
                double bonus = 0.0;
                // Nếu mình là đồng minh đứng trong vòng -> Tăng sát thương gây ra
                if (attacker.hasMetadata("Domain_Ally_Buff")) bonus += attacker.getMetadata("Domain_Ally_Buff").get(0).asDouble();
                // Nếu mình là quái bị dính debuff vòng -> Giảm sát thương đánh trả
                if (attacker.hasMetadata("Domain_Enemy_Debuff")) bonus -= attacker.getMetadata("Domain_Enemy_Debuff").get(0).asDouble();
                return bonus;
            }

            @Override
            public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                double reduction = 0.0;
                // Nếu mình là đồng minh -> Nhận ít sát thương hơn
                if (victim.hasMetadata("Domain_Ally_Buff")) reduction += victim.getMetadata("Domain_Ally_Buff").get(0).asDouble();
                // Nếu thằng đánh mình là quái bị dính debuff vòng -> Sát thương của nó tới mình bị giảm
                if (attacker != null && attacker.hasMetadata("Domain_Enemy_Debuff")) reduction += attacker.getMetadata("Domain_Enemy_Debuff").get(0).asDouble();
                return reduction;
            }
        });

        // 2. [CHUẨN SOLID]: HOOK CHỈ SỐ TĨNH (Tốc Chạy)
        CombatManager.registerStatModifier(new CombatManager.IStatModifier() {
            @Override
            public void applyBonusStats(Player player, Map<String, Double> stats) {
                // Tự Kỹ năng này kiểm tra Metadata của chính nó
                if (player.hasMetadata("Domain_Ally_Buff") && player.hasMetadata("Domain_Ally_Expire")) {
                    if (System.currentTimeMillis() <= player.getMetadata("Domain_Ally_Expire").get(0).asLong()) {
                        double buff = player.getMetadata("Domain_Ally_Buff").get(0).asDouble();

                        // Buff 0.15 -> +15 Tốc chạy
                        stats.put("Tốc độ di chuyển", stats.getOrDefault("Tốc độ di chuyển", 0.0) + (buff * 100.0));
                    } else {
                        // Nếu hết hạn thì dọn dẹp rác luôn
                        player.removeMetadata("Domain_Ally_Buff", RPGCore.getInstance());
                        player.removeMetadata("Domain_Ally_Expire", RPGCore.getInstance());
                    }
                }
            }
        });
    }

    @Override public String getId() { return "warrior_judgment_domain"; }
    @Override public String getName() { return "§e§lThánh Vực Phán Quyết"; }
    @Override public String getRequiredClass() { return "warrior"; }
    @Override public Material getIcon() { return Material.BEACON; }
    @Override public int getMaxLevel() { return 3; }
    @Override public int getUpgradeCost(int currentLevel) { return 2; }
    @Override public int getRequiredPlayerLevel() { return 45; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }
    @Override public boolean isPassiveSkill() { return false; }
    @Override public double getManaCost(int level) { return 110.0; }
    @Override public long getCooldown(int level) { return 75000L; }

    // [CẬP NHẬT]: MÔ TẢ ĐẦY ĐỦ CHI TIẾT VÀ CHỈ SỐ ĐỘNG THEO LEVEL
    @Override
    public List<String> getDescription(int level) {
        int radius = 12 + (level * 4); // Cấp 1: 16m | Cấp 2: 20m | Cấp 3: 24m
        int buffRate = 5 + (level * 5); // Cấp 1: 10% | Cấp 2: 15% | Cấp 3: 20%
        double awakenRate = buffRate * 1.5; // Cấp 1: 15% | Cấp 2: 22.5% | Cấp 3: 30%
        // [MỚI]: Biến hiển thị sát thương Lôi Phạt (600%, 800%, 1000%)
        int strikeDmg = 400 + (level * 200);

        return Arrays.asList(
                "§7Thiết lập một Thánh Vực khổng lồ để tăng cường sức mạnh cho đồng minh.",
                "§7Bán kính lãnh vực: §e" + radius + " block§7. Niệm chú tại chỗ.",
                "",
                "§e[Kích hoạt: Thánh Vực]",
                "§f- Hút thần lực §a5s§f. §bKháng 100% Đẩy lùi§f. Chịu tối đa §c3 §fđòn tấn công.",
                "§f- §aĐồng minh§f: Tăng §e" + buffRate + "% Sát thương §fvà §b" + buffRate + "% Tốc chạy§f.",
                "§f- §cKẻ địch§f: Bị áp chế, giảm §c" + buffRate + "% Sát thương §fgây ra.",
                "",
                "§c[Thức Tỉnh Kỹ Năng]",
                "§f- Thời gian niệm chú giảm §a50%§f. Chịu tối đa §c5 §fđòn tấn công.",
                "§f- Hiệu quả Buff/Debuff tăng cường lên §e" + String.format("%.1f", awakenRate) + "%§f.",
                "§f- Mỗi §c5 kẻ địch bị giết§f trong lãnh vực, tích trữ §e1 Mảnh Sấm§f.",
                "§f- Khi lãnh vực khép lại: Dựng Ma Trận Thiên Lôi giáng phạt",
                "§f  vào kẻ địch ngẫu nhiên (Làm tê liệt và gây §c\" + strikeDmg + \"% Sát thương§f).",
                "",
                "§b[Mảnh Sấm Sét] §8(Tối đa 8 tích trữ)",
                "§f- Chém thường tiêu hao 1 đơn vị tích trữ, dẫn sấm sét vào lưỡi kiếm",
                "§f  giật sét lan (Max 3 mục tiêu), gây §c145% Sát thương AOE§f."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey)) {
            if (System.currentTimeMillis() < player.getMetadata(cdKey).get(0).asLong()) {
                player.sendMessage("§cKỹ năng đang hồi chiêu!");
                return false;
            }
        }

        double cost = getManaCost(level);
        boolean isEmpowered = rpgPlayer.getCurrentResource() >= cost;

        if (isEmpowered) {
            rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        }

        final int channelTicks = isEmpowered ? 50 : 100;
        final int maxHits = isEmpowered ? 5 : 3;

        StatusEffectManager.applyAbsoluteRoot(player, channelTicks);

        player.setMetadata("Domain_Channeling", new FixedMetadataValue(plugin, true));
        player.setMetadata("Domain_HitCount", new FixedMetadataValue(plugin, 0));

        player.sendMessage("§e§l[!] §fĐang hiệu triệu Thánh Vực...");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 0.5f);

        // GIAI ĐOẠN 1: HÚT NĂNG LƯỢNG MẠNH MẼ
        new BukkitRunnable() {
            int ticks = 0;
            final Particle.DustOptions goldDust = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.0f);

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !player.hasMetadata("Domain_Channeling")) {
                    cancelChanneling(player, true);
                    this.cancel();
                    return;
                }

                int hitsTaken = player.getMetadata("Domain_HitCount").get(0).asInt();
                if (hitsTaken >= maxHits) {
                    player.sendMessage("§c§l[!] §fNiệm chú bị ngắt quãng!");
                    cancelChanneling(player, false);
                    this.cancel();
                    return;
                }

                Location loc = player.getLocation();

                // NÂNG CẤP LỰC HÚT: Tăng gia tốc hút bằng vector multiply(0.8)
                if (ticks % 2 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double r = 5.0 + Math.random() * 3.0; // Xuất hiện từ xa
                        Location spawnLoc = loc.clone().add(Math.cos(angle) * r, 0.5 + Math.random() * 2, Math.sin(angle) * r);

                        // Vector hướng về người chơi với tốc độ cao
                        org.bukkit.util.Vector dir = loc.clone().add(0, 1, 0).toVector().subtract(spawnLoc.toVector()).normalize().multiply(0.8);
                        player.getWorld().spawnParticle(Particle.END_ROD, spawnLoc, 0, dir.getX(), dir.getY(), dir.getZ(), 0.5);
                        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, spawnLoc, 0, dir.getX(), dir.getY(), dir.getZ(), 0.5);
                    }
                }

                // Vòng xoáy dưới chân
                for (int i = 0; i < 3; i++) {
                    double angle = Math.toRadians(ticks * 20 + (i * 120));
                    double x = Math.cos(angle) * 1.5;
                    double z = Math.sin(angle) * 1.5;
                    player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0.1, z), 2, 0, 0, 0, 0, goldDust);
                }

                ticks++;
                if (ticks >= channelTicks) {
                    cancelChanneling(player, true);
                    activateDomain(player, rpgPlayer, level, isEmpowered);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        player.setMetadata(cdKey, new FixedMetadataValue(plugin, System.currentTimeMillis() + getCooldown(level)));
        return true;
    }

    private void cancelChanneling(Player player, boolean isSuccess) {
        StatusEffectManager.removeAbsoluteRoot(player);
        player.removeMetadata("Domain_Channeling", plugin);
        player.removeMetadata("Domain_HitCount", plugin);

        if (!isSuccess) {
            long reducedCd = (long) (getCooldown(1) * 0.4);
            player.setMetadata("CD_" + getId(), new FixedMetadataValue(plugin, System.currentTimeMillis() + reducedCd));
        }
    }

    private void activateDomain(Player caster, RPGPlayer rpgCaster, int level, boolean isEmpowered) {
        Location center = caster.getLocation().clone();
        int finalRadius = 12 + (level * 4);
        int durationTicks = (15 + (level * 3)) * 20;
        double finalBuffRate = (0.05 + (level * 0.05)) * (isEmpowered ? 1.5 : 1.0);

        caster.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
        caster.sendMessage("§e§l[!] TRIỂN KHAI THÁNH VỰC!");

        new BukkitRunnable() {
            double currentR = 2.0;
            final Particle.DustOptions cyan = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 1.0f);
            final Particle.DustOptions white = new Particle.DustOptions(Color.WHITE, 1.0f);

            @Override
            public void run() {
                if (currentR >= finalRadius) { this.cancel(); return; }
                for (int i = 0; i < 360; i += 6) {
                    double rad = Math.toRadians(i);
                    Location pLoc = center.clone().add(Math.cos(rad) * currentR, 0.2, Math.sin(rad) * currentR);
                    center.getWorld().spawnParticle(Particle.GLOW, pLoc, 1, 0.1, 0.1, 0.1, 0);
                    center.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, Math.random() > 0.5 ? cyan : white);
                }
                currentR += (finalRadius / 10.0);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // =================================================================
        // [TỐI ƯU]: Tính toán viền 1 LẦN DUY NHẤT rồi lưu lại (Cache)
        // =================================================================
        List<Location> cachedEdges = new ArrayList<>();
        for (int i = 0; i < 360; i += 8) { // Khoảng cách hạt viền
            double rad = Math.toRadians(i);
            cachedEdges.add(center.clone().add(Math.cos(rad) * finalRadius, 0.2, Math.sin(rad) * finalRadius));
        }

        DomainData data = new DomainData(center, caster.getUniqueId(), level, isEmpowered, finalBuffRate, finalRadius, cachedEdges);
        activeDomains.put(caster.getUniqueId(), data);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks || !caster.isOnline()) {
                    activeDomains.remove(caster.getUniqueId());
                    transitionToSkyMatrix(caster, rpgCaster, data);
                    this.cancel();
                    return;
                }

                // Truyền hẳn Cục Data vào để nó dùng list tọa độ đã tính
                drawDomainEdge(data, ticks);

                if (ticks % 20 == 0) {
                    long expireTime = System.currentTimeMillis() + 1500L;
                    for (Entity e : center.getWorld().getNearbyEntities(center, finalRadius, 15, finalRadius)) {
                        if (e instanceof LivingEntity) {
                            LivingEntity le = (LivingEntity) e;
                            if (isAlly(caster, le)) {
                                le.setMetadata("Domain_Ally_Buff", new FixedMetadataValue(plugin, finalBuffRate));
                                le.setMetadata("Domain_Ally_Expire", new FixedMetadataValue(plugin, expireTime));

                                if (le instanceof Player) {
                                    RPGPlayer targetRpg = RPGCore.getInstance().getPlayerManager().getRPGPlayer(le.getUniqueId());
                                    if (targetRpg != null) targetRpg.updateMaxStats();
                                }
                            } else if (CombatManager.canTarget(caster, e)) {
                                le.setMetadata("Domain_Enemy_Debuff", new FixedMetadataValue(plugin, finalBuffRate));
                                le.setMetadata("Domain_Enemy_Expire", new FixedMetadataValue(plugin, expireTime));
                            }
                        }
                    }
                }
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void drawDomainEdge(DomainData data, int currentTick) {
        Particle.DustOptions cyanDust = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 1.0f);

        // 1. Vẽ Vòng Tròn Viền Tĩnh (Chỉ vẽ mỗi 6 ticks để giảm render cho client)
        if (currentTick % 6 == 0) {
            for (Location edgeLoc : data.edgePoints) {
                edgeLoc.getWorld().spawnParticle(Particle.DUST, edgeLoc, 1, 0, 0, 0, 0, cyanDust);
            }
        }

        // 2. TƯỜNG ẢO ẢNH (Holographic Wall) siêu nhẹ
        // Thay vì xoay 8 cột khổng lồ, bốc ngẫu nhiên vài điểm trên viền bắn hạt vút lên trời
        Random rand = new Random();
        int particleCount = data.radius; // Lãnh vực càng to thì Tường hạt bắn lên càng nhiều

        for (int i = 0; i < particleCount; i++) {
            Location randomEdge = data.edgePoints.get(rand.nextInt(data.edgePoints.size()));

            // Hạt END_ROD với vector (0, 0.08, 0) tạo hiệu ứng hạt sáng lơ lửng bay lên trên cực ảo
            randomEdge.getWorld().spawnParticle(Particle.END_ROD, randomEdge, 0, 0, 0.08, 0, 1.0);

            // Thêm các hạt GLOW lấp lánh ẩn hiện trong tường
            if (Math.random() > 0.6) {
                randomEdge.getWorld().spawnParticle(Particle.GLOW, randomEdge.clone().add(0, Math.random() * 4, 0), 1, 0.1, 0.1, 0.1, 0);
            }
        }
    }

    // GIAI ĐOẠN 3: CỘT ÁNH SÁNG BAY LÊN TRỜI
    private void transitionToSkyMatrix(Player caster, RPGPlayer rpgCaster, DomainData data) {
        if (!data.isEmpowered) return;
        int orbs = data.killCount / 5;
        if (orbs <= 0) return;

        caster.sendMessage("§e§l[!] §fHiệu triệu Ma Trận Lôi Phạt...");
        data.center.getWorld().playSound(data.center, Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 0.5f);

        new BukkitRunnable() {
            double currentY = 0;
            @Override
            public void run() {
                if (currentY >= 25) {
                    this.cancel();
                    buildSkyMatrix(caster, rpgCaster, data, orbs); // Chuyển sang Giai đoạn 4
                    return;
                }

                // 8 cột sáng hội tụ dần dần vào tâm trên bầu trời
                double progress = currentY / 25.0; // 0.0 -> 1.0
                double currentRadius = data.radius * (1.0 - (progress * 0.5)); // Hẹp dần lại

                for (int i = 0; i < 360; i += 45) {
                    double rad = Math.toRadians(i);
                    Location pLoc = data.center.clone().add(Math.cos(rad) * currentRadius, currentY, Math.sin(rad) * currentRadius);
                    pLoc.getWorld().spawnParticle(Particle.END_ROD, pLoc, 3, 0.2, 0.2, 0.2, 0.05);
                    pLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pLoc, 5, 0.2, 0.2, 0.2, 0);
                }
                currentY += 1.5;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // GIAI ĐOẠN 4: VẼ MA TRẬN ỔN ĐỊNH TRONG 2 GIÂY
    private void buildSkyMatrix(Player caster, RPGPlayer rpgCaster, DomainData data, int orbs) {
        Location skyCenter = data.center.clone().add(0, 25, 0);
        skyCenter.getWorld().playSound(skyCenter, Sound.ENTITY_WITHER_SPAWN, 2.0f, 1.5f);

        // 40 ticks = 2 giây
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 40) {
                    this.cancel();
                    executeHomingStrikes(caster, rpgCaster, data, orbs, skyCenter); // Giai đoạn 5
                    return;
                }

                drawComplexGeometricMatrix(skyCenter, data.radius * 0.8, ticks);
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // THUẬT TOÁN VẼ MA TRẬN HÌNH HỌC (CIRCLE + SQUARES)
    private void drawComplexGeometricMatrix(Location center, double radius, int currentTick) {
        Particle.DustOptions cyan = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 1.0f);
        Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.0f);

        double rot = Math.toRadians(currentTick);

        // 1. Vòng tròn ngoài cùng (Xoay)
        for (int i = 0; i < 360; i += 8) {
            double rad = Math.toRadians(i + currentTick * 2);
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(Math.cos(rad)*radius, 0, Math.sin(rad)*radius), 1, 0,0,0,0, cyan);
        }

        // 2. Vòng tròn trong tĩnh
        double innerRadius = radius * 0.7;
        for (int i = 0; i < 360; i += 12) {
            double rad = Math.toRadians(i);
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(Math.cos(rad)*innerRadius, 0, Math.sin(rad)*innerRadius), 1, 0,0,0,0, gold);
        }

        // 3. Hai hình vuông lồng nhau tạo thành sao 8 cánh
        // Dùng thuật toán tham số hóa đường thẳng để vẽ cạnh hình vuông
        for (int i = 0; i < 8; i++) { // 8 đỉnh của sao
            double rad1 = Math.toRadians((i * 45) + currentTick);
            double rad2 = Math.toRadians(((i + 2) % 8 * 45) + currentTick); // Nối đỉnh cách 1 đỉnh (tạo 2 hình vuông)

            Location p1 = center.clone().add(Math.cos(rad1)*innerRadius, 0, Math.sin(rad1)*innerRadius);
            Location p2 = center.clone().add(Math.cos(rad2)*innerRadius, 0, Math.sin(rad2)*innerRadius);

            // Interpolate vẽ đường nối giữa 2 đỉnh
            org.bukkit.util.Vector dir = p2.toVector().subtract(p1.toVector());
            double length = dir.length();
            dir.normalize();
            for (double step = 0; step < length; step += 1.5) {
                Location lineLoc = p1.clone().add(dir.clone().multiply(step));
                center.getWorld().spawnParticle(Particle.END_ROD, lineLoc, 1, 0,0,0, 0);
            }
        }
    }

    // GIAI ĐOẠN 5: BẮN HẠT RƠI XUỐNG VÀ GIÁNG LÔI
    private void executeHomingStrikes(Player caster, RPGPlayer rpgCaster, DomainData data, int orbs, Location skyCenter) {
        List<LivingEntity> targets = getNearbyEnemies(caster, data);
        if (targets.isEmpty()) return;

        double damagePerOrbMultiplier = 4.0 + (data.level * 2.0); // Cấp 1 = 6.0, Cấp 2 = 8.0, Cấp 3 = 10.0
        Random rand = new Random();

        Map<LivingEntity, Integer> hitsMap = new HashMap<>();
        if (targets.size() == 1) {
            hitsMap.put(targets.get(0), orbs);
        } else {
            for (int i = 0; i < orbs; i++) {
                LivingEntity t = targets.get(rand.nextInt(targets.size()));
                hitsMap.put(t, hitsMap.getOrDefault(t, 0) + 1);
            }
        }

        for (Map.Entry<LivingEntity, Integer> entry : hitsMap.entrySet()) {
            LivingEntity victim = entry.getKey();
            int hitCount = entry.getValue();

            // Trói chân mục tiêu ngay khi bắt đầu nhắm bắn
            if (victim instanceof Player) {
                StatusEffectManager.applyAbsoluteRoot((Player) victim, 40);
            } else {
                StatusEffectManager.applyEffect(victim, PotionEffectType.SLOWNESS, 40, 10);
            }

            // Hoạt ảnh tia sáng lao từ trên Ma Trận xuống mục tiêu
            Location vLoc = victim.getLocation();

            // Ép tốc độ rơi cực nhanh (3.5 blocks/tick)
            org.bukkit.util.Vector fallDir = vLoc.toVector().subtract(skyCenter.toVector()).normalize().multiply(3.5);

            new BukkitRunnable() {
                Location current = skyCenter.clone();
                @Override
                public void run() {
                    // Nếu chạm đất hoặc chạm mục tiêu
                    if (current.getY() <= vLoc.getY() || current.distanceSquared(vLoc) < 4.0) {
                        this.cancel();
                        executeFinalLightning(caster, rpgCaster, victim, hitCount, damagePerOrbMultiplier);
                        return;
                    }
                    current.add(fallDir);

                    // [TINH CHỈNH]: Radius cực mỏng (0.02) và giảm số lượng hạt lại để tia không bị "Béo"
                    current.getWorld().spawnParticle(Particle.END_ROD, current, 1, 0.02, 0.02, 0.02, 0);
                    current.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, current, 2, 0.05, 0.05, 0.05, 0);
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    private void executeFinalLightning(Player caster, RPGPlayer rpgCaster, LivingEntity victim, int hitCount, double dmgMultiplier) {
        Location vLoc = victim.getLocation();

        vLoc.getWorld().spawnParticle(Particle.EXPLOSION, vLoc, hitCount, 0.5, 0.5, 0.5, 0);
        vLoc.getWorld().strikeLightningEffect(vLoc);
        vLoc.getWorld().playSound(vLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.8f);

        double totalDmgMulti = dmgMultiplier * hitCount;
        double finalDmg = CombatManager.calculateDamage(caster, rpgCaster, victim, totalDmgMulti, 0.0, false, false);

        caster.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
        CombatManager.applySkillDamage(caster, victim, finalDmg);
        caster.removeMetadata("Ghost_L_Blocker", plugin);

        if (victim.isDead() || victim.getHealth() <= 0) {
            spawnInheritanceOrb(caster, vLoc);
        }
    }

    private List<LivingEntity> getNearbyEnemies(Player caster, DomainData data) {
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity e : data.center.getWorld().getNearbyEntities(data.center, data.radius, 15, data.radius)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(caster, e) && !e.isDead()) {
                targets.add((LivingEntity) e);
            }
        }
        return targets;
    }

    private void spawnInheritanceOrb(Player owner, Location loc) {
        Location spawnLoc = loc.clone().add(0, 0.5, 0);
        // Tăng thời gian Mảnh rớt dưới đất lên 60 giây (60000L)
        activeOrbs.add(new InheritanceOrbData(spawnLoc, owner.getUniqueId(), System.currentTimeMillis() + 30000L));
        spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
    }

    private void startOrbPickupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Iterator<InheritanceOrbData> it = activeOrbs.iterator();

                while (it.hasNext()) {
                    InheritanceOrbData orb = it.next();
                    if (now > orb.expireTime) {
                        it.remove();
                        continue;
                    }

                    drawLightningOrb(orb.loc);

                    Player owner = Bukkit.getPlayer(orb.ownerId);
                    if (owner != null && owner.isOnline() && owner.getWorld().equals(orb.loc.getWorld())) {
                        if (owner.getLocation().distance(orb.loc) <= 1.5) {
                            int currentStacks = owner.hasMetadata("Warrior_Judgment_Stacks") ? owner.getMetadata("Warrior_Judgment_Stacks").get(0).asInt() : 0;

                            if (currentStacks < 8) {
                                owner.setMetadata("Warrior_Judgment_Stacks", new FixedMetadataValue(plugin, currentStacks + 1));

                                // Tăng thời gian giữ Stack trên người lên 60 GIÂY (1200L)
                                applyWeaponEnhancement(owner, "Warrior_Judgment_Stacks", currentStacks + 1, 1200L,
                                        "§e§l[!] §fHấp thu Mảnh Sấm Sét! (Stack: " + (currentStacks + 1) + "/8)",
                                        "§8§o(Sức mạnh Phán Quyết đã tàn lụi...)");

                                owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                                owner.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, owner.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                                it.remove();
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void drawLightningOrb(Location loc) {
        Particle.DustOptions gold = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 215, 0), 0.8f);
        double time = System.currentTimeMillis() / 300.0;
        Location center = loc.clone().add(0, Math.sin(time) * 0.2 + 0.5, 0);

        center.getWorld().spawnParticle(Particle.END_ROD, center, 1, 0.05, 0.05, 0.05, 0.01);
        for (int i = 0; i < 3; i++) {
            double angle = time * 2 + (i * Math.PI * 2 / 3);
            double x = Math.cos(angle) * 0.3;
            double z = Math.sin(angle) * 0.3;
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 0, z), 1, 0, 0, 0, 0, gold);
            center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(x, 0, z), 1, 0, 0, 0, 0);
        }
    }

    @EventHandler
    public void onDomainDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        for (DomainData data : activeDomains.values()) {
            if (data.isEmpowered && data.center.getWorld().equals(victim.getWorld())) {
                if (data.center.distance(victim.getLocation()) <= data.radius) {
                    Player caster = Bukkit.getPlayer(data.casterId);
                    if (caster != null && CombatManager.canTarget(caster, victim)) {
                        data.killCount++;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCastingDamageTaken(EntityDamageEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (player.hasMetadata("Domain_Channeling") && event instanceof EntityDamageByEntityEvent) {
            java.util.List<org.bukkit.metadata.MetadataValue> meta = player.getMetadata("Domain_HitCount");
            if (!meta.isEmpty()) {
                int currentHits = meta.get(0).asInt();
                player.setMetadata("Domain_HitCount", new FixedMetadataValue(plugin, currentHits + 1));
            }
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.5f);
            player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
        }
    }

    private boolean isAlly(Player caster, LivingEntity target) {
        if (caster.equals(target)) return true;
        Player targetOwner = null;
        if (target instanceof Player) {
            targetOwner = (Player) target;
        } else if (target.hasMetadata("Summoner_Pet")) {
            try {
                if (target.getMetadata("Summoner_Pet").isEmpty()) return false;
                String ownerId = target.getMetadata("Summoner_Pet").get(0).asString();
                if (caster.getUniqueId().toString().equals(ownerId)) return true;
                targetOwner = Bukkit.getPlayer(UUID.fromString(ownerId));
            } catch (Exception ignored) {}
        }
        if (targetOwner != null && targetOwner.isOnline()) {
            for (CombatManager.IAllyChecker checker : CombatManager.allyCheckers) {
                if (checker.isAlly(caster, targetOwner)) return true;
            }
        }
        return false;
    }

    private static class DomainData {
        Location center; UUID casterId; int level; boolean isEmpowered; double buffRate; int radius;
        int killCount = 0;

        // [TỐI ƯU]: Lưu sẵn danh sách điểm viền để khỏi phải tính Toán học mỗi tick
        List<Location> edgePoints;

        public DomainData(Location center, UUID casterId, int level, boolean isEmpowered, double buffRate, int radius, List<Location> edgePoints) {
            this.center = center; this.casterId = casterId; this.level = level; this.isEmpowered = isEmpowered; this.buffRate = buffRate; this.radius = radius;
            this.edgePoints = edgePoints;
        }
    }

    private static class InheritanceOrbData {
        Location loc; UUID ownerId; long expireTime;
        public InheritanceOrbData(Location loc, UUID ownerId, long expireTime) { this.loc = loc; this.ownerId = ownerId; this.expireTime = expireTime; }
    }
}