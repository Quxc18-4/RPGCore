package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElementalDomainSkill implements ISkill {

    private final JavaPlugin plugin;

    public ElementalDomainSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // [SIÊU HOOK]: XỬ LÝ TOÀN BỘ BUFF/DEBUFF THÔNG QUA CORE
    // ==========================================
    static {
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                // [PHẠT]: Chịu x2 ST khi đang niệm chú
                if (victim.hasMetadata("Mage_Domain_Channeling") && !victim.getMetadata("Mage_Domain_Channeling").isEmpty()) return -1.0;

                // [DARK]: Kẻ địch bị giảm ST đầu ra
                if (attacker != null && isValidBuff(attacker, "Mage_Domain_Enemy_DARK")) {
                    List<MetadataValue> meta = attacker.getMetadata("Mage_Domain_Enemy_DARK");
                    if (!meta.isEmpty()) return meta.get(0).asDouble();
                }
                return 0.0;
            }

            @Override
            public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity victim) {
                // [KHÓA]: Không thể đánh khi niệm chú
                if (attacker.hasMetadata("Mage_Domain_Channeling") && !attacker.getMetadata("Mage_Domain_Channeling").isEmpty()) return -1.0;

                // [FIRE]: Tăng ST cho đồng minh
                if (isValidBuff(attacker, "Mage_Domain_Ally_FIRE")) {
                    List<MetadataValue> meta = attacker.getMetadata("Mage_Domain_Ally_FIRE");
                    if (!meta.isEmpty()) return meta.get(0).asDouble();
                }
                return 0.0;
            }
        });

        CombatManager.registerStatModifier(new CombatManager.IStatModifier() {
            @Override
            public void applyBonusStats(Player player, Map<String, Double> stats) {
                // 1. [WIND]: Buff Tốc chạy & Kháng hiệu ứng
                if (isValidBuff(player, "Mage_Domain_Ally_WIND")) {
                    List<MetadataValue> windMeta = player.getMetadata("Mage_Domain_Ally_WIND");
                    List<MetadataValue> resistMeta = player.getMetadata("Mage_Domain_Ally_WIND_Resist");
                    if (!windMeta.isEmpty() && !resistMeta.isEmpty()) {
                        double speed = windMeta.get(0).asDouble();
                        double resist = resistMeta.get(0).asDouble();
                        stats.put("Tốc độ di chuyển", stats.getOrDefault("Tốc độ di chuyển", 0.0) + (speed * 100.0));
                        stats.put("Kháng hiệu ứng", stats.getOrDefault("Kháng hiệu ứng", 0.0) + resist);
                    }
                }

                // 2. [LIGHT]: Buff Hồi Mana
                if (isValidBuff(player, "Mage_Domain_Ally_LIGHT")) {
                    List<MetadataValue> lightMeta = player.getMetadata("Mage_Domain_Ally_LIGHT");
                    if (!lightMeta.isEmpty()) {
                        double manaRegen = lightMeta.get(0).asDouble();
                        stats.put("Hồi Năng lượng", stats.getOrDefault("Hồi Năng lượng", 0.0) + (manaRegen * 100.0));
                    }
                }

                // 3. [NATURE]: Debuff Giảm Tốc chạy cho Player Kẻ Địch
                if (isValidBuff(player, "Mage_Domain_Enemy_NATURE")) {
                    List<MetadataValue> natureMeta = player.getMetadata("Mage_Domain_Enemy_NATURE");
                    if (!natureMeta.isEmpty()) {
                        double slowPct = natureMeta.get(0).asDouble();
                        stats.put("Tốc độ di chuyển", stats.getOrDefault("Tốc độ di chuyển", 0.0) - (slowPct * 100.0));
                    }
                }
            }
        });
    }

    private static boolean isValidBuff(LivingEntity entity, String key) {
        if (!entity.hasMetadata(key + "_Expire")) return false;
        List<MetadataValue> expireMeta = entity.getMetadata(key + "_Expire");
        // [FIX]: Chặn lỗi IndexOutOfBoundsException khi PlugMan xóa ruột Metadata
        if (expireMeta == null || expireMeta.isEmpty()) return false;
        return System.currentTimeMillis() <= expireMeta.get(0).asLong();
    }

    @Override public String getId() { return "mage_elemental_domain"; }
    @Override public String getName() { return "Kết Giới Nguyên Tố"; }
    @Override public String getRequiredClass() { return "mage"; }
    @Override public Material getIcon() { return Material.BEACON; }
    @Override public boolean isPassiveSkill() { return false; }
    @Override public int getMaxLevel() { return 3; }
    @Override public int getUpgradeCost(int currentLevel) { return 2; }
    @Override public int getRequiredPlayerLevel() { return 35; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override public double getManaCost(int level) { return 120.0 + ((level - 1) * 20.0); }
    @Override public long getCooldown(int level) { return (long) ((120.0 - (level - 1) * 12.0) * 1000); }

    @Override public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        List<String> lore = new ArrayList<>();
        int lv = Math.max(1, level);
        MageElement element = player != null ? ElementalSwapSkill.getActiveElement(player) : MageElement.FIRE;

        lore.add("§7Vận tụ ma lực kiến tạo Lãnh vực nguyên tố tối thượng.");
        lore.add("§f- Tầng niệm: §a4s §8/ §a16s §8/ §a28s");
        lore.add("§f- Rủi ro: §cTrói chân §8& §cx2 ST nhận vào §8khi niệm.");
        lore.add("");
        lore.add("§d§lHIỆU ỨNG: " + element.getDisplayName().toUpperCase());

        switch (element) {
            case FIRE: lore.add("§f ➥ Đồng minh: §c+10/20/35% §fSát thương gây ra."); break;
            case WATER: lore.add("§f ➥ Đồng minh: §bHồi 1/1.5/2% §fMáu tối đa/s."); break;
            case WIND: lore.add("§f ➥ Đồng minh: §a+30/50/80% §fTốc chạy & §a20/40/60 §fKháng CC."); break;
            case NATURE: lore.add("§f ➥ Kẻ địch: §2Giảm 15/30/45% §fTốc độ di chuyển."); break;
            case DARK: lore.add("§f ➥ Kẻ địch: §8Giảm 10/20/30% §fSát thương gây ra."); break;
            case LIGHT:
                lore.add("§f ➥ Đồng minh: §e+25/40/60% §fTốc độ hồi Mana.");
                lore.add("§f ➥ Kẻ địch: §cĐốt 0.5/0.5/1% §fMáu hiện tại (ST Chuẩn).");
                break;
        }
        lore.add("");
        lore.add("§8[Chuột phải để bắt đầu niệm chú]");
        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        if (player.hasMetadata("Mage_Domain_Channel_Task")) {
            triggerDomainRelease(player, rpgPlayer, level);
            return true;
        }

        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey)) {
            List<MetadataValue> metaList = player.getMetadata(cdKey);
            if (!metaList.isEmpty()) {
                long expire = metaList.get(0).asLong();
                if (System.currentTimeMillis() < expire) {
                    double timeLeft = (expire - System.currentTimeMillis()) / 1000.0;
                    player.sendMessage("§cKết Giới đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
                    return false;
                }
            }
            player.removeMetadata(cdKey, plugin);
        }

        startChanneling(player, rpgPlayer, level);
        return true;
    }

    private void startChanneling(Player player, RPGPlayer rpgPlayer, int level) {
        StatusEffectManager.applyAbsoluteRoot(player, 35 * 20);
        player.setMetadata("Mage_Domain_Channeling", new FixedMetadataValue(plugin, true));
        player.setMetadata("Mage_Domain_Phase", new FixedMetadataValue(plugin, 0));

        MageElement element = ElementalSwapSkill.getActiveElement(player);
        player.sendMessage("§e§l[!] §fĐang khởi tạo Kết Giới... (Tái kích hoạt để triển khai sớm)");

        BukkitRunnable channelTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !player.hasMetadata("Mage_Domain_Channeling")) {
                    cancelChanneling(player);
                    this.cancel();
                    return;
                }

                if (ticks % 10 == 0) {
                    float pitch = 0.5f + ((ticks / 560.0f) * 1.5f);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, pitch);
                }

                if (ticks % 2 == 0) {
                    double angle = Math.random() * Math.PI * 2;
                    Location spawnLoc = player.getLocation().add(Math.cos(angle) * 3, 0.5 + Math.random() * 1.5, Math.sin(angle) * 3);
                    org.bukkit.util.Vector dir = player.getLocation().add(0, 1, 0).toVector().subtract(spawnLoc.toVector()).normalize();
                    player.getWorld().spawnParticle(element.getParticle(), spawnLoc, 0, dir.getX(), dir.getY(), dir.getZ(), 0.3);
                }

                drawPersistentRings(player, element);
                handlePhaseAnimation(player, ticks, element);

                if (ticks == 80) {
                    reachPhaseLogic(player, 1);
                } else if (ticks == 320) {
                    reachPhaseLogic(player, 2);
                } else if (ticks == 560) {
                    reachPhaseLogic(player, 3);
                    new BukkitRunnable() {
                        @Override public void run() { triggerDomainRelease(player, rpgPlayer, level); }
                    }.runTaskLater(plugin, 20L);
                    this.cancel();
                }
                ticks++;
            }
        };
        channelTask.runTaskTimer(plugin, 0L, 1L);
        player.setMetadata("Mage_Domain_Channel_Task", new FixedMetadataValue(plugin, channelTask.getTaskId()));
    }

    private void handlePhaseAnimation(Player player, int ticks, MageElement element) {
        if (ticks < 60) drawDroppingRing(player, element, 2.0, 2.0 - (ticks * 0.03));
        else if (ticks < 80) drawLinkToPlayer(player, element, 2.0);

        if (ticks >= 80 && ticks < 280) drawDroppingRing(player, element, 3.0, 2.0 - ((ticks - 80) * 0.01));
        else if (ticks >= 280 && ticks < 320) drawLinkBetweenRings(player, element, 3.0, 2.0);

        if (ticks >= 320 && ticks < 480) drawDroppingRing(player, element, 4.0, 2.0 - ((ticks - 320) * 0.012));
        else if (ticks >= 480 && ticks < 560) drawLinkBetweenRings(player, element, 4.0, 3.0);
    }

    private void drawPersistentRings(Player player, MageElement element) {
        int phase = player.hasMetadata("Mage_Domain_Phase") ? player.getMetadata("Mage_Domain_Phase").get(0).asInt() : 0;
        if (phase >= 1) drawStaticRing(player, element, 2.0);
        if (phase >= 2) drawStaticRing(player, element, 3.0);
        if (phase >= 3) drawStaticRing(player, element, 4.0);
    }

    private void drawDroppingRing(Player player, MageElement element, double radius, double height) {
        Location center = player.getLocation().add(0, Math.max(0.1, height), 0);
        for (int i = 0; i < 360; i += 20) {
            double rad = Math.toRadians(i);
            player.getWorld().spawnParticle(element.getParticle(), center.clone().add(Math.cos(rad) * radius, 0, Math.sin(rad) * radius), 1, 0, 0, 0, 0);
        }
    }

    private void drawStaticRing(Player player, MageElement element, double radius) {
        Location center = player.getLocation().add(0, 0.1, 0);
        for (int i = 0; i < 360; i += 15) {
            double rad = Math.toRadians(i);
            player.getWorld().spawnParticle(element.getParticle(), center.clone().add(Math.cos(rad) * radius, 0, Math.sin(rad) * radius), 1, 0, 0, 0, 0);
        }
    }

    private void drawLinkToPlayer(Player player, MageElement element, double radius) {
        Location center = player.getLocation().add(0, 0.1, 0);
        for (int i = 0; i < 360; i += 60) {
            double rad = Math.toRadians(i);
            Location ringPoint = center.clone().add(Math.cos(rad) * radius, 0, Math.sin(rad) * radius);
            drawParticleLine(ringPoint, player.getLocation().add(0, 1, 0), element);
        }
    }

    private void drawLinkBetweenRings(Player player, MageElement element, double rOuter, double rInner) {
        Location center = player.getLocation().add(0, 0.1, 0);
        for (int i = 0; i < 360; i += 45) {
            double rad = Math.toRadians(i);
            Location p1 = center.clone().add(Math.cos(rad) * rOuter, 0, Math.sin(rad) * rOuter);
            Location p2 = center.clone().add(Math.cos(rad + 0.4) * rInner, 0, Math.sin(rad + 0.4) * rInner);
            drawParticleLine(p1, p2, element);
        }
    }

    private void drawParticleLine(Location l1, Location l2, MageElement element) {
        org.bukkit.util.Vector dir = l2.toVector().subtract(l1.toVector());
        double dist = dir.length();
        dir.normalize();
        for (double d = 0; d < dist; d += 0.4) {
            l1.getWorld().spawnParticle(element.getParticle(), l1.clone().add(dir.clone().multiply(d)), 1, 0, 0, 0, 0);
        }
    }

    private void reachPhaseLogic(Player player, int phase) {
        player.setMetadata("Mage_Domain_Phase", new FixedMetadataValue(plugin, phase));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.0f + (phase * 0.2f));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.5f, 0.8f + (phase * 0.2f));
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§e§lĐẠT TẦNG " + phase + "!"));
    }

    private void cancelChanneling(Player player) {
        StatusEffectManager.removeAbsoluteRoot(player);
        player.removeMetadata("Mage_Domain_Channeling", plugin);
        player.removeMetadata("Mage_Domain_Phase", plugin);
        if (player.hasMetadata("Mage_Domain_Channel_Task")) {
            Bukkit.getScheduler().cancelTask(player.getMetadata("Mage_Domain_Channel_Task").get(0).asInt());
            player.removeMetadata("Mage_Domain_Channel_Task", plugin);
        }
    }

    private void triggerDomainRelease(Player player, RPGPlayer rpgPlayer, int level) {
        int phase = 0;
        if (player.hasMetadata("Mage_Domain_Phase") && !player.getMetadata("Mage_Domain_Phase").isEmpty()) {
            phase = player.getMetadata("Mage_Domain_Phase").get(0).asInt();
        }

        cancelChanneling(player);

        if (phase == 0) {
            player.sendMessage("§c§l[!] §fNiệm chú thất bại (Chưa đạt Tầng 1).");
            applyFailureCooldown(player, rpgPlayer, level);
            return;
        }

        double baseMana = getManaCost(level);
        double requiredMana = baseMana + ((phase - 1) * 40.0);

        if (rpgPlayer.getCurrentResource() < requiredMana) {
            player.sendMessage("§c§l[!] §fKhông đủ §b" + requiredMana + " Mana §fđể mở Kết giới Tầng " + phase + "!");
            applyFailureCooldown(player, rpgPlayer, level);
            return;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - requiredMana);
        MageElement element = ElementalSwapSkill.getActiveElement(player);

        activateDomain(player, rpgPlayer, level, phase, element);
    }

    private void applyFailureCooldown(Player player, RPGPlayer rpgPlayer, int level) {
        long baseCd = getCooldown(level);
        long penaltyCd = (long) (getFinalCooldown(rpgPlayer, baseCd) * 0.1);
        player.setMetadata("CD_" + getId(), new FixedMetadataValue(plugin, System.currentTimeMillis() + penaltyCd));
        startCooldownNotifier(player, penaltyCd);
    }

    private void activateDomain(Player caster, RPGPlayer rpgCaster, int level, int phase, MageElement element) {
        Location center = caster.getLocation().clone();
        int radius = level == 1 ? 16 : (level == 2 ? 24 : 36);
        int durationTicks = (level == 1 ? 10 : (level == 2 ? 22 : 36)) * 20;

        double fireDmg = phase == 1 ? 0.1 : (phase == 2 ? 0.2 : 0.35);
        double waterHeal = phase == 1 ? 0.01 : (phase == 2 ? 0.015 : 0.02);
        double windSpeed = phase == 1 ? 0.3 : (phase == 2 ? 0.5 : 0.8);
        double windResist = phase == 1 ? 20.0 : (phase == 2 ? 40.0 : 60.0);
        double natureSlow = phase == 1 ? 0.15 : (phase == 2 ? 0.3 : 0.45);
        double darkWeak = phase == 1 ? -0.1 : (phase == 2 ? -0.2 : -0.3);
        double lightMana = phase == 1 ? 0.25 : (phase == 2 ? 0.4 : 0.6);
        double lightBurn = phase == 1 ? 0.005 : (phase == 2 ? 0.005 : 0.01);

        caster.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
        caster.getWorld().playSound(center, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.5f, 0.5f);
        caster.sendMessage("§d§l[!] TRIỂN KHAI KẾT GIỚI " + element.getDisplayName().toUpperCase() + " (TẦNG " + phase + ")!");

        new BukkitRunnable() {
            double currentRadius = 1.0;
            @Override
            public void run() {
                if (!caster.isOnline() || currentRadius >= radius) {
                    if (caster.isOnline()) StatusEffectManager.removeAbsoluteRoot(caster);
                    this.cancel();
                    return;
                }
                int particleCount = (int) (currentRadius * 15);
                for (int i = 0; i < particleCount; i++) {
                    double angle = 2 * Math.PI * i / particleCount;
                    Location loc = center.clone().add(Math.cos(angle) * currentRadius, 0.2, Math.sin(angle) * currentRadius);
                    center.getWorld().spawnParticle(element.getParticle(), loc, 1, 0, 0, 0, 0);
                }
                currentRadius += 3.0;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks || !caster.isOnline() || caster.isDead()) {
                    if (caster.isOnline()) caster.sendMessage("§8§o(Kết giới nguyên tố đã khép lại...)");
                    applyGlobalCooldown(caster, rpgCaster, level);
                    this.cancel();
                    return;
                }

                if (ticks % 10 == 0) {
                    for (int i = 0; i < 360; i += 4) {
                        double rad = Math.toRadians(i);
                        double x = Math.cos(rad) * radius;
                        double z = Math.sin(rad) * radius;
                        center.getWorld().spawnParticle(element.getParticle(), center.clone().add(x, 0.2, z), 1, 0, 0, 0, 0);
                        center.getWorld().spawnParticle(element.getParticle(), center.clone().add(x, 1.1, z), 1, 0, 0, 0, 0);
                        center.getWorld().spawnParticle(element.getParticle(), center.clone().add(x, 2.0, z), 1, 0, 0, 0, 0);
                    }
                }

                if (ticks % 20 == 0) {
                    long expireTime = System.currentTimeMillis() + 1500L;

                    for (Entity e : center.getWorld().getNearbyEntities(center, radius, 15, radius)) {
                        if (!(e instanceof LivingEntity)) continue;
                        LivingEntity le = (LivingEntity) e;

                        if (!CombatManager.canTarget(caster, le)) {
                            if (element == MageElement.FIRE) {
                                applyBuffMeta(le, "Mage_Domain_Ally_FIRE", fireDmg, expireTime);
                            }
                            else if (element == MageElement.WIND) {
                                applyBuffMeta(le, "Mage_Domain_Ally_WIND", windSpeed, expireTime);
                                le.setMetadata("Mage_Domain_Ally_WIND_Resist", new FixedMetadataValue(plugin, windResist));
                            }
                            else if (element == MageElement.LIGHT) {
                                applyBuffMeta(le, "Mage_Domain_Ally_LIGHT", lightMana, expireTime);
                            }
                            else if (element == MageElement.WATER) {
                                double maxHp = le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                                double heal = maxHp * waterHeal;

                                if (le instanceof Player) {
                                    RPGPlayer tRpg = RPGCore.getInstance().getPlayerManager().getRPGPlayer(le.getUniqueId());
                                    if (tRpg != null) tRpg.heal(heal);
                                } else {
                                    if (!StatusEffectManager.isHealingBlocked(le)) {
                                        le.setHealth(Math.min(maxHp, le.getHealth() + heal));
                                    }
                                }
                                if (!StatusEffectManager.isHealingBlocked(le)) {
                                    le.getWorld().spawnParticle(Particle.HEART, le.getLocation().add(0, 2, 0), 1, 0.3, 0.3, 0.3, 0);
                                }
                            }

                            if (le instanceof Player) {
                                RPGPlayer tRpg = RPGCore.getInstance().getPlayerManager().getRPGPlayer(le.getUniqueId());
                                if (tRpg != null) { tRpg.updateMaxStats(); tRpg.updateMoveSpeed(); }
                            }
                        }
                        else {
                            if (element == MageElement.DARK) {
                                applyBuffMeta(le, "Mage_Domain_Enemy_DARK", darkWeak, expireTime);
                            }
                            else if (element == MageElement.NATURE) {
                                if (le instanceof Player) {
                                    applyBuffMeta(le, "Mage_Domain_Enemy_NATURE", natureSlow, expireTime);
                                    RPGPlayer tRpg = RPGCore.getInstance().getPlayerManager().getRPGPlayer(le.getUniqueId());
                                    if (tRpg != null) tRpg.updateMoveSpeed();
                                } else {
                                    StatusEffectManager.applyEffect(le, PotionEffectType.SLOWNESS, 40, phase - 1);
                                }
                            }
                            else if (element == MageElement.LIGHT) {
                                double dmg = le.getHealth() * lightBurn;
                                StatusEffectManager.applyTrueDamage(le, dmg);
                                le.getWorld().spawnParticle(Particle.GLOW, le.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void applyBuffMeta(LivingEntity entity, String key, double value, long expireTime) {
        entity.setMetadata(key, new FixedMetadataValue(plugin, value));
        entity.setMetadata(key + "_Expire", new FixedMetadataValue(plugin, expireTime));
    }
}