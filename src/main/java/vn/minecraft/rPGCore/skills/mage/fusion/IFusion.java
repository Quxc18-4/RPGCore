package vn.minecraft.rPGCore.skills.mage.fusion;

import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.player.RPGPlayer;

public interface IFusion {
    // 2 Nguyên tố tạo thành (Thứ tự không quan trọng)
    MageElement getElement1();
    MageElement getElement2();

    // Tên phản ứng hiển thị
    String getReactionName();

    // Hệ số nhân Sát thương cơ bản
    double getDamageMultiplier();

    // [CẬP NHẬT MẠNH MẼ]: Trao toàn quyền quyết định cách thức thi triển (Hình thái Kỹ năng)
    // Nó có thể là bắn đạn, gọi thiên thạch, quét hình nón, đặt bẫy... Class con sẽ tự lo toàn bộ!
    void cast(Player caster, RPGPlayer rpgPlayer, int skillLevel, double baseMultiplier);
}