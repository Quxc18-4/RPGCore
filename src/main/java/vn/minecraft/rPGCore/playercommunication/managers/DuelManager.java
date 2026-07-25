package vn.minecraft.rPGCore.playercommunication.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.playercommunication.models.Arena;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelManager {
    private final RPGCore plugin;

    private final Map<UUID, UUID> duelRequests = new HashMap<>();
    private final Map<UUID, Location> oldLocations = new HashMap<>(); // Lưu tọa độ cũ để trả về
    private final Map<UUID, Arena> activeDuels = new HashMap<>();     // Trạng thái đang bận
    private final Map<UUID, UUID> duelOpponents = new HashMap<>();    // Biết ai đang đánh với ai

    public DuelManager(RPGCore plugin) {
        this.plugin = plugin;
    }

    public void sendDuelRequest(Player sender, Player receiver) {
        if (activeDuels.containsKey(sender.getUniqueId()) || activeDuels.containsKey(receiver.getUniqueId())) {
            sender.sendMessage("§cMột trong hai người đang ở trong một trận đấu khác!");
            return;
        }

        duelRequests.put(receiver.getUniqueId(), sender.getUniqueId());
        sender.sendMessage("§aĐã gửi lời mời thách đấu tới §e" + receiver.getName());
        receiver.sendMessage("§c§l[Thách Đấu] §b" + sender.getName() + " §fmuốn giao lưu võ thuật với bạn!");
        receiver.sendMessage("§7Gõ §a/duel accept §7để đồng ý.");

        Bukkit.getScheduler().runTaskLater(plugin, () -> duelRequests.remove(receiver.getUniqueId(), sender.getUniqueId()), 20L * 30);
    }

    public void acceptDuel(Player receiver) {
        UUID senderId = duelRequests.remove(receiver.getUniqueId());
        if (senderId == null) {
            receiver.sendMessage("§cBạn không có lời mời thách đấu nào!");
            return;
        }

        Player sender = Bukkit.getPlayer(senderId);
        if (sender == null || !sender.isOnline()) {
            receiver.sendMessage("§cĐối thủ đã Offline!");
            return;
        }

        Arena freeArena = plugin.getArenaManager().getFreeArena();
        if (freeArena == null) {
            sender.sendMessage("§cHiện tại không có map nào rảnh. Vui lòng thử lại sau!");
            receiver.sendMessage("§cHiện tại không có map nào rảnh. Vui lòng thử lại sau!");
            return;
        }

        startDuel(sender, receiver, freeArena);
    }

    private void startDuel(Player p1, Player p2, Arena arena) {
        arena.setOccupied(true);

        oldLocations.put(p1.getUniqueId(), p1.getLocation());
        oldLocations.put(p2.getUniqueId(), p2.getLocation());
        duelOpponents.put(p1.getUniqueId(), p2.getUniqueId());
        duelOpponents.put(p2.getUniqueId(), p1.getUniqueId());
        activeDuels.put(p1.getUniqueId(), arena);
        activeDuels.put(p2.getUniqueId(), arena);

        // Gắn cờ Metadata để Listener xử lý sát thương
        p1.setMetadata("IN_DUEL", new FixedMetadataValue(plugin, true));
        p2.setMetadata("IN_DUEL", new FixedMetadataValue(plugin, true));

        // Hồi máu, thanh tẩy hiệu ứng trước khi đánh
        preparePlayerForDuel(p1);
        preparePlayerForDuel(p2);

        p1.teleport(arena.getSpawn1());
        p2.teleport(arena.getSpawn2());

        p1.sendMessage("§e§lTRẬN ĐẤU BẮT ĐẦU! §fBạn đang đối đầu với §c" + p2.getName());
        p2.sendMessage("§e§lTRẬN ĐẤU BẮT ĐẦU! §fBạn đang đối đầu với §c" + p1.getName());
    }

    public void endDuel(Player loser, Player winner, boolean disconnected) {
        Arena arena = activeDuels.remove(loser.getUniqueId());
        if (arena != null) arena.setOccupied(false);

        activeDuels.remove(winner.getUniqueId());
        duelOpponents.remove(loser.getUniqueId());
        duelOpponents.remove(winner.getUniqueId());

        if (!disconnected) {
            loser.sendMessage("§c§lBẠN ĐÃ THUA! §f" + winner.getName() + " đã đánh bại bạn.");
            winner.sendMessage("§a§lCHIẾN THẮNG! §fBạn đã hạ gục " + loser.getName() + ".");
        } else {
            winner.sendMessage("§a§lCHIẾN THẮNG! §fĐối thủ đã bỏ chạy (Thoát game).");
        }

        restorePlayerAfterDuel(winner);
        restorePlayerAfterDuel(loser); // Xử lý khôi phục trạng thái cho kẻ thua
    }

    public void preparePlayerForDuel(Player p) {
        if (p == null || !p.isOnline()) return;
        StatusEffectManager.cleanseNegativeEffects(p);
        p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
        p.setFoodLevel(20);
        p.setFireTicks(0);

        // Hồi full Năng Lượng/Nộ Khí chuẩn theo RPGPlayer
        vn.minecraft.rPGCore.player.RPGPlayer rpgP = plugin.getPlayerManager().getRPGPlayer(p.getUniqueId());
        if (rpgP != null) {
            // Sử dụng hàm tổng quát Resource thay vì Mana
            rpgP.setCurrentResource(rpgP.getTotalMaxResource());
        }
    }

    public void restorePlayerAfterDuel(Player p) {
        if (p == null || !p.isOnline()) return;

        p.removeMetadata("IN_DUEL", plugin);
        preparePlayerForDuel(p); // Thanh tẩy hiệu ứng và hồi máu 1 lần nữa sau trận

        Location oldLoc = oldLocations.remove(p.getUniqueId());
        if (oldLoc != null) p.teleport(oldLoc);
    }

    public UUID getOpponent(UUID playerId) { return duelOpponents.get(playerId); }
    public boolean isInDuel(Player p) { return activeDuels.containsKey(p.getUniqueId()); }
}