package vn.minecraft.rPGCore.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.managers.ArrowRecipeManager;

import java.util.Arrays;

public class ArrowCommand implements CommandExecutor {

    private final ArrowRecipeManager recipeManager;

    public ArrowCommand(ArrowRecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!player.hasPermission("rpgcore.admin")) {
            player.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
            return true;
        }

        // =====================================
        // CHỨC NĂNG 1: MỞ RƯƠNG CHỨA MŨI TÊN GỐC
        // =====================================
        if (args.length == 1 && args[0].equalsIgnoreCase("chest")) {
            openArrowChest(player);
            player.sendMessage("§a§l[!] §aĐã mở Kho Mũi Tên Đặc Biệt!");
            return true;
        }

        // =====================================
        // CHỨC NĂNG 2: LẤY MŨI TÊN TỪ CÔNG THỨC
        // =====================================
        if (args.length == 1 && args[0].equalsIgnoreCase("giveall")) {
            if (recipeManager.getRecipes().isEmpty()) {
                player.sendMessage("§c(!) Hiện chưa có công thức mũi tên nào được cấu hình trong hệ thống!");
                return true;
            }

            for (ArrowRecipeManager.RecipeData data : recipeManager.getRecipes().values()) {
                ItemStack arrow = data.result.clone();
                arrow.setAmount(64);
                player.getInventory().addItem(arrow);
            }

            player.sendMessage("§a§l[!] §aĐã cấp phát tất cả " + recipeManager.getRecipes().size() + " loại mũi tên từ công thức!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            return true;
        }

        player.sendMessage("§e§l--- HƯỚNG DẪN LỆNH MŨI TÊN ---");
        player.sendMessage("§b/arrow chest §7- Mở rương lấy 8 mũi tên gốc để setup.");
        player.sendMessage("§b/arrow giveall §7- Lấy mũi tên đã cấu hình trong bảng chế tạo.");
        return true;
    }

    // =====================================
    // HÀM TẠO RƯƠNG VÀ NHÉT ĐỒ VÀO
    // =====================================
    private void openArrowChest(Player player) {
        // Tạo một cái rương ảo 27 ô
        Inventory inv = Bukkit.createInventory(null, 27, "§8§lKHO MŨI TÊN GỐC");

        // Tự động tạo và nhét 8 loại mũi tên vào rương
        inv.addItem(createCustomArrow("§b§lMũi Tên Băng Giá", "§7Đóng băng và làm chậm mục tiêu", "§8arrow_ice"));
        inv.addItem(createCustomArrow("§c§lMũi Tên Lửa", "§7Thiêu đốt kẻ địch", "§8arrow_fire"));
        inv.addItem(createCustomArrow("§e§lMũi Tên Sấm Sét", "§7Giáng sét và làm tê liệt", "§8arrow_lightning"));
        inv.addItem(createCustomArrow("§a§lMũi Tên Thiên Nhiên", "§7Trói chân diện rộng", "§8arrow_nature"));
        inv.addItem(createCustomArrow("§6§lMũi Tên Nổ", "§7Gây sát thương nổ văng đích", "§8arrow_explosive"));
        inv.addItem(createCustomArrow("§4§lMũi Tên Huyết Dạ", "§7Hút 15% máu dựa trên sát thương", "§8arrow_lifesteal"));
        inv.addItem(createCustomArrow("§d§lMũi Tên Lỗ Đen", "§7Tạo hố đen hút quái vật", "§8arrow_blackhole"));
        inv.addItem(createCustomArrow("§2§lMũi Tên Độc", "§7Gây sát thương độc kéo dài", "§8arrow_poison"));

        player.openInventory(inv);
    }

    // =====================================
    // HÀM ĐÚC MŨI TÊN (CÓ CHỨA ID ẨN)
    // =====================================
    private ItemStack createCustomArrow(String name, String desc, String hiddenId) {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW, 64);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            // Dòng cuối cùng là Hidden ID cực kỳ quan trọng để Listener nhận diện
            meta.setLore(Arrays.asList(desc, "", hiddenId));
            item.setItemMeta(meta);
        }
        return item;
    }
}