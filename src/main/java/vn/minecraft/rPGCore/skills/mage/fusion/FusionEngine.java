package vn.minecraft.rPGCore.skills.mage.fusion;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.impl.*;

import java.util.HashMap;
import java.util.Map;

public class FusionEngine {

    private static final Map<String, IFusion> FUSION_REGISTRY = new HashMap<>();

    private static String getFusionKey(MageElement e1, MageElement e2) {
        if (e1.name().compareTo(e2.name()) > 0) {
            return e2.name() + "_" + e1.name();
        }
        return e1.name() + "_" + e2.name();
    }

    public static void registerFusion(IFusion fusion) {
        String key = getFusionKey(fusion.getElement1(), fusion.getElement2());
        FUSION_REGISTRY.put(key, fusion);
    }

    public static void init() {
        FUSION_REGISTRY.clear();
        // Các phản ứng sẽ được nạp vào đây
        // registerFusion(new PyroclasmFusion()); // Sẽ thêm Lửa + Lửa vào sau
        registerFusion(new VaporizeFusion());
        registerFusion(new ChaosFusion());
        registerFusion(new OvergrowthFusion());
        registerFusion(new LightningFusion());
        registerFusion(new ScorchFusion());
        registerFusion(new BlackFireFusion());
        //Thuần 1 hệ
        registerFusion(new DarkRiftFusion());
        registerFusion(new GeyserFusion());
        registerFusion(new GaleDragFusion());
        registerFusion(new PyroclasmFusion());
        registerFusion(new VinePathFusion());
        registerFusion(new LightBeamFusion());
    }

    // ==========================================
    // [LÕI XỬ LÝ]: ĐỊNH TUYẾN VÀ KÍCH NỔ
    // ==========================================
    public static void processFusionHit(Player caster, RPGPlayer rpgPlayer, Location loc, Vector direction, MageElement tnt, MageElement nth, double baseMultiplier) {
        String key = getFusionKey(tnt, nth);
        IFusion fusion = FUSION_REGISTRY.get(key);

        // [NGOẠI LỆ]: Đại Hỏa Cầu (Lửa + Lửa) được phép chạy dù trùng hệ
        boolean isPyroclasm = (tnt == MageElement.FIRE && nth == MageElement.FIRE);

        if (fusion != null && (tnt != nth || isPyroclasm)) {
            // [TRƯỜNG HỢP 1]: Giao toàn quyền (Sát thương, VFX, Hitbox) cho Class Phản Ứng
            fusion.executeVisualAndDamage(caster, rpgPlayer, loc, direction, baseMultiplier);

            // Thông báo UI cho người chơi
            caster.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(fusion.getReactionName() + " §fKích hoạt!"));
        } else {
            // [TRƯỜNG HỢP 2]: KÍCH HOẠT MẶC ĐỊNH (BÙNG NỔ THUẬT PHÁP)
            // Áp dụng cho các trường hợp trùng hệ (trừ Lửa + Lửa) hoặc hệ chưa code
            double finalDmgMulti = baseMultiplier * 1.15; // Hệ số mặc định

            loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.2f, 0.8f);
            loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
            loc.getWorld().spawnParticle(nth.getParticle(), loc, 40, 2.5, 2.5, 2.5, 0.1);

            for (org.bukkit.entity.Entity entity : loc.getWorld().getNearbyEntities(loc, 5.0, 5.0, 5.0)) {
                if (!(entity instanceof LivingEntity) || entity.equals(caster)) continue;
                LivingEntity target = (LivingEntity) entity;

                if (CombatManager.canTarget(caster, target)) {
                    // [CHUẨN RPGCORE]: Đẩy qua CombatManager để lấy Xuyên kháng/Crit
                    double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, finalDmgMulti, 0, true, false);
                    CombatManager.applySkillDamage(caster, target, hitDmg);
                }
            }

            caster.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§b§lBÙNG NỔ THUẬT PHÁP §fKích hoạt!"));
        }
    }
}