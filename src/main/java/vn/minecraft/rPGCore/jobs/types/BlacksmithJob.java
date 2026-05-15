package vn.minecraft.rPGCore.jobs.types;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.jobs.core.RPGJob;
import vn.minecraft.rPGCore.jobs.quests.AdvancedQuest;
import vn.minecraft.rPGCore.jobs.quests.QuestAction;
import vn.minecraft.rPGCore.jobs.quests.RPGQuest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlacksmithJob extends RPGJob {

    // Kho chứa nhiệm vụ của Thợ Rèn
    private final Map<String, RPGQuest> questPool = new HashMap<>();

    public BlacksmithJob() {
        super("BLACKSMITH", "Thợ Rèn");
        registerQuests(); // Đăng ký nhiệm vụ ngay khi khởi tạo
    }

    private void registerQuests() {
        // =========================================
        // KHO NHIỆM VỤ NGÀY (DAILY POOL)
        // =========================================
        questPool.put("DAILY_FORGE_1", new AdvancedQuest("DAILY_FORGE_1", "Khởi Động Lò Rèn", "Thực hiện rèn trang bị 15 lần.", 15, QuestAction.FORGE_ANY, null));

        questPool.put("DAILY_FORGE_2", new AdvancedQuest("DAILY_FORGE_2", "Bàn Tay Ma Thuật", "Đập đồ thành công 5 lần.", 5, QuestAction.FORGE_SUCCESS, null));

        questPool.put("DAILY_FORGE_3", new AdvancedQuest("DAILY_FORGE_3", "Kiên Trì Nhẫn Nại", "Đập đồ thất bại 10 lần.", 10, QuestAction.FORGE_FAIL, null));

        questPool.put("DAILY_FORGE_4", new AdvancedQuest("DAILY_FORGE_4", "Đại Gia Nguyên Liệu", "Tiêu hao 20 Đá Nâng Cấp.", 20, QuestAction.CONSUME_UPGRADE_STONE, null));

        questPool.put("DAILY_FORGE_5", new AdvancedQuest("DAILY_FORGE_5", "Thần Tài Gõ Cửa", "Sử dụng tính năng Rèn Bậc Thầy 1 lần.", 1, QuestAction.USE_MASTER_FORGE, null));

        questPool.put("DAILY_FORGE_6", new AdvancedQuest("DAILY_FORGE_6", "Cảm Giác Thăng Hoa", "Đạt chuỗi đập đồ thành công liên tiếp 3 lần.", 1, QuestAction.FORGE_STREAK, args -> {
            if (args.length > 0 && args[0] instanceof Integer) return (Integer) args[0] >= 3;
            return false;
        }));

        // =========================================
        // KHO NHIỆM VỤ TUẦN (WEEKLY POOL)
        // =========================================
        questPool.put("WEEKLY_MASTER_1", new AdvancedQuest("WEEKLY_MASTER_1", "Đột Phá Giới Hạn", "Nâng cấp thành công 3 trang bị đang ở mức +9 trở lên.", 3, QuestAction.FORGE_SUCCESS, args -> {
            if (args.length > 0 && args[0] instanceof Integer) return (Integer) args[0] >= 9;
            return false;
        }));

        questPool.put("WEEKLY_MASTER_2", new AdvancedQuest("WEEKLY_MASTER_2", "Huyền Thoại Rèn Đúc", "Sử dụng tính năng Rèn Bậc Thầy 8 lần.", 8, QuestAction.USE_MASTER_FORGE, null));

        questPool.put("WEEKLY_MASTER_3", new AdvancedQuest("WEEKLY_MASTER_3", "Tiêu Hao Khổng Lồ", "Tiêu hao 300 Đá Nâng Cấp.", 300, QuestAction.CONSUME_UPGRADE_STONE, null));
    }

    @Override
    public Map<String, RPGQuest> getQuests() {
        return questPool;
    }

    @Override
    public ItemStack getIcon(int currentLevel) {
        ItemStack icon = new ItemStack(Material.ANVIL);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§lNGHỀ THỢ RÈN");
            meta.setLore(getDescription(currentLevel));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    @Override
    public List<String> getDescription(int currentLevel) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Bậc thầy về thao tác kim loại và nhiệt độ.");
        lore.add("");
        lore.add("§fCấp độ hiện tại: §b" + currentLevel + "§8/§b" + maxLevel);
        lore.add("");
        lore.add("§a§lNỘI TẠI NGHỀ:");
        lore.add("§8▪ §fTỉ lệ thành công khi rèn: §a+" + String.format("%.1f", getForgeSuccessBonus(currentLevel)) + "%");
        lore.add("§8▪ §fTỉ lệ nhận thêm nâng cấp Dòng dõi: §6" + String.format("%.1f", (50.0 + getLineageStarBonus(currentLevel) * 100)) + "%");
        lore.add("§8▪ §fTỉ lệ hoàn trả Mạt Thợ Rèn khi thất bại: §c" + String.format("%.1f", getScrapRefundChance(currentLevel)) + "%");
        lore.add("§8▪ §fXác suất không tiêu hao Đá khi rèn trang bị: §b" + String.format("%.1f", getStoneSaveChance(currentLevel)) + "%");
        return lore;
    }

    @Override
    public double getForgeSuccessBonus(int level) { return level * 0.2; }

    @Override
    public double getLineageStarBonus(int level) { return (level * 0.6) / 100.0; }

    @Override
    public double getScrapRefundChance(int level) { return level * 0.6; }

    @Override
    public double getStoneSaveChance(int level) { return level * 0.5; }
}