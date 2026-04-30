package vn.minecraft.rPGCore.skills.mage.fusion;

import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.player.RPGPlayer;
import java.util.List;

public interface IFusion {
    MageElement getElement1();
    MageElement getElement2();
    String getReactionName();
    double getDamageMultiplier();

    // [CẬP NHẬT MỚI]: Mỗi Class tự định nghĩa Lore của riêng nó!
    List<String> getLore(double baseDmgPct);

    void cast(Player caster, RPGPlayer rpgPlayer, int skillLevel, double baseMultiplier);
}