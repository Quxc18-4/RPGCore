package vn.minecraft.rPGCore.classes.impl.mage;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import org.bukkit.potion.PotionEffectType;

public class ReactionEngine {

    private static final String MARK_PREFIX = "Elemental_Mark_";
    private static final long MARK_DURATION = 5000L; // 5 giây

    // ==========================================
    // KHỞI TẠO HOOK PHÁ GIÁP "HỖN ĐỘN" VÀO COMBAT MANAGER
    // ==========================================
    static {
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getTargetArmorReductionPercentage(LivingEntity target) {
                if (target.hasMetadata("Chaos_Armor_Shred")) {
                    java.util.List<org.bukkit.metadata.MetadataValue> meta = target.getMetadata("Chaos_Armor_Shred");
                    if (!meta.isEmpty() && System.currentTimeMillis() < meta.get(0).asLong()) return 0.20;
                }
                return 0.0;
            }

            @Override
            public double getTargetMagicResistReductionPercentage(LivingEntity target) {
                if (target.hasMetadata("Chaos_Magic_Shred")) { // Đã đổi Key
                    java.util.List<org.bukkit.metadata.MetadataValue> meta = target.getMetadata("Chaos_Magic_Shred");
                    if (!meta.isEmpty() && System.currentTimeMillis() < meta.get(0).asLong()) return 0.20;
                }
                return 0.0;
            }
        });
    }

    public static class ReactionResult {
        public double damageMultiplier = 1.0;
        public boolean isReacted = false;
        public String reactionName = "";
    }

    public static ReactionResult processHit(Player caster, LivingEntity target, MageElement incomingElement) {
        ReactionResult result = new ReactionResult();
        MageElement existingElement = getExistingMark(target);

        // 1. NẾU CHƯA CÓ ẤN -> Gắn ấn mới và kết thúc
        if (existingElement == null) {
            applyMarkOnly(target, incomingElement);
            return result;
        }

        // 2. ĐÃ CÓ ẤN -> KÍCH NỔ PHẢN ỨNG!
        result.isReacted = true;
        clearMark(target); // Xóa ấn cũ sau khi nổ

        // A. CỘNG HƯỞNG CÙNG HỆ (BÙNG NỔ THUẬT PHÁP) -> 115% Sát thương
        if (existingElement == incomingElement) {
            result.damageMultiplier = 1.15;
            result.reactionName = "§b§lBÙNG NỔ THUẬT PHÁP";
            target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
        }
        // B. PHẢN ỨNG KHÁC HỆ
        else {
            // Lửa + Nước = Bốc Hơi (150%)
            if (isReaction(existingElement, incomingElement, MageElement.FIRE, MageElement.WATER)) {
                result.damageMultiplier = 1.50;
                result.reactionName = "§c§lBỐC HƠI";
                target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);
            }
            else if (isReaction(existingElement, incomingElement, MageElement.LIGHT, MageElement.DARK)) {
                result.damageMultiplier = 1.50;
                result.reactionName = "§d§lHỖN ĐỘN";
                // Gắn ĐỒNG THỜI cờ Phá Giáp và Phá Kháng
                target.setMetadata("Chaos_Armor_Shred", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 5000L));
                target.setMetadata("Chaos_Magic_Shred", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 5000L));
                target.getWorld().spawnParticle(Particle.WITCH, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
            }
            // Lửa + Sáng = Chớp Giật (150% + Sét đánh + Tê liệt AoE)
            else if (isReaction(existingElement, incomingElement, MageElement.FIRE, MageElement.LIGHT)) {
                result.damageMultiplier = 1.50;
                result.reactionName = "§e§lCHỚP GIẬT";
                target.getWorld().strikeLightningEffect(target.getLocation()); // Sét đánh visual

                // Tê liệt diện rộng (Slowness cực mạnh trong 2s)
                for (org.bukkit.entity.Entity ent : target.getWorld().getNearbyEntities(target.getLocation(), 4.0, 4.0, 4.0)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(caster, ent)) {
                        StatusEffectManager.applyEffect((LivingEntity) ent, PotionEffectType.SLOWNESS, 40, 10);
                        ent.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, ent.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
                    }
                }
            }
            // Lửa + Thảo = Thiêu Rụi (Đốt cháy 7s)
            else if (isReaction(existingElement, incomingElement, MageElement.FIRE, MageElement.NATURE)) {
                result.damageMultiplier = 1.0;
                result.reactionName = "§6§lTHIÊU RỤI";
                target.setFireTicks(140); // Vanilla xử lý 7 giây -> PlayerListener của bạn sẽ kéo 75 DMG chuẩn mỗi giây
                target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            }
            // Lửa + Tối = Hắc Hỏa (% Máu mất trong 4s)
            else if (isReaction(existingElement, incomingElement, MageElement.FIRE, MageElement.DARK)) {
                result.damageMultiplier = 1.0;
                result.reactionName = "§5§lHẮC HỎA";
                applyBlackFire(caster, target);
            }
            // Nước + Thảo = Sinh Sôi (Trói chân AoE & Hồi Máu Dẫn Hướng)
            else if (isReaction(existingElement, incomingElement, MageElement.WATER, MageElement.NATURE)) {
                result.damageMultiplier = 1.0;
                result.reactionName = "§a§lSINH SÔI";

                // Trói chân diện rộng 3s (60 ticks)
                for (org.bukkit.entity.Entity ent : target.getWorld().getNearbyEntities(target.getLocation(), 4.0, 4.0, 4.0)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(caster, ent)) {
                        StatusEffectManager.applyEffect((LivingEntity) ent, PotionEffectType.SLOWNESS, 60, 10);
                        ent.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, ent.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);
                    }
                }

                // Cắm cờ giám sát Cái chết (Death Monitor)
                monitorDeathForDewdrop(target, caster);
            }
            // Mặc định phản ứng khác
            else {
                result.damageMultiplier = 1.10;
                result.reactionName = "§f§lPHẢN ỨNG";
            }
        }

        caster.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(result.reactionName + " §fKích hoạt!"));

        return result;
    }

    private static boolean isReaction(MageElement e1, MageElement e2, MageElement req1, MageElement req2) {
        return (e1 == req1 && e2 == req2) || (e1 == req2 && e2 == req1);
    }

    // ==========================================
    // LOGIC: ĐẠN DẪN HƯỚNG HỒI MÁU (GIỌT SƯƠNG)
    // ==========================================
    private static void monitorDeathForDewdrop(LivingEntity target, Player caster) {
        // Task này sẽ chạy ngầm và theo dõi con quái bị dính Sinh Sôi trong 3 giây
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 60 || !caster.isOnline()) { // Theo dõi tối đa 3 giây
                    this.cancel();
                    return;
                }

                if (target.isDead()) {
                    this.cancel();
                    spawnHomingHealingProjectile(target.getLocation().add(0, 1, 0), caster);
                    return;
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 1L, 1L);
    }

    private static void spawnHomingHealingProjectile(Location startLoc, Player targetPlayer) {
        new BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            int lifespan = 0;

            @Override
            public void run() {
                // Nếu đạn bay quá 5 giây (100 ticks) hoặc Player offline -> tự hủy
                if (!targetPlayer.isOnline() || targetPlayer.isDead() || lifespan > 100) {
                    this.cancel();
                    return;
                }

                Location targetLoc = targetPlayer.getLocation().add(0, 1, 0);
                Vector dir = targetLoc.toVector().subtract(currentLoc.toVector());
                double dist = dir.length();

                // Nếu đạn bay trúng ngực người chơi -> Hút và Hồi máu
                if (dist < 1.0) {
                    double maxHp = targetPlayer.getAttribute(Attribute.MAX_HEALTH).getValue();
                    double healAmount = maxHp * 0.05; // 5% HP Tối đa

                    targetPlayer.setHealth(Math.min(maxHp, targetPlayer.getHealth() + healAmount));
                    targetPlayer.getWorld().playSound(targetPlayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                    targetPlayer.getWorld().spawnParticle(Particle.HEART, targetPlayer.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);

                    targetPlayer.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§a+ §c" + String.format("%.1f", healAmount) + " ♥ §a(Sinh Sôi)"));

                    this.cancel();
                    return;
                }

                // Di chuyển đạn về phía người chơi (Tốc độ 0.6 blocks/tick)
                currentLoc.add(dir.normalize().multiply(0.6));
                currentLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, currentLoc, 2, 0.1, 0.1, 0.1, 0); // Mảnh sương xanh lá
                currentLoc.getWorld().spawnParticle(Particle.ITEM_SLIME, currentLoc, 1, 0.1, 0.1, 0.1, 0);

                lifespan++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // ==========================================
    // LOGIC: HẮC HỎA (Sát thương chuẩn theo % Máu đã mất)
    // ==========================================
    private static void applyBlackFire(Player caster, LivingEntity target) {
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.5f, 0.5f);
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                // Đốt 4 lần (trong 4s)
                if (ticks >= 4 || target.isDead() || !target.isValid()) {
                    this.cancel(); return;
                }
                target.getWorld().spawnParticle(Particle.LARGE_SMOKE, target.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0);
                target.getWorld().spawnParticle(Particle.SQUID_INK, target.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0);

                double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                double currentHealth = target.getHealth();
                double missingHealth = maxHealth - currentHealth;

                double blackFireDmg = missingHealth * 0.05; // 5% máu đã mất
                blackFireDmg = Math.min(blackFireDmg, 350.0); // Cap sát thương chống phá Boss
                blackFireDmg = Math.max(blackFireDmg, 10.0);

                CombatManager.applySkillDamage(caster, target, blackFireDmg);
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 20L); // Mỗi 20 ticks (1s)
    }

    public static void applyMark(LivingEntity target, MageElement element) {
        target.setMetadata(MARK_PREFIX + "Type", new FixedMetadataValue(RPGCore.getInstance(), element.name()));
        target.setMetadata(MARK_PREFIX + "Expire", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + MARK_DURATION));
        target.getWorld().spawnParticle(element.getParticle(), target.getLocation().add(0, 2.2, 0), 5, 0.1, 0.1, 0.1, 0.01);
    }

    public static MageElement getExistingMark(LivingEntity target) {
        if (!target.hasMetadata(MARK_PREFIX + "Type") || !target.hasMetadata(MARK_PREFIX + "Expire")) return null;

        java.util.List<org.bukkit.metadata.MetadataValue> expireMeta = target.getMetadata(MARK_PREFIX + "Expire");
        java.util.List<org.bukkit.metadata.MetadataValue> typeMeta = target.getMetadata(MARK_PREFIX + "Type");

        // Bọc thép
        if (expireMeta.isEmpty() || typeMeta.isEmpty()) return null;

        long expireTime = expireMeta.get(0).asLong();
        if (System.currentTimeMillis() > expireTime) {
            clearMark(target); return null;
        }
        String typeName = typeMeta.get(0).asString();
        try { return MageElement.valueOf(typeName); } catch (IllegalArgumentException e) { return null; }
    }

    private static void clearMark(LivingEntity target) {
        target.removeMetadata(MARK_PREFIX + "Type", RPGCore.getInstance());
        target.removeMetadata(MARK_PREFIX + "Expire", RPGCore.getInstance());
    }

    public static void applyMarkOnly(LivingEntity target, MageElement element) {
        if (target.hasMetadata("Reaction_Immunity")) {
            java.util.List<org.bukkit.metadata.MetadataValue> meta = target.getMetadata("Reaction_Immunity");
            if (!meta.isEmpty() && System.currentTimeMillis() < meta.get(0).asLong()) return; // Bọc thép
        }
        applyMark(target, element);
    }

    public static void triggerReactionImmunity(LivingEntity target) {
        target.setMetadata("Reaction_Immunity", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 1500L));
    }
}