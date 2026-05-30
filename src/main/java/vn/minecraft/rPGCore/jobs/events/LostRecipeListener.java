package vn.minecraft.rPGCore.jobs.events;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.brewing.BrewingEngine;
import vn.minecraft.rPGCore.jobs.core.JobManager;
import vn.minecraft.rPGCore.jobs.core.PlayerJobData;
import vn.minecraft.rPGCore.managers.ItemManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LostRecipeListener implements Listener {

    private final ItemManager itemManager;

    public LostRecipeListener() {
        this.itemManager = RPGCore.getInstance().getItemManager();
    }

    @EventHandler
    public void onReadBook(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() == org.bukkit.Material.AIR) return;

        if ("alch_lost_recipe".equals(itemManager.getItemId(item))) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);

            if (data == null || !data.getJobId().equalsIgnoreCase("ALCHEMIST")) {
                player.sendMessage("§c§l[!] §cChỉ Dược Sư mới có thể đọc hiểu văn tự cổ đại này!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // Tìm các công thức người chơi CHƯA HỌC
            List<vn.minecraft.rPGCore.brewing.BrewingEngine.SecretRecipe> unlearned = new ArrayList<>();
            for (vn.minecraft.rPGCore.brewing.BrewingEngine.SecretRecipe recipe : vn.minecraft.rPGCore.brewing.BrewingEngine.SECRET_RECIPES) {
                if (!data.hasUnlockedSkill("ALCH_RECIPE", recipe.recipeId)) {
                    unlearned.add(recipe);
                }
            }

            if (unlearned.isEmpty()) {
                player.sendMessage("§c§l[!] §cBạn đã học được mọi bí thuật thất truyền trên thế gian rồi!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // Quay ngẫu nhiên 1 công thức
            vn.minecraft.rPGCore.brewing.BrewingEngine.SecretRecipe learnedRecipe = unlearned.get(new Random().nextInt(unlearned.size()));

            // Mở khóa công thức
            data.unlockSkill("ALCH_RECIPE", learnedRecipe.recipeId);

            // Trừ sách
            item.setAmount(item.getAmount() - 1);

            // ========================================================
            // TẠO LIST MỚI ĐỂ DỊCH TỪ "ID" SANG "TÊN VẬT PHẨM"
            // ========================================================
            List<String> ingredientNames = new ArrayList<>();
            for (String reqId : learnedRecipe.requiredItems) {
                vn.minecraft.rPGCore.managers.ItemManager.ItemData reqData = itemManager.getItemData(reqId);

                if (reqData != null) {
                    // Lấy tên vật phẩm (Ví dụ: "Cỏ Máu").
                    // Lưu ý: Nếu field tên trong class ItemData của bạn bị private, hãy đổi reqData.name thành reqData.getName()
                    ingredientNames.add("§e" + reqData.name + "§7");
                } else {
                    // Nếu lỡ cấu hình sai ID, hệ thống sẽ in thẳng ID ra để bạn dễ debug
                    ingredientNames.add("§c" + reqId + "§7");
                }
            }

            // Bắn pháo bông & Hiệu ứng
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            player.sendMessage("");
            player.sendMessage("§d§lKIẾN THỨC BÙNG NỔ! §fBạn vừa đọc hiểu cuốn sách cổ.");
            player.sendMessage("§fMở khóa thành công công thức luyện: " + learnedRecipe.potionName);

            // In ra danh sách tên đã được biên dịch thay vì ID
            player.sendMessage("§8▪ Nguyên liệu yêu cầu: §7" + String.join(", ", ingredientNames));
            player.sendMessage("");
        }
    }
}