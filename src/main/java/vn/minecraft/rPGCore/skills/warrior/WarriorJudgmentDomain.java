package vn.minecraft.rPGCore.skills.warrior;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.AbstractEnhancementSkill;

import java.util.*;

public class WarriorJudgmentDomain extends AbstractEnhancementSkill implements Listener {

    public static final Map<UUID, DomainData> activeDomains = new HashMap<>();
    private final List<InheritanceSword> activeSwords = new ArrayList<>();

    public WarriorJudgmentDomain(JavaPlugin plugin) {
        super(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startSwordPickupTask();
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
    @Override public double getManaCost(int level) { return 110.0; } // Cost cho Thức Tỉnh
    @Override public long getCooldown(int level) { return 90000L; }

    @Override
    public List<String> getDescription(int level) {
        // Tính toán chỉ số dựa theo level (Cấp 1: 10%, Cấp 2: 15%, Cấp 3: 20%)
        int buffRate = 5 + (level * 5);
        double awakenRate = buffRate * 1.5;

        return Arrays.asList(
                "§7Dựng lên Thánh Vực tăng sức mạnh toàn diện.",
                "§7Yêu cầu niệm chú tại chỗ.",
                "",
                "§e[Kích hoạt: Thánh vực]",
                "§f- Niệm §a5s§f. Bị ngắt nếu nhận §c3 Hit§f.",
                "§f- Phe ta: Tăng §e" + buffRate + "% ST §fvà Tốc chạy.",
                "§f- Kẻ địch: Bị áp chế, giảm §c" + buffRate + "% ST §fgây ra.",
                "",
                "§c[Thức Tỉnh Kỹ Năng]",
                "§f- Niệm §a2.5s§f. Chịu được §c5 Hit§f.",
                "§f- Hiệu quả Buff/Debuff tăng thành §e" + String.format("%.1f", awakenRate) + "%§f.",
                "§f- Cứ giết §c9 địch§f trong Vùng = Tích 1 Kiếm Phán Quyết.",
                "§f- Khi hết hạn: Giáng Lôi Phạt dựa trên số Kiếm tích lũy.",
                "§f- Nhặt Kiếm Ánh Sáng để x2 Sát thương nhát chém tới."
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

        final int channelTicks = isEmpowered ? 50 : 100; // 2.5s hoặc 5s
        final int maxHits = isEmpowered ? 5 : 3;

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, channelTicks, 10, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, channelTicks, 128, false, false));
        player.setMetadata("Domain_Channeling", new FixedMetadataValue(plugin, true));
        player.setMetadata("Domain_HitCount", new FixedMetadataValue(plugin, 0));

        player.sendMessage("§e§l[!] §fThiết lập Thánh Vực...");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !player.hasMetadata("Domain_Channeling")) {
                    cancelChanneling(player, true);
                    this.cancel();
                    return;
                }

                int hitsTaken = player.getMetadata("Domain_HitCount").get(0).asInt();
                if (hitsTaken >= maxHits) {
                    player.sendMessage("§c§l[!] §fThánh Vực bị phá vỡ!");
                    cancelChanneling(player, false);
                    this.cancel();
                    return;
                }

                Location loc = player.getLocation();
                double radius = 2.0 - (1.5 * ((double) ticks / channelTicks));
                double x = Math.cos(ticks) * radius;
                double z = Math.sin(ticks) * radius;
                player.getWorld().spawnParticle(Particle.ENCHANT, loc.add(x, 0.1, z), 2, 0, 0, 0, 0);

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
        player.removeMetadata("Domain_Channeling", plugin);
        player.removeMetadata("Domain_HitCount", plugin);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);

