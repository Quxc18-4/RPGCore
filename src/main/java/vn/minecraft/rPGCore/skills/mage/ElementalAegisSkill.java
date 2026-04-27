package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElementalAegisSkill implements ISkill {

    private final JavaPlugin plugin;

    public ElementalAegisSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // HOOK ĐỘC LẬP: XỬ LÝ KHI MAGE BỊ ĐÁNH TRÚNG LÚC CÓ KHIÊN
    // ==========================================
    static {
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                if (victim.hasMetadata("Aegis_Active") && victim instanceof Player) {
                    Player player = (Player) victim;

                    int level = 1;
                    if (player.hasMetadata("Aegis_Level") && !player.getMetadata("Aegis_Level").isEmpty()) {
                        level = player.getMetadata("Aegis_Level").get(0).asInt();
                    }
                    double reduction = 0.20 + ((level - 1) * 0.02);

                    long lastHit = player.hasMetadata("Aegis_ICD") && !player.getMetadata("Aegis_ICD").isEmpty()
                            ? player.getMetadata("Aegis_ICD").get(0).asLong() : 0;

                    if (System.currentTimeMillis() - lastHit > 500) {
                        RPGCore pluginInstance = RPGCore.getInstance();

                        // Cố gắng lấy Element an toàn
                        MageElement element = MageElement.FIRE;
                        if (player.hasMetadata("Aegis_Element") && !player.getMetadata("Aegis_Element").isEmpty()) {
                            try { element = MageElement.valueOf(player.getMetadata("Aegis_Element").get(0).asString()); }
                            catch (Exception ignored) {}
                        }

                        if (attacker != null) {
                            ReactionEngine.applyMarkOnly(attacker, element);
                            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.8f, 1.5f);
                        }

                        RPGPlayer rpgP = pluginInstance.getPlayerManager().getRPGPlayer(player.getUniqueId());
                        if (rpgP != null) {
                            double currentMana = rpgP.getCurrentResource();

                            // [CÂN BẰNG]: Phạt 20 Mana mỗi lần ăn đòn
                            if (currentMana < 20.0) {
                                rpgP.setCurrentResource(0);
                                player.removeMetadata("Aegis_Active", pluginInstance);
                                return reduction;
                            } else {
                                rpgP.setCurrentResource(currentMana - 20.0);
                                double consumed = player.hasMetadata("Aegis_Consumed") && !player.getMetadata("Aegis_Consumed").isEmpty()
                                        ? player.getMetadata("Aegis_Consumed").get(0).asDouble() : 0.0;
                                player.setMetadata("Aegis_Consumed", new FixedMetadataValue(pluginInstance, consumed + 20.0));
                            }
                        }

                        player.setMetadata("Aegis_ICD", new FixedMetadataValue(pluginInstance, System.currentTimeMillis()));
                    }
                    return reduction;
                }
                return 0.0;
            }
        });
    }

    @Override public String getId() { return "mage_elemental_aegis"; }
    @Override public String getName() { return "Lá Chắn Nguyên Tố"; }
    @Override public String getRequiredClass() { return "mage"; }
    @Override public Material getIcon() { return Material.SHIELD; }
    @Override public boolean isPassiveSkill() { return false; }
    @Override public int getMaxLevel() { return 6; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public int getRequiredPlayerLevel() { return 20; }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        Map<String, Integer> req = new HashMap<>();
        req.put("mage_focus", 4);
        req.put("mage_elemental_swap", 1);
        return req;
    }

    @Override
    public double getManaCost(int level) {
        return 60.0 + (level * 5.0);
    }

    @Override
    public long getCooldown(int level) {
        double cd = 16.0;
        if (level >= 3) cd -= 1.0;
        if (level >= 6) cd -= 1.0;
        return (long) (cd * 1000);
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        double dmgReduction = 20 + ((displayLevel - 1) * 2);

        // [CÂN BẰNG]: Thời gian nổ từ 5.0s giảm dần
        double pulseTicks = 4.0 - ((displayLevel - 1) * 0.2);

        // [CÂN BẰNG]: Sát thương nổ lan 70% -> 145%
        int pulseDmg = (int) ((0.70 + ((displayLevel - 1) * 0.15)) * 100);
        int baseShatterDmg = (int) ((1.0 + ((displayLevel - 1) * 0.20)) * 100);

        MageElement element = player != null ? ElementalSwapSkill.getActiveElement(player) : MageElement.FIRE;

        lore.add("§7Kỹ thuật thực thể hóa ma lực của bản thân.");
        lore.add("§7Chuyển đổi thành lớp khiên nguyên tố bao bọc lấy cơ thể.");
        lore.add("");
        lore.add("§8▪ §fNguyên tố hiện tại: " + element.getDisplayName());
        lore.add("");
        lore.add("§e[Nội tại - Lá Chắn Ma Pháp]");
        lore.add("§f- §fTiêu hao: §b-15 Mana/s §f| §b-20 Mana/Lần chịu đòn");
        lore.add("§f- Giảm §a" + dmgReduction + "% §fSát thương nhận vào.");
        lore.add("§f- Kẻ địch tấn công vào khiên sẽ bị gán §nẤn Nguyên Tố§f.");
        lore.add("");
        lore.add("§c[Nội tại - Xung Năng Lượng]");
        lore.add("§f- Tỏa ra sát thương bán kính 2.5m mỗi §e" + String.format("%.1f", pulseTicks) + "s§f.");
        lore.add("§f- Gây §c" + pulseDmg + "% Sát thương §fvà Kích nổ Phản ứng nguyên tố.");
        lore.add("");
        lore.add("§d[Kích hoạt - Bộc Phá Năng Lượng]");
        lore.add("§f- Nổ tung hất văng mục tiêu xung quanh bản thân.");
        lore.add("§f- Sát thương gốc: §c" + baseShatterDmg + "% Sát thương§f.");
        lore.add("§f- Tích tụ: Mỗi §b5 Mana §fhao tổn cộng §c1% Sát thương§f.");
        lore.add("§f- Sát thương cộng thêm tối đa: §c+500% Sát thương§f.");

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        String cdKey = "CD_" + getId();

        if (player.hasMetadata("Aegis_Active")) {
            // [SỬA LỖI ĐÚP]: Không gọi triggerShatter ở đây.
            // Chỉ cần xóa cờ, vòng lặp bên dưới sẽ tự bắt được và kích nổ 1 lần duy nhất.
            player.removeMetadata("Aegis_Active", plugin);
            return true;
        }

        if (player.hasMetadata(cdKey)) {
            List<MetadataValue> metaList = player.getMetadata(cdKey);
            if (!metaList.isEmpty()) {
                long expire = metaList.get(0).asLong();
                if (System.currentTimeMillis() < expire) {
                    double timeLeft = (expire - System.currentTimeMillis()) / 1000.0;
                    player.sendMessage("§cLá Chắn Nguyên Tố đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
                    return false;
                }
            }
            player.removeMetadata(cdKey, plugin);
        }

        double reqMana = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < reqMana) {
            player.sendMessage("§cBạn cần ít nhất " + reqMana + " Năng lượng để kích hoạt Lá Chắn Nguyên Tố!");
            return false;
        }

        MageElement snapshottedElement = ElementalSwapSkill.getActiveElement(player);

        player.setMetadata("Aegis_Active", new FixedMetadataValue(plugin, true));
        player.setMetadata("Aegis_Level", new FixedMetadataValue(plugin, level));
        player.setMetadata("Aegis_Element", new FixedMetadataValue(plugin, snapshottedElement.name()));
        player.setMetadata("Aegis_Consumed", new FixedMetadataValue(plugin, 0.0));

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1f, 0.5f);
        startAegisLoop(player, rpgPlayer, level, snapshottedElement);
        return true;
    }

    private void startAegisLoop(Player player, RPGPlayer rpgPlayer, int level, MageElement element) {
        int pulseTicks = (int) ((4.0 - (level - 1) * 0.2) * 20);
        double pulseDmgMulti = 0.70 + (level - 1) * 0.15;

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cleanUpMetadata(player);
                    this.cancel();
                    return;
                }

                if (!player.hasMetadata("Aegis_Active")) {
                    triggerShatter(player, rpgPlayer, level);
                    this.cancel();
                    return;
                }

                if (ticks > 0 && ticks % 20 == 0) {
                    if (rpgPlayer.getCurrentResource() < 15.0) {
                        player.removeMetadata("Aegis_Active", plugin);
                        return;
                    }
                    rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - 15.0);

                    double consumed = 0.0;
                    if (player.hasMetadata("Aegis_Consumed") && !player.getMetadata("Aegis_Consumed").isEmpty()) {
                        consumed = player.getMetadata("Aegis_Consumed").get(0).asDouble();
                    }
                    player.setMetadata("Aegis_Consumed", new FixedMetadataValue(plugin, consumed + 15.0));
                }

                if (ticks > 0 && ticks % pulseTicks == 0) {
                    triggerPulse(player, rpgPlayer, element, pulseDmgMulti);
                }

                // [VFX CẬP NHẬT]: Vòng xoắn ốc kép (Double Helix) nhanh hơn
                if (ticks % 2 == 0) { // Chạy mỗi 2 tick cho mượt
                    Location pLoc = player.getLocation();

                    // Tốc độ xoay cực nhanh (0.5 radian/tick)
                    double angle1 = ticks * 0.5;
                    double angle2 = angle1 + Math.PI; // Điểm đối xứng thứ 2

                    // Chiều cao dao động từ 0.5 đến 1.5 bằng hàm Sin
                    double heightOffset = 1.0 + (0.5 * Math.sin(ticks * 0.15));

                    Location partLoc1 = pLoc.clone().add(Math.cos(angle1), heightOffset, Math.sin(angle1));
                    Location partLoc2 = pLoc.clone().add(Math.cos(angle2), heightOffset, Math.sin(angle2));

                    player.getWorld().spawnParticle(element.getParticle(), partLoc1, 1, 0, 0, 0, 0.0);
                    player.getWorld().spawnParticle(element.getParticle(), partLoc2, 1, 0, 0, 0, 0.0);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void triggerPulse(Player player, RPGPlayer rpgPlayer, MageElement element, double multi) {
        // Tâm tỏa ra ở độ cao ngang bụng (1.0 block)
        Location center = player.getLocation().add(0, 1.0, 0);
        player.getWorld().playSound(center, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 0.8f, 1.5f);

        // 1. Quét sát thương (Làm ngay lập tức để không bị delay gameplay)
        for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 2.5, 2.5, 2.5)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                LivingEntity target = (LivingEntity) ent;
                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, element);
                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, multi * reaction.damageMultiplier, 0, true, false);
                CombatManager.applySkillDamage(player, target, finalDmg);
            }
        }

        // 2. [VFX CẬP NHẬT]: Vẽ làn sóng tỏa ra từ tâm
        new BukkitRunnable() {
            double radius = 0.5;
            @Override
            public void run() {
                if (radius > 2.5) {
                    this.cancel();
                    return;
                }
                // [ĐÃ GIẢM 40%]: Từ radius * 16 xuống còn radius * 10
                int particles = (int) (radius * 6);
                for (int i = 0; i < particles; i++) {
                    double angle = 2 * Math.PI * i / particles;
                    Location loc = center.clone().add(radius * Math.cos(angle), 0, radius * Math.sin(angle));
                    player.getWorld().spawnParticle(element.getParticle(), loc, 1, 0.0, 0.0, 0.0, 0);
                }
                radius += 0.5; // Dạt ra siêu nhanh
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void triggerShatter(Player player, RPGPlayer rpgPlayer, int level) {
        // [Phần bọc thép tính Consumed Mana và lấy Element giữ nguyên ở đầu]
        double consumedMana = 0.0;
        if (player.hasMetadata("Aegis_Consumed")) {
            List<MetadataValue> metaList = player.getMetadata("Aegis_Consumed");
            if (!metaList.isEmpty()) consumedMana = metaList.get(0).asDouble();
        }

        MageElement element = MageElement.FIRE;
        if (player.hasMetadata("Aegis_Element")) {
            List<MetadataValue> metaList = player.getMetadata("Aegis_Element");
            if (!metaList.isEmpty()) {
                try { element = MageElement.valueOf(metaList.get(0).asString()); } catch (Exception ignored) {}
            }
        }

        cleanUpMetadata(player);

        double baseShatter = 1.0 + (level - 1) * 0.20;
        double bonusMulti = Math.min(5.0, (consumedMana / 5.0) * 0.01);
        double totalMulti = baseShatter + bonusMulti;

        Location center = player.getLocation().add(0, 1.0, 0);

        // ==========================================
        // [VFX CẬP NHẬT]: HIỆU ỨNG ĐỘC QUYỀN TỪNG HỆ (Đã giảm 40% hạt)
        // ==========================================
        switch (element) {
            case FIRE:
                player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.0f);
                player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
                player.getWorld().spawnParticle(Particle.FLAME, center, 36, 2.5, 2.5, 2.5, 0.15); // 60 -> 36
                player.getWorld().spawnParticle(Particle.LAVA, center, 12, 2.0, 2.0, 2.0, 0.1);  // 20 -> 12
                break;
            case WATER:
                player.getWorld().playSound(center, Sound.ENTITY_GENERIC_SPLASH, 1.5f, 0.5f);
                player.getWorld().spawnParticle(Particle.SPLASH, center, 60, 2.5, 2.5, 2.5, 0.1); // 100 -> 60
                player.getWorld().spawnParticle(Particle.BUBBLE_POP, center, 24, 2.0, 2.0, 2.0, 0.1); // 40 -> 24
                break;
            case WIND:
                player.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 1.2f);
                player.getWorld().spawnParticle(Particle.CLOUD, center, 30, 3.0, 0.5, 3.0, 0.2); // 50 -> 30
                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center, 6, 2.5, 0.5, 2.5, 0.0); // 10 -> 6
                break;
            case NATURE:
                player.getWorld().playSound(center, Sound.BLOCK_AZALEA_LEAVES_BREAK, 1.5f, 0.8f);
                player.getWorld().spawnParticle(Particle.CHERRY_LEAVES, center, 48, 2.5, 2.5, 2.5, 0.1); // 80 -> 48
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center, 24, 2.0, 2.0, 2.0, 0.0); // 40 -> 24
                break;
            case LIGHT:
                player.getWorld().playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.5f);
                player.getWorld().spawnParticle(Particle.END_ROD, center, 36, 2.5, 2.5, 2.5, 0.15); // 60 -> 36
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center, 18, 2.0, 2.0, 2.0, 0.1); // 30 -> 18
                break;
            case DARK:
                player.getWorld().playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.5f);
                player.getWorld().spawnParticle(Particle.WITCH, center, 48, 2.5, 2.5, 2.5, 0.1); // Đổi WITCH -> SPELL_WITCH, 80 -> 48
                player.getWorld().spawnParticle(Particle.SQUID_INK, center, 24, 2.0, 2.0, 2.0, 0.1); // 40 -> 24
                break;
        }

        // Quét sát thương hất văng (Giữ nguyên)
        for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 4.5, 4.5, 4.5)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                LivingEntity target = (LivingEntity) ent;
                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, element);
                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, totalMulti * reaction.damageMultiplier, 0, true, false);
                CombatManager.applySkillDamage(player, target, finalDmg);
                Vector push = target.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.8).setY(0.4);
                target.setVelocity(target.getVelocity().add(push));
            }
        }

        applyGlobalCooldown(player, rpgPlayer, level);
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§c§lBỘC PHÁ! §e+" + (int)(bonusMulti*100) + "% Sát thương"));
    }

    private void cleanUpMetadata(Player player) {
        player.removeMetadata("Aegis_Active", plugin);
        player.removeMetadata("Aegis_Level", plugin);
        player.removeMetadata("Aegis_Element", plugin);
        player.removeMetadata("Aegis_Consumed", plugin);
    }
}