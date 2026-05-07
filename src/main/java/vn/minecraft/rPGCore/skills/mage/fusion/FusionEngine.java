package vn.minecraft.rPGCore.skills.mage.fusion;

import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.skills.mage.fusion.impl.DefaultFusion;
import vn.minecraft.rPGCore.skills.mage.fusion.impl.*;

import java.util.HashMap;
import java.util.Map;

public class FusionEngine {

    private static final Map<String, IFusion> FUSION_REGISTRY = new HashMap<>();
    private static final IFusion DEFAULT_FUSION = new DefaultFusion();

    private static String getFusionKey(MageElement e1, MageElement e2) {
        if (e1 == null || e2 == null) return "DEFAULT";

        // [FIX BUG]: Ép toàn bộ tên Enum về Uppercase để không bao giờ bị sai định dạng
        String name1 = e1.name().toUpperCase();
        String name2 = e2.name().toUpperCase();

        if (name1.compareTo(name2) > 0) {
            return name2 + "_" + name1;
        }
        return name1 + "_" + name2;
    }

    public static void registerFusion(IFusion fusion) {
        String key = getFusionKey(fusion.getElement1(), fusion.getElement2());
        FUSION_REGISTRY.put(key, fusion);

        // Dòng Debug này sẽ báo lên Console xem nó đã lưu Key gì. Bạn có thể xóa đi sau khi test xong.
        vn.minecraft.rPGCore.RPGCore.getInstance().getLogger().info("[FusionEngine] Đã đăng ký phản ứng: " + key + " -> " + fusion.getReactionName());
    }

    public static void init() {
        FUSION_REGISTRY.clear();
        registerFusion(new PyroclasmFusion()); // Đăng ký Lửa x Lửa
        registerFusion(new GeyserFusion()); // Nước + Nước
        registerFusion(new ParasiticMinefieldFusion()); // Thiên Nhiên + Thiên Nhiên
        registerFusion(new CycloneFusion()); // Gió + Gió
        registerFusion(new HolyBladesFusion()); // Ánh sáng + Ánh sáng
        registerFusion(new AbyssalRiftFusion()); // Bóng tối + Bóng tối
        registerFusion(new SteamBurstFusion()); // Lửa + Nước
    }

    public static IFusion getActiveFusion(MageElement tnt, MageElement nth) {
        if (nth == null) return DEFAULT_FUSION;

        MageElement effectiveTnt = (tnt == null) ? nth : tnt;
        String key = getFusionKey(effectiveTnt, nth);

        return FUSION_REGISTRY.getOrDefault(key, DEFAULT_FUSION);
    }
}