        if (!isSuccess) {
            long reducedCd = (long) (getCooldown(1) * 0.4); // 36s
            player.setMetadata("CD_" + getId(), new FixedMetadataValue(plugin, System.currentTimeMillis() + reducedCd));
        }
    }

    private void activateDomain(Player caster, RPGPlayer rpgCaster, int level, boolean isEmpowered) {
        Location center = caster.getLocation().clone();
        int durationTicks = (12 + (level * 3)) * 20; // 15s, 18s, 21s

        double baseRate = 0.05 + (level * 0.05); // 10%, 15%, 20%
        double rateMulti = isEmpowered ? 1.5 : 1.0;
        double finalBuffRate = baseRate * rateMulti;

        DomainData data = new DomainData(center, caster.getUniqueId(), level, isEmpowered, finalBuffRate);
        activeDomains.put(caster.getUniqueId(), data);

        caster.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.2f);
        caster.sendMessage("§e§l[!] THÁNH VỰC KHAI MỞ!");

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

                if (ticks % 20 == 0) { // Mỗi giây quét 1 lần
                    // Quét đồng minh và kẻ địch để áp buff (Tự động hết khi ra khỏi vùng nhờ thời gian tồn tại ngắn)
                    for (Entity e : center.getWorld().getNearbyEntities(center, 10, 10, 10)) {
                        if (e instanceof LivingEntity) {
                            LivingEntity le = (LivingEntity) e;
                            if (isAlly(caster, le)) {
                                le.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, isEmpowered ? 1 : 0, false, false));
                                le.setMetadata("Domain_Ally_Buff", new FixedMetadataValue(plugin, finalBuffRate));
                            } else if (CombatManager.canTarget(caster, e)) {
                                le.setMetadata("Domain_Enemy_Debuff", new FixedMetadataValue(plugin, finalBuffRate));
                            }
                        }
                    }
                }
                ticks += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void triggerJudgmentStrike(Player caster, RPGPlayer rpgCaster, DomainData data) {
        if (!data.isEmpowered) return;

        int swords = data.killCount / 9;
        if (swords <= 0) return;

        List<LivingEntity> targets = new ArrayList<>();
        for (Entity e : data.center.getWorld().getNearbyEntities(data.center, 10, 10, 10)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(caster, e) && !e.isDead()) {
                targets.add((LivingEntity) e);
            }
        }

        if (targets.isEmpty()) return;

        double damagePerSwordMultiplier = 5.0 + (data.level * 1.0); // 6.0, 7.0, 8.0 (600%, 700%, 800%)

        caster.sendMessage("§c§l[!] §f" + swords + " Kiếm Phán Quyết Giáng Lâm!");

        // Dồn Sát Thương (Simultaneous Strike)
        Map<LivingEntity, Integer> hitsMap = new HashMap<>();
        Random rand = new Random();

        if (targets.size() == 1) {
            hitsMap.put(targets.get(0), swords);
        } else {
            for (int i = 0; i < swords; i++) {
                LivingEntity t = targets.get(rand.nextInt(targets.size()));
                hitsMap.put(t, hitsMap.getOrDefault(t, 0) + 1);
            }
        }

        for (Map.Entry<LivingEntity, Integer> entry : hitsMap.entrySet()) {
            LivingEntity victim = entry.getKey();
            int hitCount = entry.getValue();

            // Khóa chân (Root) bằng Slow cực mạnh
            StatusEffectManager.applyEffect(victim, PotionEffectType.SLOWNESS, 40, 10);

            // VFX Sét đánh (Nhiều kiếm thì bùm to hơn)
            victim.getWorld().spawnParticle(Particle.FLASH, victim.getLocation(), hitCount, 0.5, 2, 0.5, 0);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.8f);

            double totalDmgMulti = damagePerSwordMultiplier * hitCount;
            double finalDmg = CombatManager.calculateDamage(caster, rpgCaster, victim, totalDmgMulti, 0.0, false, false);

            caster.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
            CombatManager.applySkillDamage(caster, victim, finalDmg);
            caster.removeMetadata("Ghost_L_Blocker", plugin);

            // Kiểm tra tử vong để sinh ra Kiếm Ánh Sáng (Chỉ sinh 1 cây dù ăn nhiều hit)
            if (victim.isDead() || victim.getHealth() <= 0) {
                spawnInheritanceSword(caster, victim.getLocation());
            }
        }
    }

    // ==========================================
    // CƠ CHẾ KIẾM ÁNH SÁNG (INHERITANCE SWORD)
    // ==========================================
    private void spawnInheritanceSword(Player owner, Location loc) {
        Location spawnLoc = loc.clone().add(0, 0.5, 0);
        ItemDisplay display = (ItemDisplay) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ITEM_DISPLAY);
        display.setItemStack(new ItemStack(Material.GOLDEN_SWORD));

        // Cắm kiếm xuống đất góc 135 độ
        Transformation trans = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f((float) Math.toRadians(135), 1, 0, 0),
                new Vector3f(1.5f, 1.5f, 1.5f),
                new AxisAngle4f(0, 0, 1, 0)
        );
        display.setTransformation(trans);

        activeSwords.add(new InheritanceSword(display, owner.getUniqueId(), System.currentTimeMillis() + 15000L)); // Tồn tại 15s
        spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
    }

    private void startSwordPickupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Iterator<InheritanceSword> it = activeSwords.iterator();

                while (it.hasNext()) {
                    InheritanceSword sword = it.next();
                    if (now > sword.expireTime || !sword.display.isValid()) {
                        if (sword.display.isValid()) sword.display.remove();
                        it.remove();
                        continue;
                    }

                    Player owner = Bukkit.getPlayer(sword.ownerId);
                    if (owner != null && owner.isOnline() && owner.getWorld().equals(sword.display.getWorld())) {
                        if (owner.getLocation().distance(sword.display.getLocation()) <= 1.5) {
                            // NHẶT KIẾM
                            int currentStacks = owner.hasMetadata("Warrior_Judgment_Stacks") ? owner.getMetadata("Warrior_Judgment_Stacks").get(0).asInt() : 0;

                            // Sử dụng AbstractEnhancementSkill để buff
                            applyWeaponEnhancement(owner, "Warrior_Judgment_Stacks", currentStacks + 1, 200L,
                                    "§e§l[!] §fHấp thu Ánh Sáng Phán Quyết! (Stack: " + (currentStacks + 1) + ")",
                                    "§8§o(Sức mạnh Phán Quyết đã tàn lụi...)");

                            owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                            owner.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, owner.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);

                            sword.display.remove();
                            it.remove();
                        } else {
                            // Hiệu ứng lấp lánh khi kiếm nằm chờ
                            sword.display.getWorld().spawnParticle(Particle.END_ROD, sword.display.getLocation().add(0, 0.5, 0), 1, 0.2, 0.2, 0.2, 0.01);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L); // Quét mỗi 0.25s
    }

    // ==========================================
    // LẮNG NGHE SỰ KIỆN TÍCH LŨY VÀ SÁT THƯƠNG
    // ==========================================

    @EventHandler
    public void onDomainDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        for (DomainData data : activeDomains.values()) {
            if (data.isEmpowered && data.center.getWorld().equals(victim.getWorld())) {
                if (data.center.distance(victim.getLocation()) <= 10.0) {
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
            double rate = damager.getMetadata("Domain_Ally_Buff").get(0).asDouble();
            event.setDamage(event.getDamage() * (1.0 + rate));
        }

        if (damager.hasMetadata("Domain_Enemy_Debuff")) {
            double rate = damager.getMetadata("Domain_Enemy_Debuff").get(0).asDouble();
            event.setDamage(event.getDamage() * (1.0 - rate));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChannelingDamageTaken(EntityDamageEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (player.hasMetadata("Domain_Channeling") && event instanceof EntityDamageByEntityEvent) {
            int currentHits = player.getMetadata("Domain_HitCount").get(0).asInt();
            player.setMetadata("Domain_HitCount", new FixedMetadataValue(plugin, currentHits + 1));
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.5f);
        }
    }

    private boolean isAlly(Player caster, LivingEntity target) {
        if (caster.equals(target)) return true;
        if (target.hasMetadata("Summoner_Pet")) {
            String ownerId = target.getMetadata("Summoner_Pet").get(0).asString();
            return caster.getUniqueId().toString().equals(ownerId);
        }
        return false;
    }

    private static class DomainData {
        Location center;
        UUID casterId;
        int level;
        boolean isEmpowered;
        double buffRate;
        int killCount = 0;

        public DomainData(Location center, UUID casterId, int level, boolean isEmpowered, double buffRate) {
            this.center = center;
            this.casterId = casterId;
            this.level = level;
            this.isEmpowered = isEmpowered;
            this.buffRate = buffRate;
        }
    }

    private static class InheritanceSword {
        ItemDisplay display;
        UUID ownerId;
        long expireTime;

        public InheritanceSword(ItemDisplay display, UUID ownerId, long expireTime) {
            this.display = display;
            this.ownerId = ownerId;
            this.expireTime = expireTime;
        }
    }
}