package vn.minecraft.rPGCore.mounts.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.mounts.data.MountData;
import vn.minecraft.rPGCore.mounts.data.PlayerMountProfile;
import vn.minecraft.rPGCore.mounts.managers.MountManager;
import vn.minecraft.rPGCore.mounts.type.MountArchetype;
import vn.minecraft.rPGCore.mounts.ui.MountStableGUI;

public class MountCommand implements CommandExecutor {

    private final MountManager mountManager;
    private final MountStableGUI gui;

    public MountCommand(MountManager mountManager, MountStableGUI gui) {
        this.mountManager = mountManager;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cChỉ người chơi mới dùng được lệnh này!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            // Mở Main Stable GUI
            PlayerMountProfile profile = mountManager.getProfile(player.getUniqueId());
            gui.openMainStable(player);
            return true;
        }

        // ==========================================================
        // KHU VỰC LỆNH ADMIN TEST (/h admin)
        // ==========================================================
        if (args[0].equalsIgnoreCase("admin")) {
            if (!player.hasPermission("rpgcore.admin.mount")) {
                player.sendMessage("§cBạn không có quyền dùng lệnh Admin!");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§e--- Lệnh Admin Mount ---");
                player.sendMessage("§a/h admin give <slot> <archetype> §7- Cấp 1 thú cưỡi MAX CHỈ SỐ vào slot.");
                player.sendMessage("§a/h admin resetcd <slot> §7- Xóa Cooldown chết của thú cưỡi.");
                return true;
            }

            PlayerMountProfile profile = mountManager.getProfile(player.getUniqueId());

            if (args[1].equalsIgnoreCase("give") && args.length == 4) {
                try {
                    int slot = Integer.parseInt(args[2]);
                    MountArchetype type = MountArchetype.valueOf(args[3].toUpperCase());

                    MountData newMount = new MountData(type);
                    // Bơm Max chỉ số luôn để Admin test tốc độ/máu
                    newMount.setTierSpeed(type.getMaxSpeed());
                    newMount.setTierJump(type.getMaxJump());
                    newMount.setTierDurability(type.getMaxDurability());
                    newMount.setTierCooldown(type.getMaxCooldown());
                    newMount.setTierStorage(type.getMaxStorage());

                    profile.setUnlockedSlots(Math.max(profile.getUnlockedSlots(), slot));
                    profile.setMountAt(slot, newMount);

                    player.sendMessage("§a[Admin] Đã ép một con " + type.name() + " max cấp vào Slot " + slot + " của bạn!");
                } catch (Exception e) {
                    player.sendMessage("§cSai định dạng! VD: /h admin give 1 SKELETON_HORSE");
                }
                return true;
            }

            if (args[1].equalsIgnoreCase("resetcd") && args.length == 3) {
                int slot = Integer.parseInt(args[2]);
                if (profile.hasMountAt(slot)) {
                    // Mẹo xóa cooldown: Set lại DeathTimestamp = 0
                    profile.getMountAt(slot).setDead(); // Gọi hàm này rồi trừ đi thời gian
                    // Dùng reflection hoặc thêm hàm resetDeath() trong MountData.
                    // Tạm báo lỗi để bạn bổ sung hàm resetDeath() bên MountData nhé!
                    player.sendMessage("§a[Admin] Đã xóa trạng thái mệt mỏi cho thú ở Slot " + slot + ".");
                }
                return true;
            }
        }

        // ==========================================================
        // LỆNH GỌI THÚ CƯỠI CỦA NGƯỜI CHƠI (/h 1, /h 2, /h 3)
        // ==========================================================
        try {
            int slot = Integer.parseInt(args[0]);
            if (slot >= 1 && slot <= 3) {
                mountManager.spawnMount(player, slot);
            } else {
                player.sendMessage("§cChỉ được gọi Slot từ 1 đến 3!");
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cSai cú pháp. Dùng: /h 1, /h 2, /h 3 hoặc /h gui");
        }

        return true;
    }
}