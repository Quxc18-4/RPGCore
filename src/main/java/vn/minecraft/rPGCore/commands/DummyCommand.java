package vn.minecraft.rPGCore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

public class DummyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!player.hasPermission("rpgcore.admin")) {
            player.sendMessage("§cBạn không có quyền!");
            return true;
        }

        // Triệu hồi hình nhân (Zombie không AI)
        Zombie dummy = (Zombie) player.getWorld().spawnEntity(player.getLocation(), EntityType.ZOMBIE);
        dummy.setCustomName("§e§lDummy");
        dummy.setCustomNameVisible(true);
        dummy.setAI(false); // Tắt AI
        dummy.setSilent(true); // Tắt tiếng kêu
        dummy.setRemoveWhenFarAway(false); // Không bị despawn khi đi xa
        dummy.setBaby(false);

        // ==========================================
        // [ĐÃ FIX]: GIỚI HẠN MÁU XUỐNG 1 TRIỆU HP ĐỂ TRÁNH QUÁ TẢI SERVER
        // ==========================================
        AttributeInstance maxHp = dummy.getAttribute(Attribute.MAX_HEALTH);
        if (maxHp != null) {
            maxHp.setBaseValue(1000000.0); // 1,000,000 HP
        }
        dummy.setHealth(1000000.0); // Bơm đầy 1 triệu máu ngay khi sinh ra

        // Đội nón da để không bị cháy nắng
        dummy.getEquipment().setHelmet(new ItemStack(Material.LEATHER_HELMET));

        player.sendMessage("§a[!] Đã triệu hồi Hình Nhân Thử Nghiệm (Dummy) với 1 triệu Máu!");
        return true;
    }
}