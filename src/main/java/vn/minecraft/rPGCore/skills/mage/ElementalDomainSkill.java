package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Bukkit;
import org.bukkit.Color;
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
    // HOOK ĐỘC LẬP: XỬ LÝ BUFF/DEBUFF CHO LÃNH VỰC VÀ NHẬN X2 SÁT THƯƠNG KHI CAST
    // ==========================================
    static {
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                double modifier = 0.0;
                // 1. [HÌNH PHẠT]: Người chơi đang niệm chú phải chịu X2 Sát thương (-1.0 tức là tăng 100% ST nhận vào)
                if (victim.hasMetadata("Mage_Domain_Channeling")) {
                    modifier -= 1.0;
                }
                // 2. [DEBUFF BÓNG TỐI]: Kẻ địch đứng trong Domain Bóng Tối bị giảm ST gây ra
                if (attacker != null && attacker.hasMetadata("Mage_Domain_Enemy_DARK")) {
                    if (isValidBuff(attacker, "Mage_Domain_Enemy_DARK")) {
                        modifier += attacker.getMetadata("Mage_Domain_Enemy_DARK").get(0).asDouble();
                    }
                }
                return modifier;
            }

            @Override
            public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity victim) {
                // 3. [BUFF LỬA]: Đồng minh đứng trong Domain Lửa được tăng ST gây ra
                if (attacker.hasMetadata("Mage_Domain_Ally_FIRE")) {
                    if (isValidBuff(attacker, "Mage_Domain_Ally_FIRE")) {
                        return attacker.getMetadata("Mage_Domain_Ally_FIRE").get(0).asDouble();
                    }
                }
                return 0.0;
            }
        });

        CombatManager.registerStatModifier(new CombatManager.IStatModifier() {
            @Override
            public void applyBonusStats(Player player, Map<String, Double> stats) {
                // 4. [BUFF GIÓ]: Tăng Tốc độ & Kháng Khống chế
                if (player.hasMetadata("Mage_Domain_Ally_WIND") && isValidBuff(player, "Mage_Domain_Ally_WIND")) {
                    double speedBuff = player.getMetadata("Mage_Domain_Ally_WIND").get(0).asDouble();
                    stats.put("Tốc độ di chuyển", stats.getOrDefault("Tốc độ di chuyển", 0.0) + (speedBuff * 100.0));
                    stats.put("Kháng hiệu ứng", stats.getOrDefault("Kháng hiệu ứng", 0.0) + 100.0); // Miễn nhiễm 100%
                }
                // 5. [BUFF ÁNH SÁNG]: Tăng Hồi Năng Lượng
                if (player.hasMetadata("Mage_Domain_Ally_LIGHT") && isValidBuff(player, "Mage_Domain_Ally_LIGHT")) {
                    double manaBuff = player.getMetadata("Mage_Domain_Ally_LIGHT").get(0).asDouble();
                    stats.put("Hồi Năng lượng", stats.getOrDefault("Hồi Năng lượng", 0.0) + (manaBuff * 100.0));
                }
            }
        });
    }

    private static boolean isValidBuff(LivingEntity entity, String key) {
        if (!entity.hasMetadata(key + "_Expire")) return false;
        long expire = entity.getMetadata(key + "_Expire").get(0).asLong();
        return System.currentTimeMillis() <= expire;
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

    @Override
    public double getManaCost(int level) {
        return 120.0 + ((level - 1) * 20.0);
    }

    @Override
    public long getCooldown(int level) {
        double cd = 120.0 - ((level - 1) * 12.0); // 120s / 108s / 96s
        return (long) (cd * 1000);
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        int radius = displayLevel == 1 ? 16 : (displayLevel == 2 ? 24 : 36);
        int duration = displayLevel == 1 ? 10 : (displayLevel == 2 ? 22 : 36);
        double baseMana = getManaCost(displayLevel);

        MageElement element = player != null ? ElementalSwapSkill.getActiveElement(player) : MageElement.FIRE;

        lore.add("§7Vận tụ ma lực cực đại để trải rộng Kết Giới.");
        lore.add("§7Đại Lãnh Vực buff sức mạnh mid-game cực kỳ mạnh mẽ.");
        lore.add("");
        lore.add("§8▪ §fNguyên tố hiện tại: " + element.getDisplayName());
        lore.add("");
        lore.add("§e[Kích hoạt - Niệm Chú]");
        lore.add("§f- Bị §cTrói chân tuyệt đối §fvà nhận §cx2 Sát thương§f.");
        lore.add("§f- Thời gian tụ lực chia làm 3 Tầng: §a4s / 12s / 28s§f.");
        lore.add("§f- Tái kích hoạt để hoàn tất và triển khai kết giới.");
        lore.add("§f- Nếu tụ chưa tới Tầng 1 hoặc thiếu Mana: Hủy niệm,");
        lore.add("§f  hoàn trả Mana và giảm thời gian hồi chiêu còn §a90%§f.");
        lore.add("");
        lore.add("§d[Triển Khai - Kết Giới " + element.getDisplayName() + "§d]");
        lore.add("§f- Hao tổn: §b" + (int)baseMana + " / " + (int)(baseMana+40) + " / " + (int)(baseMana+80) + " Mana");
        lore.add("§f- Bán kính: §e" + radius + "m §f| Duy trì: §e" + duration + "s");
        lore.add("§f- Tầng niệm càng cao, hiệu quả Buff càng mạnh:");

        switch (element) {
            case FIRE: lore.add("§f  ➥ Bản thân & Đồng minh: §c+10/20/30% Sát thương"); break;
            case WATER: lore.add("§f  ➥ Bản thân & Đồng minh: §bHồi 1/2/3% Max HP/s"); break;
            case WIND: lore.add("§f  ➥ Bản thân & Đồng minh: §a+20/30/40% Tốc chạy & Miễn Khống Chế"); break;
            case NATURE: lore.add("§f  ➥ Kẻ địch: §2Bị Làm chậm I / II / III"); break;
            case DARK: lore.add("§f  ➥ Kẻ địch: §8Giảm 10/20/30% Sát thương gây ra"); break;
            case LIGHT:
                lore.add("§f  ➥ Bản thân & Đồng minh: §e+30/60/100% Hồi Mana");
                lore.add("§f  ➥ Kẻ địch: §cĐốt 1/2/3% Máu hiện tại mỗi giây (Sát thương chuẩn)");
                break;
        }

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        // Nếu ĐANG NIỆM CHÚ -> Tái kích hoạt để ngắt / triển khai
        if (player.hasMetadata("Mage_Domain_Channel_Task")) {
            triggerDomainRelease(player, rpgPlayer, level);
            return true;
        }

        // KIỂM TRA HỒI CHIÊU TẠI ĐÂY (Vì Mana trừ lúc bung chiêu, CD tính sau khi kết giới sập)
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

        // BẮT ĐẦU NIỆM CHÚ
        startChanneling(player, rpgPlayer, level);
        return true;
    }

    private void startChanneling(Player player, RPGPlayer rpgPlayer, int level) {
        StatusEffectManager.applyAbsoluteRoot(player, 28 * 20); // Khóa 28s max
        player.setMetadata("Mage_Domain_Channeling", new FixedMetadataValue(plugin, true));
        player.setMetadata("Mage_Domain_Phase", new FixedMetadataValue(plugin, 0));

        MageElement element = ElementalSwapSkill.getActiveElement(player);

        player.sendMessage("§e§l[!] §fĐang tập trung ma lực... (Nhấn lại Kỹ năng để triển khai)");

        BukkitRunnable channelTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !player.hasMetadata("Mage_Domain_Channeling")) {
                    cancelChanneling(player);
                    this.cancel();
                    return;
                }

                // 1. Âm thanh Ticking (Nhịp độ và độ cao tăng dần)
                if (ticks % 10 == 0) {
                    float pitch = 0.5f + ((ticks / 560.0f) * 1.5f);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, pitch);
                }

                // 2. Hiệu ứng Hạt tụ vào người
                if (ticks % 2 == 0) {
                    double angle = Math.random() * Math.PI * 2;
                    Location spawnLoc = player.getLocation().add(Math.cos(angle) * 3, 0.5 + Math.random(), Math.sin(angle) * 3);
                    org.bukkit.util.Vector dir = player.getLocation().add(0, 1, 0).toVector().subtract(spawnLoc.toVector()).normalize().multiply(0.4);
                    player.getWorld().spawnParticle(element.getParticle(), spawnLoc, 0, dir.getX(), dir.getY(), dir.getZ(), 0.2);
                }

                // 3. XỬ LÝ MỐC TẦNG (PHASE)
                if (ticks == 80) { // 4s
                    reachPhase(player, 1);
                } else if (ticks == 240) { // 12s
                    reachPhase(player, 2);
                } else if (ticks == 560) { // 28s
                    reachPhase(player, 3);
                    triggerDomainRelease(player, rpgPlayer, level); // Tự động bung chiêu
                    this.cancel();
                    return;
                }

                ticks++;
            }
        };

        channelTask.runTaskTimer(plugin, 0L, 1L);
        player.setMetadata("Mage_Domain_Channel_Task", new FixedMetadataValue(plugin, channelTask.getTaskId()));
    }

    private void reachPhase(Player player, int phase) {
        player.setMetadata("Mage_Domain_Phase", new FixedMetadataValue(plugin, phase));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 1.0f + (phase * 0.2f));
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
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

        // THẤT BẠI: Chưa tới Tầng 1
        if (phase == 0) {
            player.sendMessage("§c§l[!] §fNiệm chú thất bại (Chưa đạt Tầng 1).");
            applyFailureCooldown(player, rpgPlayer, level);
            return;
        }

        // KIỂM TRA MANA TÙY VÀO TẦNG
        double baseMana = getManaCost(level);
        double requiredMana = baseMana + ((phase - 1) * 40.0);

        if (rpgPlayer.getCurrentResource() < requiredMana) {
            player.sendMessage("§c§l[!] §fKhông đủ §b" + requiredMana + " Mana §fđể mở Kết giới Tầng " + phase + "!");
            applyFailureCooldown(player, rpgPlayer, level);
            return;
        }

        // THÀNH CÔNG: Trừ Mana và Bật Lãnh Vực
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - requiredMana);
        MageElement element = ElementalSwapSkill.getActiveElement(player);

        activateDomain(player, rpgPlayer, level, phase, element);
    }

    private void applyFailureCooldown(Player player, RPGPlayer rpgPlayer, int level) {
        long baseCd = getCooldown(level);
        long penaltyCd = (long) (getFinalCooldown(rpgPlayer, baseCd) * 0.9); // Hoàn trả, chỉ phạt 90% CD
        player.setMetadata("CD_" + getId(), new FixedMetadataValue(plugin, System.currentTimeMillis() + penaltyCd));
        startCooldownNotifier(player, penaltyCd);
    }

    // ==========================================
    // VÒNG LẶP CHÍNH CỦA KẾT GIỚI NGUYÊN TỐ
    // ==========================================
    private void activateDomain(Player caster, RPGPlayer rpgCaster, int level, int phase, MageElement element) {
        Location center = caster.getLocation().clone();
        int radius = level == 1 ? 16 : (level == 2 ? 24 : 36);
        int durationTicks = (level == 1 ? 10 : (level == 2 ? 22 : 36)) * 20;

        caster.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
        caster.sendMessage("§d§l[!] TRIỂN KHAI KẾT GIỚI " + element.getDisplayName().toUpperCase() + " (TẦNG " + phase + ")!");

        // Tính toán các hằng số Buff dựa trên Phase
        double buffPower = phase * 0.10; // 10% / 20% / 30% (Fire, Dark)
        double windSpeed = (phase + 1) * 0.10; // 20% / 30% / 40%
        double waterHealPct = phase * 0.01; // 1% / 2% / 3%
        int natureSlowLevel = phase - 1; // 0, 1, 2 (I, II, III)
        double lightManaPct = phase == 1 ? 0.3 : (phase == 2 ? 0.6 : 1.0); // 30% / 60% / 100%
        double lightTrueDmg = phase * 0.01; // 1% / 2% / 3% Current HP

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks || !caster.isOnline()) {
                    caster.sendMessage("§8§o(Kết giới nguyên tố đã khép lại...)");
                    applyGlobalCooldown(caster, rpgCaster, level); // BẮT ĐẦU TÍNH HỒI CHIÊU Ở ĐÂY!
                    this.cancel();
                    return;
                }

                // 1. VẼ VIỀN KẾT GIỚI (Mỗi 10 ticks để đỡ lag)
                if (ticks % 10 == 0) {
                    for (int i = 0; i < 360; i += 10) {
                        double rad = Math.toRadians(i);
                        Location pLoc = center.clone().add(Math.cos(rad) * radius, 0.2, Math.sin(rad) * radius);
                        center.getWorld().spawnParticle(element.getParticle(), pLoc, 1, 0, 0, 0, 0);
                    }
                }

                // 2. QUÉT THỰC THỂ BÊN TRONG (Mỗi 20 ticks = 1s)
                if (ticks % 20 == 0) {
                    long expireTime = System.currentTimeMillis() + 1500L; // Buff tồn tại dư ra 0.5s để chống giật nhịp

                    for (Entity e : center.getWorld().getNearbyEntities(center, radius, 15, radius)) {
                        if (!(e instanceof LivingEntity)) continue;
                        LivingEntity le = (LivingEntity) e;

                        if (isAlly(caster, le)) {
                            // XỬ LÝ ĐỒNG MINH
                            if (element == MageElement.FIRE) {
                                applyBuffMeta(le, "Mage_Domain_Ally_FIRE", buffPower, expireTime);
                            }
                            else if (element == MageElement.WIND) {
                                applyBuffMeta(le, "Mage_Domain_Ally_WIND", windSpeed, expireTime);
                            }
                            else if (element == MageElement.LIGHT) {
                                applyBuffMeta(le, "Mage_Domain_Ally_LIGHT", lightManaPct, expireTime);
                            }
                            else if (element == MageElement.WATER) {
                                double maxHp = le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                                double heal = maxHp * waterHealPct;
                                le.setHealth(Math.min(maxHp, le.getHealth() + heal));
                                le.getWorld().spawnParticle(Particle.HEART, le.getLocation().add(0, 2, 0), 1, 0.3, 0.3, 0.3, 0);
                            }

                            // Cập nhật UpdateStats cho Player Đồng Minh
                            if (le instanceof Player) {
                                RPGPlayer tRpg = RPGCore.getInstance().getPlayerManager().getRPGPlayer(le.getUniqueId());
                                if (tRpg != null) { tRpg.updateMaxStats(); tRpg.updateMoveSpeed(); }
                            }
                        }
                        else if (CombatManager.canTarget(caster, e)) {
                            // XỬ LÝ KẺ ĐỊCH
                            if (element == MageElement.DARK) {
                                applyBuffMeta(le, "Mage_Domain_Enemy_DARK", buffPower, expireTime);
                            }
                            else if (element == MageElement.NATURE) {
                                StatusEffectManager.applyEffect(le, PotionEffectType.SLOWNESS, 40, natureSlowLevel);
                            }
                            else if (element == MageElement.LIGHT) {
                                double dmg = le.getHealth() * lightTrueDmg;
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
                targetOwner = Bukkit.getPlayer(java.util.UUID.fromString(ownerId));
            } catch (Exception ignored) {}
        }
        if (targetOwner != null && targetOwner.isOnline()) {
            for (CombatManager.IAllyChecker checker : CombatManager.allyCheckers) {
                if (checker.isAlly(caster, targetOwner)) return true;
            }
        }
        return false;
    }
}