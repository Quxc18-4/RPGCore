package vn.minecraft.rPGCore.skills.mage.fusion;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.player.RPGPlayer;

public interface IFusion {
    // 2 Nguyên tố tạo thành (Không phân biệt thứ tự)
    MageElement getElement1();
    MageElement getElement2();

    // Tên phản ứng hiển thị trên màn hình
    String getReactionName();

    // Hệ số nhân Sát thương của riêng phản ứng này
    double getDamageMultiplier();

    // [CẬP NHẬT MỚI]: Nơi chứa Logic riêng biệt (Hitbox, VFX, Damage Calculation)
    // Class triển khai sẽ tự động lo việc gọi CombatManager để tính sát thương
    void executeVisualAndDamage(Player caster, RPGPlayer rpgPlayer, Location hitLoc, Vector direction, double baseMultiplier);
}