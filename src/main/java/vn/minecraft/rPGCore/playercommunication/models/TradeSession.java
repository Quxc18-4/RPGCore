package vn.minecraft.rPGCore.playercommunication.models;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TradeSession implements InventoryHolder {

    private final UUID player1Id;
    private final UUID player2Id;
    private final Inventory inventory;

    private boolean p1Ready = false;
    private boolean p2Ready = false;
    private BukkitTask countdownTask = null;
    private int countdownSeconds = 3;

    // QUY HOẠCH GIAO DIỆN 54 Ô
    // 12 Ô của P1 (Trái): Hàng 2, 3, 4
    public static final Set<Integer> P1_SLOTS = new HashSet<>(Arrays.asList(
            9, 10, 11, 12,
            18, 19, 20, 21,
            27, 28, 29, 30
    ));
    // 12 Ô của P2 (Phải): Hàng 2, 3, 4
    public static final Set<Integer> P2_SLOTS = new HashSet<>(Arrays.asList(
            14, 15, 16, 17,
            23, 24, 25, 26,
            32, 33, 34, 35
    ));

    // Nút sẵn sàng
    public static final int P1_READY_SLOT = 37;
    public static final int P2_READY_SLOT = 43;

    // Biên lai đếm tiền
    public static final int P1_SUMMARY_SLOT = 22;
    public static final int P2_SUMMARY_SLOT = 23;

    // 3 Ô Đếm ngược ở giữa dưới cùng
    public static final int[] COUNTDOWN_SLOTS = {48, 49, 50};

    public TradeSession(Player p1, Player p2) {
        this.player1Id = p1.getUniqueId();
        this.player2Id = p2.getUniqueId();

        // 2 Người sẽ nhìn chung vào cái Inventory này
        this.inventory = Bukkit.createInventory(this, 54, "§0Giao Dịch: §1" + p1.getName() + " §0- §4" + p2.getName());
        setupUI();
    }

    private void setupUI() {
        ItemStack glass = createUIItem(Material.BLACK_STAINED_GLASS_PANE, "§0");
        ItemStack readyBtn = createUIItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§lCHƯA SẴN SÀNG", "§7Click để Khóa đồ và Xác nhận.");
        ItemStack waitGlass = createUIItem(Material.WHITE_STAINED_GLASS_PANE, "§f§lCHỜ XÁC NHẬN");

        // Đổ nền đen cho các khu vực không khả dụng
        for (int i = 0; i < 54; i++) {
            if (!P1_SLOTS.contains(i) && !P2_SLOTS.contains(i)) {
                inventory.setItem(i, glass);
            }
        }

        // Nút sẵn sàng
        inventory.setItem(P1_READY_SLOT, readyBtn.clone());
        inventory.setItem(P2_READY_SLOT, readyBtn.clone());

        // Hóa đơn (Sẽ được Listener cập nhật liên tục)
        inventory.setItem(P1_SUMMARY_SLOT, createUIItem(Material.PAPER, "§a§lBiên Lai Của Trái", "§fTổng tiền: §e0 Đồng"));
        inventory.setItem(P2_SUMMARY_SLOT, createUIItem(Material.PAPER, "§a§lBiên Lai Của Phải", "§fTổng tiền: §e0 Đồng"));

        // 3 Ô đếm ngược
        for (int slot : COUNTDOWN_SLOTS) {
            inventory.setItem(slot, waitGlass.clone());
        }
    }

    private ItemStack createUIItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    // Các Getter/Setter cần thiết
    @Override
    public Inventory getInventory() { return inventory; }
    public UUID getPlayer1Id() { return player1Id; }
    public UUID getPlayer2Id() { return player2Id; }

    public boolean isP1Ready() { return p1Ready; }
    public void setP1Ready(boolean p1Ready) { this.p1Ready = p1Ready; }

    public boolean isP2Ready() { return p2Ready; }
    public void setP2Ready(boolean p2Ready) { this.p2Ready = p2Ready; }

    public BukkitTask getCountdownTask() { return countdownTask; }
    public void setCountdownTask(BukkitTask countdownTask) { this.countdownTask = countdownTask; }

    public int getCountdownSeconds() { return countdownSeconds; }
    public void setCountdownSeconds(int countdownSeconds) { this.countdownSeconds = countdownSeconds; }
}