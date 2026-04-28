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
import org.bukkit.potion.PotionEffect;
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
        StatusEffectManager.applyAbsoluteRoot(player, 35 * 20); // Khóa dư ra để chờ nổ xong mới mở
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

                // 1. Âm thanh nhịp điệu
                if (ticks % 10 == 0) {
                    float pitch = 0.5f + ((ticks / 560.0f) * 1.5f);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, pitch);
                }

                // 2. [VFX]: Hạt tụ vào người (Dừng lại tại tâm)
                if (ticks % 2 == 0) {
                    double angle = Math.random() * Math.PI * 2;
                    Location spawnLoc = player.getLocation().add(Math.cos(angle) * 3, 0.5 + Math.random() * 1.5, Math.sin(angle) * 3);
                    org.bukkit.util.Vector dir = player.getLocation().add(0, 1, 0).toVector().subtract(spawnLoc.toVector()).normalize();
                    // Tốc độ 0.3 giúp hạt dừng lại nhanh hơn khi chạm người
                    player.getWorld().spawnParticle(element.getParticle(), spawnLoc, 0, dir.getX(), dir.getY(), dir.getZ(), 0.3);
                }

                // 3. Vẽ các vòng tròn đã hạ cánh (Persistent Rings)
                drawPersistentRings(player, element);

                // 4. [TIMING]: Xử lý logic tầng và hiệu ứng rơi/nối
                handlePhaseAnimation(player, ticks, element);

                // 5. Mốc tầng logic
                if (ticks == 80) reachPhaseLogic(player, 1);
                else if (ticks == 320) reachPhaseLogic(player, 2); // 16s (4s đầu + 12s sau)
                else if (ticks == 560) { // 28s
                    reachPhaseLogic(player, 3);
                    new BukkitRunnable() { // Đợi vòng 3 đặt xong rồi mới nổ
                        @Override public void run() { triggerDomainRelease(player, rpgPlayer, level); }
                    }.runTaskLater(plugin, 20L); // Delay 1s cho vòng cuối đặt xong
                    this.cancel();
                }
                ticks++;
            }
        };
        channelTask.runTaskTimer(plugin, 0L, 1L);
        player.setMetadata("Mage_Domain_Channel_Task", new FixedMetadataValue(plugin, channelTask.getTaskId()));
    }

    private void handlePhaseAnimation(Player player, int ticks, MageElement element) {
        // Tầng 1 (0-4s): 0-60 ticks Rơi, 60-80 ticks Nối
        if (ticks < 60) drawDroppingRing(player, element, 2.0, 2.0 - (ticks * 0.03));
        else if (ticks < 80) drawLinkToPlayer(player, element, 2.0);

        // Tầng 2 (4-16s): 80-280 ticks Rơi, 280-320 ticks Nối
        if (ticks >= 80 && ticks < 280) drawDroppingRing(player, element, 3.0, 2.0 - ((ticks - 80) * 0.01));
        else if (ticks >= 280 && ticks < 320) drawLinkBetweenRings(player, element, 3.0, 2.0);

        // Tầng 3 (16-28s): 320-480 ticks Rơi, 480-560 ticks Nối
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
    }

    private void reachPhase(Player player, int phase, MageElement element) {
        player.setMetadata("Mage_Domain_Phase", new FixedMetadataValue(plugin, phase));

        // [VFX MỚI]: Âm thanh Ma Thuật (Ting + Rền)
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.0f + (phase * 0.2f));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.5f, 0.8f + (phase * 0.2f));

        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§e§lĐẠT TẦNG " + phase + "!"));

        // [VFX MỚI]: Vẽ vòng tròn rớt xuống từ từ theo từng tầng
        double startRadius = 1.5 + (phase * 0.5); // T1: 2.0m, T2: 2.5m, T3: 3.0m

        new BukkitRunnable() {
            int dropTicks = 0;
            double currentHeight = 2.0; // Bắt đầu từ trên đỉnh đầu rớt xuống

            @Override
            public void run() {
                if (dropTicks > 20 || !player.isOnline()) { // Rớt trong 1 giây
                    this.cancel();
                    return;
                }

                Location center = player.getLocation();
                for (int i = 0; i < 360; i += 15) {
                    double rad = Math.toRadians(i);
                    Location pLoc = center.clone().add(Math.cos(rad) * startRadius, currentHeight, Math.sin(rad) * startRadius);
                    player.getWorld().spawnParticle(element.getParticle(), pLoc, 1, 0, 0, 0, 0);
                }

                currentHeight -= 0.1; // Mỗi tick hạ vòng xuống 0.1 block
                dropTicks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
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

    private void applyFailureCooldown(Player player, RPGPlayer rpgPlayer, int intLevel) { // Sửa tên biến level cho không trùng nếu có
        long baseCd = getCooldown(intLevel);
        // [FIX TOÁN HỌC]: Hoàn trả 90% nghĩa là người chơi CHỈ PHẢI CHỊU 10% thời gian hồi chiêu
        long penaltyCd = (long) (getFinalCooldown(rpgPlayer, baseCd) * 0.1);
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
        caster.getWorld().playSound(center, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.5f, 0.5f);
        caster.sendMessage("§d§l[!] TRIỂN KHAI KẾT GIỚI " + element.getDisplayName().toUpperCase() + " (TẦNG " + phase + ")!");

        // 1. SÓNG NỔ DẠT RA TỪ TÂM (MỞ KHÓA DI CHUYỂN KHI SÓNG CHẠM RÌA)
        new BukkitRunnable() {
            double currentRadius = 1.0;
            @Override
            public void run() {
                if (currentRadius >= radius) {
                    StatusEffectManager.removeAbsoluteRoot(caster); // Giờ mới cho phép di chuyển
                    this.cancel();
                    return;
                }
                int particleCount = (int) (currentRadius * 15);
                for (int i = 0; i < particleCount; i++) {
                    double angle = 2 * Math.PI * i / particleCount;
                    Location loc = center.clone().add(Math.cos(angle) * currentRadius, 0.2, Math.sin(angle) * currentRadius);
                    center.getWorld().spawnParticle(element.getParticle(), loc, 1, 0, 0, 0, 0);
                }
                currentRadius += 3.0; // Tốc độ tỏa ra siêu tốc
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Các hằng số Buff
        double buffPower = phase * 0.10;
        double windSpeed = (phase + 1) * 0.10;
        double waterHealPct = phase * 0.01;
        int natureSlowLevel = phase - 1;
        double lightManaPct = phase == 1 ? 0.3 : (phase == 2 ? 0.6 : 1.0);
        double lightTrueDmg = phase * 0.01;

        // 2. VÒNG LẶP DUY TRÌ LÃNH VỰC
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks || !caster.isOnline()) {
                    caster.sendMessage("§8§o(Kết giới nguyên tố đã khép lại...)");
                    applyGlobalCooldown(caster, rpgCaster, level);
                    this.cancel();
                    return;
                }

                // VẼ RÌA KẾT GIỚI (Dày hơn, cao hơn 2 block)
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

                // QUÉT THỰC THỂ BÊN TRONG (Mỗi 20 ticks = 1s)
                if (ticks % 20 == 0) {
                    long expireTime = System.currentTimeMillis() + 1500L;

                    for (Entity e : center.getWorld().getNearbyEntities(center, radius, 15, radius)) {
                        if (!(e instanceof LivingEntity)) continue;
                        LivingEntity le = (LivingEntity) e;

                        // [TỐI ƯU CỰC MẠNH]: Tận dụng logic đã có của CombatManager
                        if (!CombatManager.canTarget(caster, le)) {
                            // =============================
                            // XỬ LÝ ĐỒNG MINH (Bao gồm Bản thân, Pet, Party/Guild)
                            // =============================
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

                            if (le instanceof Player) {
                                RPGPlayer tRpg = RPGCore.getInstance().getPlayerManager().getRPGPlayer(le.getUniqueId());
                                if (tRpg != null) { tRpg.updateMaxStats(); tRpg.updateMoveSpeed(); }
                            }
                        }
                        else {
                            // =============================
                            // XỬ LÝ KẺ ĐỊCH (Chỉ chạy khi canTarget = true)
                            // =============================
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
}