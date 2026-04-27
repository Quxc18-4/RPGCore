package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
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

                    int level = player.getMetadata("Aegis_Level").get(0).asInt();
                    double reduction = 0.20 + ((level - 1) * 0.02);

                    long lastHit = player.hasMetadata("Aegis_ICD") ? player.getMetadata("Aegis_ICD").get(0).asLong() : 0;
                    if (System.currentTimeMillis() - lastHit > 500) {
                        RPGCore pluginInstance = RPGCore.getInstance();
                        MageElement element = MageElement.valueOf(player.getMetadata("Aegis_Element").get(0).asString());

                        if (attacker != null) {
                            ReactionEngine.applyMarkOnly(attacker, element);
                            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ITEM_SHIELD_BLOCK, 0.8f, 1.5f);
                        }

                        RPGPlayer rpgP = pluginInstance.getPlayerManager().getRPGPlayer(player.getUniqueId());
                        if (rpgP != null) {
                            double currentMana = rpgP.getCurrentResource();

                            // LOGIC MỚI: Nếu lượng Mana hiện tại không đủ chịu đòn ( < 15 )
                            if (currentMana < 15.0) {
                                // Trừ sạch Mana, xóa cờ Active để ép Task dừng và nổ ở tick tiếp theo
                                rpgP.setCurrentResource(0);
                                player.removeMetadata("Aegis_Active", pluginInstance);
                                return reduction; // Vẫn tính giảm dmg cho cú đòn làm vỡ khiên này
                            } else {
                                // Nếu đủ Mana thì trừ bình thường
                                rpgP.setCurrentResource(currentMana - 15.0);
                                double consumed = player.getMetadata("Aegis_Consumed").get(0).asDouble();
                                player.setMetadata("Aegis_Consumed", new FixedMetadataValue(pluginInstance, consumed + 15.0));
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
        req.put("mage_focus", 10);
        req.put("mage_blink", 4);
        return req;
    }

    @Override
    public double getManaCost(int level) {
        // [CẬP NHẬT]: Trả về con số thật để Menu/GUI hiển thị đúng thông số
        // Hàm này giờ đây chỉ có tác dụng hiển thị. Việc trừ Mana sẽ tự làm thủ công.
        return 60.0 + (level * 5.0); // 65, 70, 75, 80, 85, 90
    }

    @Override
    public long getCooldown(int level) {
        double cd = 16.0;
        if (level >= 3) cd -= 1.0;
        if (level >= 6) cd -= 1.0;
        return (long) (cd * 1000);
    }

    public double getActivationThreshold(int level) {
        return 60.0 + (level * 5.0); // 65, 70, 75, 80, 85, 90
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        double dmgReduction = 20 + ((displayLevel - 1) * 2);
        double pulseTicks = 3.0 - ((displayLevel - 1) * 0.2);
        int pulseDmg = (int) ((1.40 + ((displayLevel - 1) * 0.15)) * 100);
        int baseShatterDmg = (int) ((1.0 + ((displayLevel - 1) * 0.20)) * 100);

        MageElement element = player != null ? ElementalSwapSkill.getActiveElement(player) : MageElement.FIRE;

        lore.add("§7Kỹ thuật thực thể hóa ma lực của bản thân.");
        lore.add("§7Chuyển đổi thành lớp khiên nguyên tố bao bọc lấy cơ thể.");
        lore.add("");
        lore.add("§8▪ §fNguyên tố hiện tại: " + element.getDisplayName());
        lore.add("");
        lore.add("§e[Nội tại - Lá Chắn Ma Pháp]");
        lore.add("§f- §fTiêu hao: §b-5 Mana/s §f| §b-15 Mana/Lần chịu đòn");
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
        lore.add("§f- Tích tụ: Mỗi §b1 Mana §fhao tổn cộng §c1% Sát thương§f.");
        lore.add("§f- Sát thương cộng thêm tối đa: §c+500% Sát thương§f.");

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        String cdKey = "CD_" + getId();

        // TRƯỜNG HỢP 1: KHIÊN ĐANG BẬT -> CHỦ ĐỘNG TẮT KHIÊN (KÍCH NỔ)
        if (player.hasMetadata("Aegis_Active")) {
            triggerShatter(player, rpgPlayer, level);
            return true;
        }

        // TRƯỜNG HỢP 2: BẬT KHIÊN -> KIỂM TRA COOLDOWN VÀ MANA TỐI THIỂU
        if (player.hasMetadata(cdKey)) {
            List<org.bukkit.metadata.MetadataValue> metaList = player.getMetadata(cdKey);
            if (!metaList.isEmpty()) {
                long expire = metaList.get(0).asLong();
                if (System.currentTimeMillis() < expire) {
                    double timeLeft = (expire - System.currentTimeMillis()) / 1000.0;
                    player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
                    return false;
                }
            }
            player.removeMetadata(cdKey, plugin);
        }

        double reqMana = getActivationThreshold(level);
        if (rpgPlayer.getCurrentResource() < reqMana) {
            player.sendMessage("§cBạn cần ít nhất " + reqMana + " Mana để kích hoạt Lá Chắn Nguyên Tố!");
            return false;
        }

        // ==========================================
        // KÍCH HOẠT MA KHẢI
        // ==========================================
        MageElement snapshottedElement = ElementalSwapSkill.getActiveElement(player);

        player.setMetadata("Aegis_Active", new FixedMetadataValue(plugin, true));
        player.setMetadata("Aegis_Level", new FixedMetadataValue(plugin, level));
        player.setMetadata("Aegis_Element", new FixedMetadataValue(plugin, snapshottedElement.name()));
        player.setMetadata("Aegis_Consumed", new FixedMetadataValue(plugin, 0.0));

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);

        startAegisLoop(player, rpgPlayer, level, snapshottedElement);
        return true;
    }

    private void startAegisLoop(Player player, RPGPlayer rpgPlayer, int level, MageElement element) {
        int pulseTicks = (int) ((3.0 - (level - 1) * 0.2) * 20);
        double pulseDmgMulti = 1.40 + (level - 1) * 0.15;

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                // LOGIC MỚI: Dọn dẹp rác nếu Chết hoặc Offline
                if (!player.isOnline() || player.isDead()) {
                    player.removeMetadata("Aegis_Active", plugin);
                    player.removeMetadata("Aegis_Level", plugin);
                    player.removeMetadata("Aegis_Element", plugin);
                    player.removeMetadata("Aegis_Consumed", plugin);
                    this.cancel();
                    return;
                }

                // Nếu mất cờ Active (do người chơi tự tắt HOẶC do hook ở trên ép tắt vì cạn Mana)
                if (!player.hasMetadata("Aegis_Active")) {
                    triggerShatter(player, rpgPlayer, level);
                    this.cancel();
                    return;
                }

                // [MỖI GIÂY (20 Ticks)] Trừ 5 Mana duy trì
                if (ticks > 0 && ticks % 20 == 0) {
                    if (rpgPlayer.getCurrentResource() < 5.0) {
                        triggerShatter(player, rpgPlayer, level); // Hết Mana duy trì -> Nổ
                        this.cancel();
                        return;
                    }
                    rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - 5.0);
                    double consumed = player.getMetadata("Aegis_Consumed").get(0).asDouble();
                    player.setMetadata("Aegis_Consumed", new FixedMetadataValue(plugin, consumed + 5.0));
                }

                // ... (Phần logic Pulse và Particle bên dưới giữ nguyên) ...

                // [XUNG NĂNG THEO CHU KỲ]
                if (ticks > 0 && ticks % pulseTicks == 0) {
                    triggerPulse(player, rpgPlayer, element, pulseDmgMulti);
                }

                // [VFX VỎ KHIÊN LƠ LỬNG]
                if (ticks % 5 == 0) {
                    Location pLoc = player.getLocation().add(0, 1, 0);
                    double angle = ticks * 0.2;
                    Location partLoc = pLoc.clone().add(1.2 * Math.cos(angle), 0, 1.2 * Math.sin(angle));
                    Location partLoc2 = pLoc.clone().add(1.2 * Math.cos(angle + Math.PI), 0, 1.2 * Math.sin(angle + Math.PI));

                    player.getWorld().spawnParticle(element.getParticle(), partLoc, 2, 0, 0, 0, 0.02);
                    player.getWorld().spawnParticle(element.getParticle(), partLoc2, 2, 0, 0, 0, 0.02);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void triggerPulse(Player player, RPGPlayer rpgPlayer, MageElement element, double multi) {
        Location center = player.getLocation().add(0, 1, 0);
        player.getWorld().playSound(center, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 0.8f, 1.5f);

        // Vẽ vòng tròn Xung Năng
        for (double t = 0; t < Math.PI * 2; t += Math.PI / 8) {
            Location loc = center.clone().add(2.5 * Math.cos(t), 0, 2.5 * Math.sin(t));
            player.getWorld().spawnParticle(element.getParticle(), loc, 1, 0.1, 0.1, 0.1, 0);
        }

        for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 2.5, 2.5, 2.5)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                LivingEntity target = (LivingEntity) ent;

                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, element);
                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, multi * reaction.damageMultiplier, 0, true, false);
                CombatManager.applySkillDamage(player, target, finalDmg);

                if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
            }
        }
    }

    private void triggerShatter(Player player, RPGPlayer rpgPlayer, int level) {
        // Lấy thông tin lần cuối trước khi xóa
        double consumedMana = player.hasMetadata("Aegis_Consumed") ? player.getMetadata("Aegis_Consumed").get(0).asDouble() : 0.0;
        MageElement element = MageElement.valueOf(player.getMetadata("Aegis_Element").get(0).asString());

        // Dọn dẹp Metadata
        player.removeMetadata("Aegis_Active", plugin);
        player.removeMetadata("Aegis_Level", plugin);
        player.removeMetadata("Aegis_Element", plugin);
        player.removeMetadata("Aegis_Consumed", plugin);

        // Tính sát thương
        double baseShatter = 1.0 + (level - 1) * 0.20;
        double bonusMulti = Math.min(5.0, consumedMana * 0.01); // Cap 500%
        double totalMulti = baseShatter + bonusMulti;

        // KÍCH NỔ VFX & SFX
        Location center = player.getLocation().add(0, 1, 0);
        player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.2f);
        player.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        player.getWorld().spawnParticle(element.getParticle(), center, 100, 2.0, 2.0, 2.0, 0.2);

        // QUÉT MỤC TIÊU
        for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 4.5, 4.5, 4.5)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                LivingEntity target = (LivingEntity) ent;

                // Kích phản ứng & Gây sát thương
                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, element);
                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, totalMulti * reaction.damageMultiplier, 0, true, false);
                CombatManager.applySkillDamage(player, target, finalDmg);

                if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);

                // Đẩy lùi (Hất văng ra xa tâm)
                Vector push = target.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.8).setY(0.4);
                target.setVelocity(target.getVelocity().add(push));
            }
        }

        // Bắt đầu tính Cooldown Global
        applyGlobalCooldown(player, rpgPlayer, level);

        // Gửi thông báo cho ngầu
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§c§lBẠO KÍCH CHUYỂN HÓA! §e+" + (int)(bonusMulti*100) + "% ST"));
    }
}