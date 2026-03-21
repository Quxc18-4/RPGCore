package vn.minecraft.rPGCore.skills;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.List;
import java.util.Map;

public interface ISkill {
    String getId();               // VD: "archer_spread"
    String getName();             // VD: "Mưa Tên Kép"
    String getRequiredClass();    // VD: "archer"
    Material getIcon();           // Icon hiển thị trên Menu

    int getMaxLevel();
    double getManaCost(int level);
    long getCooldown(int level);  // Trả về Milliseconds

    // Tự động sinh mô tả dựa trên cấp độ hiện tại
    List<String> getDescription(int level);

    // Hàm thực thi chính khi bấm đúng Combo
    boolean execute(Player player, RPGPlayer rpgPlayer, int level);
    // Thêm hàm này để mỗi kỹ năng tự quyết định giá SP khi nâng cấp
    int getUpgradeCost(int currentLevel);
    // Trả về danh sách Kỹ năng yêu cầu. Key = ID kỹ năng, Value = Level tối thiểu
    Map<String, Integer> getRequiredSkills();
    // Thêm hàm để quy định cấp độ tối thiểu của người chơi (Mặc định là 1 nếu không cài đặt)
    default int getRequiredPlayerLevel() { return 1; }
}