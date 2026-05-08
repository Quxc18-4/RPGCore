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
    @Override
    public Material getIcon() {
        return Material.ENCHANTING_TABLE; // Đổi sang Bàn Phù Phép cho bớt trùng lặp
    }
    @Override public boolean isPassiveSkill() { return false; }
    @Override public int getMaxLevel() { return 3; }
    @Override public int getUpgradeCost(int currentLevel) { return 2; }
    @Override public int getRequiredPlayerLevel() { return 28; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override public double getManaCost(int level) { return 120.0 + ((level - 1) * 20.0); }
    @Override public long getCooldown(int level) { return (long) ((120.0 - (level - 1) * 12.0) * 1000); }

    @Override public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        List<String> lore = new ArrayList<>();
        int lv = Math.max(1, level);
        MageElement element = player != null ? ElementalSwapSkill.getActiveElement(player) : MageElement.FIRE;

        // Xây dựng giá trị động (Thời gian) dựa trên cấp độ hiện tại
        StringBuilder durationBuilder = new StringBuilder("§a10s");
        StringBuilder castTimeBuilder = new StringBuilder("§a4s");

        if (lv >= 2) {
            durationBuilder.append(" / 22s");
            castTimeBuilder.append(" / 16s");
        }
        if (lv >= 3) {
            durationBuilder.append(" / 36s");
            castTimeBuilder.append(" / 28s");
        }

        lore.add("§fNguyên tố hiện tại: " + element.getDisplayName());
        lore.add("");

        lore.add("§d[Thi triển - Niệm chú]");
        lore.add("§f- Bản thân không thể di chuyển và nhận §c200% Sát thương");
        lore.add("§f  từ mọi nguồn.");
        lore.add("§f- Thời gian niệm để thăng tầng: " + castTimeBuilder.toString() + "§f.");
        lore.add("§f- Từ Tầng 2 trở đi, ma pháp trận yêu cầu tiêu tốn thêm");
        lore.add("§f  §b40 Mana §fmỗi tầng khi giải phóng.");
        lore.add("");

        String domainName = "";
        switch (element) {
            case FIRE: domainName = "§cLiệt Hỏa Lãnh Vực"; break;
            case WATER: domainName = "§bHải Thần Lãnh Vực"; break;
            case WIND: domainName = "§aPhong Thần Lãnh Vực"; break;
            case NATURE: domainName = "§2Mộc Thần Lãnh Vực"; break;
            case LIGHT: domainName = "§eThánh Quang Lãnh Vực"; break;
            case DARK: domainName = "§5Hư Không Lãnh Vực"; break;
        }

        lore.add("§e[Kích hoạt - " + domainName + "§e]");

        // Mảng chứa các giá trị cho từng tầng
        String[] fireVals = {"10", "20", "35"};
        String[] waterVals = {"1", "1.5", "2"};
        String[] windSpdVals = {"30", "50", "80"};
        String[] windResVals = {"20", "40", "60"};
        String[] natureVals = {"15", "30", "45"};
        String[] darkVals = {"10", "20", "30"};
        String[] lightManaVals = {"25", "40", "60"};
        String[] lightBurnVals = {"0.5", "0.5", "1"};

        // Xây dựng giá trị động (Chỉ số) dựa trên cấp độ hiện tại
        StringBuilder stat1 = new StringBuilder();
        StringBuilder stat2 = new StringBuilder();

        for (int i = 0; i < lv; i++) {
            String sep = (i == 0) ? "" : " / "; // Dấu phân cách các tầng
            switch (element) {
                case FIRE: stat1.append(sep).append(fireVals[i]); break;
                case WATER: stat1.append(sep).append(waterVals[i]); break;
                case WIND:
                    stat1.append(sep).append(windSpdVals[i]);
                    stat2.append(sep).append(windResVals[i]);
                    break;
                case NATURE: stat1.append(sep).append(natureVals[i]); break;
                case DARK: stat1.append(sep).append(darkVals[i]); break;
                case LIGHT:
                    stat1.append(sep).append(lightManaVals[i]);
                    stat2.append(sep).append(lightBurnVals[i]);
                    break;
            }
        }

        // Lắp ráp vào hiển thị
        switch (element) {
            case FIRE:
                lore.add("§f- Đồng minh: §cTăng " + stat1.toString() + " % §fSát thương gây ra.");
                break;
            case WATER:
                lore.add("§f- Đồng minh: §bHồi " + stat1.toString() + " % §fMáu tối đa/s.");
                break;
            case WIND:
                lore.add("§f- Đồng minh: §aTăng " + stat1.toString() + " % §fTốc chạy & §a" + stat2.toString() + " % §fKháng hiệu ứng.");
                break;
            case NATURE:
                lore.add("§f- Kẻ địch: §2Giảm " + stat1.toString() + " % §fTốc độ di chuyển.");
                break;
            case DARK:
                lore.add("§f- Kẻ địch: §8Giảm " + stat1.toString() + " % §fSát thương gây ra.");
                break;
            case LIGHT:
                lore.add("§f- Đồng minh: §eTăng " + stat1.toString() + " % §fTốc độ hồi Mana.");
                lore.add("§f- Kẻ địch: §cBị Thiêu Đốt " + stat2.toString() + " % §fMáu hiện tại.");
                break;
        }
        lore.add("§f- Thời gian duy trì: " + durationBuilder.toString());

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

                // Vẽ Ma pháp trận (Truyền thêm ticks để tạo hiệu ứng xoay)
                drawPersistentRings(player, element, ticks);
                handlePhaseAnimation(player, ticks, element);

                // ==========================================
                // ÉP MỞ KHÓA TẦNG THEO LEVEL
                // ==========================================
                if (ticks == 80) { // Đạt 4s -> Tầng 1
                    reachPhaseLogic(player, 1);
                    if (level == 1) {
                        new BukkitRunnable() {
                            @Override public void run() { triggerDomainRelease(player, rpgPlayer, level); }
                        }.runTaskLater(plugin, 5L);
                        this.cancel();
                        return;
                    }
                } else if (ticks == 320) { // Đạt 16s -> Tầng 2
                    reachPhaseLogic(player, 2);
                    if (level == 2) {
                        new BukkitRunnable() {
                            @Override public void run() { triggerDomainRelease(player, rpgPlayer, level); }
                        }.runTaskLater(plugin, 5L);
                        this.cancel();
                        return;
                    }
                } else if (ticks == 560) { // Đạt 28s -> Tầng 3
                    reachPhaseLogic(player, 3);
                    new BukkitRunnable() {
                        @Override public void run() { triggerDomainRelease(player, rpgPlayer, level); }
                    }.runTaskLater(plugin, 20L);
                    this.cancel();
                    return;
                }
                ticks++;
            }
        };
        channelTask.runTaskTimer(plugin, 0L, 1L);
        player.setMetadata("Mage_Domain_Channel_Task", new FixedMetadataValue(plugin, channelTask.getTaskId()));
    }

    private void handlePhaseAnimation(Player player, int ticks, MageElement element) {
        // Chỉ làm nhiệm vụ hạ các vòng sáng từ trên trời rơi xuống (Giảm mật độ hạt)
        if (ticks < 80) drawDroppingRing(player, element, 2.0, 2.0 - (ticks * 0.025));
        else if (ticks >= 80 && ticks < 320) drawDroppingRing(player, element, 3.0, 2.0 - ((ticks - 80) * 0.008));
        else if (ticks >= 320 && ticks < 560) drawDroppingRing(player, element, 4.0, 2.0 - ((ticks - 320) * 0.008));
    }

    private void drawPersistentRings(Player player, MageElement element, int ticks) {
        int phase = player.hasMetadata("Mage_Domain_Phase") ? player.getMetadata("Mage_Domain_Phase").get(0).asInt() : 0;

        // [NGHỆ THUẬT]: Các vòng xoay ngược chiều nhau dựa trên thời gian (ticks)
        double rot1 = ticks * 0.05;  // Xoay nhanh, thuận chiều
        double rot2 = -ticks * 0.03; // Xoay vừa, ngược chiều
        double rot3 = ticks * 0.02;  // Xoay chậm, thuận chiều

        if (phase >= 1) {
            drawStaticRing(player, element, 2.0, rot1);
            // Nối từ tâm (0.5) ra Vòng 1 (2.0), uốn cong góc Pi/1.5
            drawMagicRuneLinks(player, element, 0.5, 2.0, 5, Math.PI / 1.5, rot1);
        }
        if (phase >= 2) {
            drawStaticRing(player, element, 3.0, rot2);
            // Nối từ Vòng 1 ra Vòng 2, uốn cong ngược lại
            drawMagicRuneLinks(player, element, 2.0, 3.0, 7, -Math.PI / 1.5, rot2);
        }
        if (phase >= 3) {
            drawStaticRing(player, element, 4.0, rot3);
            drawMagicRuneLinks(player, element, 3.0, 4.0, 9, Math.PI / 1.5, rot3);
        }
    }

    private void drawDroppingRing(Player player, MageElement element, double radius, double height) {
        Location center = player.getLocation().add(0, Math.max(0.1, height), 0);
        for (int i = 0; i < 360; i += 30) {
            double rad = Math.toRadians(i);
            Location loc = center.clone().add(Math.cos(rad) * radius, 0, Math.sin(rad) * radius);
            spawnRuneParticle(loc, element); // [ĐÃ SỬA]
        }
    }

    private void drawStaticRing(Player player, MageElement element, double radius, double rotOffset) {
        Location center = player.getLocation().add(0, 0.1, 0);
        for (int i = 0; i < 360; i += 24) {
            double rad = Math.toRadians(i) + rotOffset;
            Location loc = center.clone().add(Math.cos(rad) * radius, 0, Math.sin(rad) * radius);
            spawnRuneParticle(loc, element); // [ĐÃ SỬA]
        }
    }

    // [HÀM MỚI]: Vẽ các nhánh xoắn ốc (Spiral Arms) nghệ thuật
    private void drawMagicRuneLinks(Player player, MageElement element, double rInner, double rOuter, int arms, double curve, double rotOffset) {
        Location center = player.getLocation().add(0, 0.1, 0);
        for (int i = 0; i < arms; i++) {
            double baseAngle = Math.toRadians((360.0 / arms) * i) + rotOffset;
            for (double t = 0; t <= 1.0; t += 0.2) {
                double r = rInner + t * (rOuter - rInner);
                double theta = baseAngle + (t * curve);

                Location loc = center.clone().add(r * Math.cos(theta), 0, r * Math.sin(theta));
                spawnRuneParticle(loc, element); // [ĐÃ SỬA]
            }
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

        // [ĐÃ SỬA]: Tính toán thời gian duy trì Kết Giới
        int durationTicks = (level == 1 ? 10 : (level == 2 ? 22 : 36)) * 20;
        long durationMillis = durationTicks * 50L;

        // Gọi hàm applyGlobalCooldown từ ISkill, CỘNG THÊM thời gian duy trì của Kết giới
        long baseCd = getCooldown(level);
        long finalCd = getFinalCooldown(rpgPlayer, baseCd) + durationMillis;

        player.setMetadata("CD_" + getId(), new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

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
        caster.sendMessage("§d§l[!] TRIỂN KHAI KẾT GIỚI " + element.getDisplayName().toUpperCase() + " (Tầng " + phase + ")!");

        // Khối 1: Lãnh vực tỏa ra (Giảm từ 15 -> 8)
        new BukkitRunnable() {
            double currentRadius = 1.0;
            @Override
            public void run() {
                if (!caster.isOnline() || currentRadius >= radius) {
                    if (caster.isOnline()) StatusEffectManager.removeAbsoluteRoot(caster);
                    this.cancel();
                    return;
                }
                int particleCount = (int) (currentRadius * 8); // [GIẢM HẠT TỎA RA]
                for (int i = 0; i < particleCount; i++) {
                    double angle = 2 * Math.PI * i / particleCount;
                    Location loc = center.clone().add(Math.cos(angle) * currentRadius, 0.2, Math.sin(angle) * currentRadius);
                    spawnRuneParticle(loc, element);
                }
                currentRadius += 3.0;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Khối 2: Tường lãnh vực duy trì
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks || !caster.isOnline() || caster.isDead()) {
                    if (caster.isOnline()) caster.sendMessage("§8§o(Kết giới nguyên tố đã khép lại...)");
                    this.cancel();
                    return;
                }

                // [GIẢM HẠT TƯỜNG DUY TRÌ]: Từ 10 ticks -> 15 ticks mới chớp 1 lần. Khoảng cách hạt từ 4 độ -> 12 độ
                if (ticks % 15 == 0) {
                    double rot = ticks * 0.02; // Tường lãnh vực cũng xoay nhẹ cho đồng bộ
                    for (int i = 0; i < 360; i += 12) {
                        double rad = Math.toRadians(i) + rot;
                        double x = Math.cos(rad) * radius;
                        double z = Math.sin(rad) * radius;
                        // Bỏ hẳn tường lớp thứ 3 (y=2.0) để tránh rối mắt, chỉ lấy lớp chân (0.2) và lớp hông (1.0)
                        Location loc1 = center.clone().add(x, 0.2, z);
                        Location loc2 = center.clone().add(x, 1.0, z);
                        spawnRuneParticle(loc1, element); // [ĐÃ SỬA]
                        spawnRuneParticle(loc2, element); // [ĐÃ SỬA]
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
                                // Tự động xử lý thông minh (-15/30/45% cho Player, Slowness cho Quái)
                                StatusEffectManager.applySlow(le, 40, natureSlow, phase - 1);
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

    // ==========================================
    // [HỆ THỐNG VFX]: HẠT MA PHÁP TRẬN CHUYÊN DỤNG
    // ==========================================
    private void spawnRuneParticle(Location loc, MageElement element) {
        org.bukkit.Color color;
        float size = 1.0f; // Kích thước hạt mặc định

        switch (element) {
            case FIRE: color = org.bukkit.Color.fromRGB(255, 60, 0); break; // Đỏ cam rực
            case WATER: color = org.bukkit.Color.fromRGB(0, 150, 255); break; // Xanh đại dương
            case WIND: color = org.bukkit.Color.fromRGB(150, 255, 200); break; // Xanh ngọc nhạt
            case NATURE: color = org.bukkit.Color.fromRGB(50, 200, 50); break; // Xanh lục
            case LIGHT: color = org.bukkit.Color.fromRGB(255, 255, 100); break; // Vàng ánh kim
            case DARK:
                color = org.bukkit.Color.fromRGB(120, 0, 180); // Tím Hư Không sắc nét
                size = 1.3f; // Cho hạt Dark to hơn một chút để rõ ràng dứt khoát
                break;
            default: color = org.bukkit.Color.WHITE; break;
        }

        Particle.DustOptions dust = new Particle.DustOptions(color, size);
        // Force spawn (tham số true) để đảm bảo người chơi ở xa vẫn nhìn thấy
        loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0.0, dust, true);
    }
}