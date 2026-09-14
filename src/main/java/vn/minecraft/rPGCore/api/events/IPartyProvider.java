package vn.minecraft.rPGCore.api.events;

import org.bukkit.entity.Player;
import java.util.List;

public interface IPartyProvider {
    /**
     * Lấy danh sách thành viên Party của người chơi.
     */
    List<Player> getPartyMembers(Player player);
}
