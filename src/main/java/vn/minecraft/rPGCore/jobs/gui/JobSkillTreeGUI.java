package vn.minecraft.rPGCore.jobs.gui;

public class JobSkillTreeGUI {
    public static void openSkillTree(Player player) {
        PlayerJobData data = JobManager.getPlayerData(player);

        if (data.getLevel() < 50) {
            player.sendMessage("§c§l(!) §cKỹ năng tối thượng chỉ mở khóa khi đạt Cấp 50!");
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§l§dCÂY KỸ NĂNG TỐI THƯỢNG");
        // Triển khai các nút kỹ năng như:
        // - "Bàn tay của Chúa": +1% tỉ lệ đập đồ vĩnh viễn
        // - "Bậc thầy tiết kiệm": Giảm 50% chi phí Mạt Thợ Rèn khi rèn bậc thầy

        player.openInventory(inv);
    }
}
