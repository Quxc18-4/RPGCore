package vn.minecraft.rPGCore.skills.mage.fusion;

import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.skills.mage.fusion.impl.DefaultFusion;

import java.util.HashMap;
import java.util.Map;

public class FusionEngine {

    private static final Map<String, IFusion> FUSION_REGISTRY = new HashMap<>();
    private static final IFusion DEFAULT_FUSION = new DefaultFusion(); // Trạng thái Không có gì

    // ==========================================
    // THUẬT TOÁN ĐẢM BẢO TÍNH GIAO HOÁN (A+B = B+A)
    // ==========================================
    private static String getFusionKey(MageElement e1, MageElement e2) {
        if (e1 == null || e2 == null) return "DEFAULT";
        // Luôn sắp xếp theo thứ tự Alphabet để tạo Key chuẩn xác
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
        registerFusion(new vn.minecraft.rPGCore.skills.mage.fusion.impl.PyroclasmFusion());
    }

    // ==========================================
    // TRẢ VỀ CLASS PHẢN ỨNG CHUẨN XÁC
    // ==========================================
    public static IFusion getActiveFusion(MageElement tnt, MageElement nth) {
        if (tnt == null || nth == null) return DEFAULT_FUSION;

        String key = getFusionKey(tnt, nth);
        return FUSION_REGISTRY.getOrDefault(key, DEFAULT_FUSION);
    }
}