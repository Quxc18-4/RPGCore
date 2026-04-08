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
    }

    @Override public String getId() { return "warrior_judgment_domain"; }
    @Override public String getName() { return "§e§lThánh Vực Phán Quyết"; }
    @Override public String getRequiredClass() { return "warrior"; }
    @Override public Material getIcon() { return Material.BEACON; }
    @Override public int getMaxLevel() { return 3; }
    @Override public int getUpgradeCost(int currentLevel) { return 3; }
    @Override public int getRequiredPlayerLevel() { return 40; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }
    @Override public boolean isPassiveSkill() { return false; }
    @Override public double getManaCost(int level) { return 110.0; }
    @Override public long getCooldown(int level) { return 9000L; } //9s cd để test

    @Override
    public List<String> getDescription(int level) {
        int radius = 12 + (level * 4); // 16, 20, 24
        int buffRate = 5 + (level * 5);
        double awakenRate = buffRate * 1.5;

        return Arrays.asList(
                "§7Thiết lập một thánh vực khổng lồ duy trì trật tự.",
                "§7Tầm xa: §e" + radius + "m§7. Niệm tại chỗ.",
                "",
                "§e[Kích hoạt: Thánh vực]",
                "§f- Đứng im §a5s§f. §bKháng 100% Đẩy lùi§f. Chịu 3 Hit.",
                "§f- Phe ta: Tăng §e" + buffRate + "% ST §fvà Tốc chạy.",
                "§f- Kẻ địch: Bị áp chế, giảm §c" + buffRate + "% ST §fgây ra.",
                "",
                "§c[Thức Tỉnh Kỹ Năng]",
                "§f- Niệm §a2.5s§f. Chịu được §c5 Hit§f.",
                "§f- Hiệu quả Buff/Debuff tăng thành §e" + String.format("%.1f", awakenRate) + "%§f.",
                "§f- Cứ giết §c5 địch§f trong Vùng = Tích 1 Mảnh Sấm Sét.",
                "§f- Khi hết hạn: Tạo Ma Trận Thiên Lôi giáng xuống.",
                "§f- Nhặt Mảnh Sấm Sét để x2 ST và giáng lôi đòn tới."
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

        // [MỚI]: Sử dụng Universal Root & Anti-Knockback từ StatusEffectManager
        StatusEffectManager.applyAbsoluteRoot(player, channelTicks);

        player.setMetadata("Domain_Channeling", new FixedMetadataValue(plugin, true));
        player.setMetadata("Domain_HitCount", new FixedMetadataValue(plugin, 0));

        player.sendMessage("§e§l[!] §fĐang hiệu triệu Thánh Vực...");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);

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
                    player.sendMessage("§c§l[!] §fTập trung bị ngắt quãng!");
                    cancelChanneling(player, false);
                    this.cancel();
                    return;
                }

                Location loc = player.getLocation();

                // VFX 1: Hút Năng Lượng Trời Đất (Hạt từ xa bay ngược vào tâm)
                if (ticks % 2 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double radius = 3.0 + Math.random() * 3.0;
                        Location spawnLoc = loc.clone().add(Math.cos(angle) * radius, Math.random() * 2, Math.sin(angle) * radius);

                        // Tính vector hướng vào người chơi
                        org.bukkit.util.Vector dir = loc.clone().add(0, 1, 0).toVector().subtract(spawnLoc.toVector()).normalize().multiply(0.3);
                        // Dùng END_ROD với vector để nó bắn về phía người chơi
                        player.getWorld().spawnParticle(Particle.END_ROD, spawnLoc, 0, dir.getX(), dir.getY(), dir.getZ(), 0.1);
                    }
                }

                // Vòng tròn dưới chân
                for (int i = 0; i < 3; i++) {
                    double angle = Math.toRadians(ticks * 15 + (i * 120));
                    double x = Math.cos(angle) * 1.5;
                    double z = Math.sin(angle) * 1.5;
                    player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0, goldDust);
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
        int finalRadius = 12 + (level * 4); // 16, 20, 24
        int durationTicks = (15 + (level * 3)) * 20;

        double baseRate = 0.05 + (level * 0.05);
        double rateMulti = isEmpowered ? 1.5 : 1.0;
        double finalBuffRate = baseRate * rateMulti;

        // Âm thanh bùng nổ khi khai mở Lãnh Vực
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.5f);
        caster.sendMessage("§e§l[!] THÁNH VỰC KHAI MỞ!");

        new BukkitRunnable() {
            double currentR = 2.0;
            @Override
            public void run() {
                if (currentR >= finalRadius) { this.cancel(); return; }

                Particle.DustOptions cyan = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 1.5f);
                for (int i = 0; i < 360; i += 8) {
                    double rad = Math.toRadians(i);
                    Location pLoc = center.clone().add(Math.cos(rad) * currentR, 0.2, Math.sin(rad) * currentR);
                    center.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, cyan);
                    if (Math.random() > 0.5) {
                        center.getWorld().spawnParticle(Particle.GLOW, pLoc, 1, 0.1, 0.1, 0.1, 0); // Glittering
                    }
                }
                currentR += (finalRadius / 10.0);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        DomainData data = new DomainData(center, caster.getUniqueId(), level, isEmpowered, finalBuffRate, finalRadius);
        activeDomains.put(caster.getUniqueId(), data);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks || !caster.isOnline()) {
                    activeDomains.remove(caster.getUniqueId());
                    triggerJudgmentStrike(caster, rpgCaster, data);
                    this.cancel();
                    return;
                }

                // Cập nhật hoạt ảnh liên tục (Vẽ mỗi 2 tick để xoay mượt)
                double rotation = (ticks * 2.5) % 360; // Góc xoay của Trụ

                Particle.DustOptions cyan = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 1.0f);

                // Vẽ biên giới lấp lánh
                for (int i = 0; i < 360; i += 15) {
                    double rad = Math.toRadians(i);
                    Location edge = center.clone().add(Math.cos(rad) * finalRadius, 0.2, Math.sin(rad) * finalRadius);
                    center.getWorld().spawnParticle(Particle.DUST, edge, 1, 0, 0, 0, 0, cyan);
                }

                // Vẽ 8 Cột Ánh Sáng XOAY VÒNG quanh biên giới
                for (int i = 0; i < 360; i += 45) { // 360 / 45 = 8 cột
                    double rad = Math.toRadians(i + rotation);
                    Location pLoc = center.clone().add(Math.cos(rad) * finalRadius, 0, Math.sin(rad) * finalRadius);
                    for (double y = 0; y < 7; y += 0.8) {
                        center.getWorld().spawnParticle(Particle.END_ROD, pLoc.clone().add(0, y, 0), 1, 0, 0, 0, 0);
                    }
                }

                // Cấp Buff/Debuff (Quét mỗi 20 tick = 1 giây)
                if (ticks % 20 == 0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, finalRadius, 15, finalRadius)) {
                        if (e instanceof LivingEntity) {
                            LivingEntity le = (LivingEntity) e;
                            if (isAlly(caster, le)) {
                                // [ĐÃ SỬA]: Sử dụng chuẩn SOLID Abstract Manager
                                StatusEffectManager.applyEffect(le, PotionEffectType.SPEED, 40, isEmpowered ? 1 : 0);
                                le.setMetadata("Domain_Ally_Buff", new FixedMetadataValue(plugin, finalBuffRate));
                            } else if (CombatManager.canTarget(caster, e)) {
                                le.setMetadata("Domain_Enemy_Debuff", new FixedMetadataValue(plugin, finalBuffRate));
                            }
                        }
                    }
                }
                ticks += 2; // Tăng 2 tick để loop mượt
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void drawDomainCircle(Location center, int radius) {
        Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(255, 223, 0), 1.5f);
        for (int i = 0; i < 360; i += 4) {
            double rad = Math.toRadians(i);
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(Math.cos(rad) * radius, 0.2, Math.sin(rad) * radius), 1, 0, 0, 0, 0, gold);
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(Math.cos(rad) * (radius - 0.5), 0.2, Math.sin(rad) * (radius - 0.5)), 1, 0, 0, 0, 0, gold);
        }
    }

    private void triggerJudgmentStrike(Player caster, RPGPlayer rpgCaster, DomainData data) {
        if (!data.isEmpowered) return;

        int orbs = data.killCount / 5; // Yêu cầu 5 mạng
        if (orbs <= 0) return;

        caster.sendMessage("§e§l[!] §fHiệu triệu Ma Trận Lôi Phạt...");

        // BƯỚC 1: BẮN TIA NĂNG LƯỢNG LÊN TRỜI
        for (int i = 0; i < 5; i++) {
            Location beamLoc = data.center.clone().add((Math.random()-0.5)*8, 0, (Math.random()-0.5)*8);
            new BukkitRunnable() {
                double y = 0;
                @Override
                public void run() {
                    if (y >= 25) { this.cancel(); return; }
                    beamLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, beamLoc.clone().add(0, y, 0), 5, 0.1, 0.1, 0.1, 0.05);
                    beamLoc.getWorld().spawnParticle(Particle.END_ROD, beamLoc.clone().add(0, y, 0), 2, 0, 0, 0, 0.02);
                    y += 2.0;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
        data.center.getWorld().playSound(data.center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0f, 0.5f);

        // BƯỚC 2: TẠO MA TRẬN BẦU TRỜI & GIÁNG SÉT (Chờ 1.25 giây để tia sáng bay lên)
        new BukkitRunnable() {
            @Override
            public void run() {
                // Vẽ Ma trận lấp lánh trên cao
                Location skyCenter = data.center.clone().add(0, 25, 0);
                for (int i = 0; i < 360; i += 8) {
                    double rad = Math.toRadians(i);
                    skyCenter.getWorld().spawnParticle(Particle.GLOW, skyCenter.clone().add(Math.cos(rad)*10, 0, Math.sin(rad)*10), 10, 1, 1, 1, 0.1);
                }
                skyCenter.getWorld().playSound(skyCenter, Sound.ENTITY_WITHER_DEATH, 1.5f, 1.5f);

                // Lấy danh sách quái
                List<LivingEntity> targets = getNearbyEnemies(caster, data);
                if (targets.isEmpty()) return;

                double damagePerOrbMultiplier = 5.0 + (data.level * 1.0);
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

                // Giáng sét từ Ma Trận xuống từng con
                for (Map.Entry<LivingEntity, Integer> entry : hitsMap.entrySet()) {
                    executeSkyStrike(caster, rpgCaster, entry.getKey(), entry.getValue(), damagePerOrbMultiplier);
                }
            }
        }.runTaskLater(plugin, 25L);
    }

    // Hàm Phụ trợ: Quét lấy danh sách kẻ địch (SỬA LỖI MÀ BẠN GẶP PHẢI)
    private List<LivingEntity> getNearbyEnemies(Player caster, DomainData data) {
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity e : data.center.getWorld().getNearbyEntities(data.center, data.radius, 15, data.radius)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(caster, e) && !e.isDead()) {
                targets.add((LivingEntity) e);
            }
        }
        return targets;
    }

    // Hàm Phụ trợ: Thực thi Sét đánh
    private void executeSkyStrike(Player caster, RPGPlayer rpgCaster, LivingEntity victim, int hitCount, double dmgMultiplier) {
        Location vLoc = victim.getLocation();
        StatusEffectManager.applyEffect(victim, PotionEffectType.SLOWNESS, 40, 10);

        // Tia sét dày giáng từ Y=25 (Ma Trận) xuống
        for (double y = 25; y >= 0; y -= 0.5) {
            vLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, vLoc.clone().add(0, y, 0), 10, 0.2, 0.5, 0.2, 0.1);
            vLoc.getWorld().spawnParticle(Particle.END_ROD, vLoc.clone().add(0, y, 0), 5, 0.1, 0.1, 0.1, 0.02);
        }

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

    private void spawnInheritanceOrb(Player owner, Location loc) {
        Location spawnLoc = loc.clone().add(0, 0.5, 0);
        activeOrbs.add(new InheritanceOrbData(spawnLoc, owner.getUniqueId(), System.currentTimeMillis() + 15000L));
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
                            applyWeaponEnhancement(owner, "Warrior_Judgment_Stacks", currentStacks + 1, 200L,
                                    "§e§l[!] §fHấp thu Mảnh Sấm Sét! (Stack: " + (currentStacks + 1) + ")",
                                    "§8§o(Sức mạnh Phán Quyết đã tàn lụi...)");

                            owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                            owner.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, owner.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                            it.remove();
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onDomainDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof LivingEntity)) return;
        Entity damager = event.getDamager();

        if (damager.hasMetadata("Domain_Ally_Buff")) {
            java.util.List<org.bukkit.metadata.MetadataValue> meta = damager.getMetadata("Domain_Ally_Buff");
            if (!meta.isEmpty()) {
                double rate = meta.get(0).asDouble();
                event.setDamage(event.getDamage() * (1.0 + rate));
            }
        }

        if (damager.hasMetadata("Domain_Enemy_Debuff")) {
            java.util.List<org.bukkit.metadata.MetadataValue> meta = damager.getMetadata("Domain_Enemy_Debuff");
            if (!meta.isEmpty()) {
                double rate = meta.get(0).asDouble();
                event.setDamage(event.getDamage() * (1.0 - rate));
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
        public DomainData(Location center, UUID casterId, int level, boolean isEmpowered, double buffRate, int radius) {
            this.center = center; this.casterId = casterId; this.level = level; this.isEmpowered = isEmpowered; this.buffRate = buffRate; this.radius = radius;
        }
    }

    private static class InheritanceOrbData {
        Location loc; UUID ownerId; long expireTime;
        public InheritanceOrbData(Location loc, UUID ownerId, long expireTime) { this.loc = loc; this.ownerId = ownerId; this.expireTime = expireTime; }
    }
}