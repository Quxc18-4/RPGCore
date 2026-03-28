package vn.minecraft.rPGCore.skills.archer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ArrowCraftingSkill implements ISkill {

    private final JavaPlugin plugin;

    public ArrowCraftingSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "archer_arrow_crafting"; }
    @Override public String getName() { return "Chế Tác Tiễn"; }
    @Override public String getRequiredClass() { return "archer"; }
    @Override public Material getIcon() { return Material.FLETCHING_TABLE; }
    @Override public int getMaxLevel() { return 1; } // Kỹ năng bị động, max lv 1
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public boolean isPassiveSkill() { return true; }

    @Override public int getRequiredPlayerLevel() { return 16; } // Yêu cầu lv 16

    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }
    @Override public double getManaCost(int level) { return 0; }
    @Override public long getCooldown(int level) { return 0; }

    @Override
    public List<String> getDescription(int level) {
        return List.of(
                "§7Kỹ năng bị động của Cung Thủ.",
                "§7Mở khóa khả năng sử dụng Bàn Chế Tạo Mũi Tên",
                "§7(Fletching Table) để chế tác các loại mũi tên",
                "§7đặc biệt mang sức mạnh nguyên tố.",
                "",
                "§e§lCách dùng:",
                "§7- Click chuột phải vào Fletching Table.",
                "§7- Cầm mũi tên đặc biệt ở §bTay Trái (Off-hand)",
                "§7  khi bắn để kích hoạt 100% hiệu ứng."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        // Kỹ năng bị động không thể chủ động cast bằng Combo
        player.sendMessage("§c(!) Đây là kỹ năng bị động. Hãy click chuột phải vào Bàn Chế Tạo Mũi Tên (Fletching Table) để sử dụng.");
        return false;
    }
}