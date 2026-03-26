package vn.minecraft.rPGCore.skills.archer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SniperPassiveSkill implements ISkill {

    private final JavaPlugin plugin;

    public SniperPassiveSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "archer_sniper_passive"; }
    @Override public String getName() { return "Nội tại: Ngắm Chuẩn"; }
    @Override public String getRequiredClass() { return "archer"; }
    @Override public Material getIcon() { return Material.SPYGLASS; }
    @Override public int getMaxLevel() { return 5; } // Max level 5
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override public double getManaCost(int level) { return 0; }
    @Override public long getCooldown(int level) { return 0; }

    @Override
    public List<String> getDescription(int level) {
        return Arrays.asList(
                "§7Kỹ năng bị động của Cung Thủ.",
                "§7Càng giữ khoảng cách với mục tiêu,",
                "§7sát thương mũi tên càng khủng khiếp.",
                "",
                "§f- Mỗi 1 block khoảng cách: §c+" + level + "% ST",
                "§8  (Tối đa cộng dồn ở khoảng cách 25 block)"
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        player.sendMessage("§c(!) Đây là kỹ năng bị động, tự động kích hoạt khi bắn tên!");
        return false;
    }
}