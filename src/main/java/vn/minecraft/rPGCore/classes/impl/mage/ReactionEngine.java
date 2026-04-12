package vn.minecraft.rPGCore.classes.impl.mage;

import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import org.bukkit.potion.PotionEffectType;

public class ReactionEngine {

    private static final String MARK_PREFIX = "Elemental_Mark_";
    private static final long MARK_DURATION = 5000L; // 5 giây

    // Lớp chứa kết quả trả về khi 2 nguyên tố chạm nhau
    public static class ReactionResult {
        public double damageMultiplier = 1.0;
        public boolean isReacted = false;
        public String reactionName = "";
    }

    /**
     * Hàm này được gọi khi Pháp sư tung 1 kỹ năng trúng đích.
     * Nó sẽ kiểm tra xem mục tiêu có sẵn Ấn cũ không để tạo phản ứng,
     * nếu KHÔNG có, nó sẽ gắn Ấn MỚI lên mục tiêu.
     */
    public static ReactionResult processHit(Player caster, LivingEntity target, MageElement incomingElement) {
        ReactionResult result = new ReactionResult();

        // Kiểm tra xem mục tiêu đang có Ấn gì trên người
        MageElement existingElement = getExistingMark(target);

        if (existingElement != null && existingElement != incomingElement) {
            // CÓ PHẢN ỨNG NGUYÊN TỐ! Xóa ấn cũ ngay lập tức để tránh lặp
            clearMark(target);
            result.isReacted = true;

            // =====================================
            // BẢNG CÔNG THỨC PHẢN ỨNG (REACTION TABLE)
            // =====================================

            // 1. LỬA + NƯỚC = BỐC HƠI (Vaporize) -> Sát thương tức thời x1.5
            if (isCombo(existingElement, incomingElement, MageElement.FIRE, MageElement.WATER)) {
                result.damageMultiplier = 1.5;
                result.reactionName = "§c§lBỐC HƠI";
                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
            }
            // 2. LỬA + THIÊN NHIÊN = THIÊU RỤI (Burn) -> Đốt cháy mạnh
            else if (isCombo(existingElement, incomingElement, MageElement.FIRE, MageElement.NATURE)) {
                result.damageMultiplier = 1.2;
                result.reactionName = "§6§lTHIÊU RỤI";
                target.setFireTicks(100); // Đốt 5 giây
            }
            // 3. NƯỚC + ÁNH SÁNG = BĂNG GIÁ (Freeze) -> Đóng băng cứng (Stun) 2 giây
            else if (isCombo(existingElement, incomingElement, MageElement.WATER, MageElement.LIGHT)) {
                result.damageMultiplier = 1.1;
                result.reactionName = "§b§lBĂNG GIÁ";
                StatusEffectManager.applyEffect(target, PotionEffectType.SLOWNESS, 40, 10); // Slowness 10 ~ Bất động
                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 2.0f);
            }
            // 4. THIÊN NHIÊN + NƯỚC = TĂNG TRƯỞNG (Overgrowth) -> Trói chân
            else if (isCombo(existingElement, incomingElement, MageElement.NATURE, MageElement.WATER)) {
                result.damageMultiplier = 1.0;
                result.reactionName = "§2§lTĂNG TRƯỞNG";
                StatusEffectManager.applyEffect(target, PotionEffectType.LEVITATION, 20, 250); // Trói chặt xuống đất (Mẹo dùng Potion)
            }

            // Gửi thông báo ActionBar cho người chơi để sướng tay
            if (!result.reactionName.isEmpty()) {
                caster.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§f[Phản Ứng]: " + result.reactionName));
            }

        } else {
            // Nếu chưa có Ấn hoặc trùng Ấn, thì gắn (hoặc làm mới) Ấn hiện tại
            applyMark(target, incomingElement);
        }

        return result;
    }

    // --- CÁC HÀM TIỆN ÍCH QUẢN LÝ ẤN (METADATA) ---

    private static boolean isCombo(MageElement e1, MageElement e2, MageElement req1, MageElement req2) {
        return (e1 == req1 && e2 == req2) || (e1 == req2 && e2 == req1);
    }

    private static void applyMark(LivingEntity target, MageElement element) {
        // Gắn loại Nguyên tố và Thời gian hết hạn
        target.setMetadata(MARK_PREFIX + "Type", new FixedMetadataValue(RPGCore.getInstance(), element.name()));
        target.setMetadata(MARK_PREFIX + "Expire", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + MARK_DURATION));

        // Hiệu ứng hạt vòng quanh chân mục tiêu (Có thể viết Runnable sau)
        target.getWorld().spawnParticle(element.getParticle(), target.getLocation().add(0, 0.5, 0), 10, 0.3, 0.3, 0.3, 0.05);
    }

    private static MageElement getExistingMark(LivingEntity target) {
        if (!target.hasMetadata(MARK_PREFIX + "Type") || !target.hasMetadata(MARK_PREFIX + "Expire")) return null;

        long expireTime = target.getMetadata(MARK_PREFIX + "Expire").get(0).asLong();
        if (System.currentTimeMillis() > expireTime) {
            clearMark(target);
            return null; // Ấn đã hết hạn
        }

        String typeName = target.getMetadata(MARK_PREFIX + "Type").get(0).asString();
        try {
            return MageElement.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void clearMark(LivingEntity target) {
        target.removeMetadata(MARK_PREFIX + "Type", RPGCore.getInstance());
        target.removeMetadata(MARK_PREFIX + "Expire", RPGCore.getInstance());
    }

    // ==========================================
    // CƠ CHẾ MỚI: ĐÁNH THƯỜNG GẮN ẤN (KHÔNG KÍCH NỔ)
    // ==========================================
    public static void applyMarkOnly(LivingEntity target, MageElement element) {
        // Nếu đang trong thời gian miễn nhiễm sau phản ứng (ICD) -> Không cho gắn ấn mới
        if (target.hasMetadata("Reaction_Immunity")) {
            long immunityTime = target.getMetadata("Reaction_Immunity").get(0).asLong();
            if (System.currentTimeMillis() < immunityTime) return;
        }

        // Trực tiếp gắn (hoặc đè) Ấn mới
        applyMark(target, element);
    }

    // Hàm gọi khi 1 Kỹ năng (Skill) kích nổ thành công phản ứng
    public static void triggerReactionImmunity(LivingEntity target) {
        target.setMetadata("Reaction_Immunity", new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), System.currentTimeMillis() + 1500L)); // Miễn nhiễm 1.5s
    }
}