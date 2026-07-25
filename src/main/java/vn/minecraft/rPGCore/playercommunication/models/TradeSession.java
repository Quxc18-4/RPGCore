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

    // ==========================================
    // TỌA ĐỘ MỚI (CHUẨN THEO ẢNH 2 BÁC GỬI)
    // ==========================================
    public static final Set<Integer> P1_SLOTS = new HashSet<>(Arrays.asList(
            9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30, 36, 37, 38, 39
    ));
    public static final Set<Integer> P2_SLOTS = new HashSet<>(Arrays.asList(
            14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35, 41, 42, 43, 44
    ));

    public static final int P1_READY_SLOT = 47;
    public static final int P2_READY_SLOT = 51;
    public static final int SUMMARY_SLOT = 49;

    public static final int[] COUNTDOWN_SLOTS = {13, 22, 31, 40};

    private final UUID player1Id;
    private final UUID player2Id;
    private final Inventory inventory;

    private boolean p1Ready = false;
    private boolean p2Ready = false;

    // [QUAN TRỌNG]: Cờ báo hiệu giao dịch đã hoàn tất để chặn Dup Log
    private boolean completed = false;

    private BukkitTask countdownTask = null;
    private int countdownSeconds = 4; // Set 4 giây để chạy 4 màu Trắng -> Đỏ -> Vàng -> Xanh
    private final String p1Name;
    private final String p2Name;

    public TradeSession(Player p1, Player p2) {
        this.player1Id = p1.getUniqueId();
        this.player2Id = p2.getUniqueId();
        this.p1Name = p1.getName();
        this.p2Name = p2.getName();

        this.inventory = Bukkit.createInventory(this, 54, "§0Giao Dịch: §1" + p1Name + " §0- §4" + p2Name);
        setupUI();
    }

    private void setupUI() {
        ItemStack blackGlass = createUIItem(Material.BLACK_STAINED_GLASS_PANE, "§0");
        ItemStack greenGlass = createUIItem(Material.LIME_STAINED_GLASS_PANE, "§a§lVách Ngăn");
        // Khởi tạo nút trạng thái ban đầu là ĐỎ (Chưa sẵn sàng)
        ItemStack notReady = createUIItem(Material.RED_STAINED_GLASS_PANE, "§c§lCHƯA SẴN SÀNG", "§7Click để Khóa đồ và Xác nhận.");

        for (int i = 0; i < 54; i++) {
            if (!P1_SLOTS.contains(i) && !P2_SLOTS.contains(i)) {
                inventory.setItem(i, blackGlass);
            }
        }

        for (int slot : COUNTDOWN_SLOTS) {
            inventory.setItem(slot, greenGlass);
        }

        inventory.setItem(P1_READY_SLOT, notReady);
        inventory.setItem(P2_READY_SLOT, notReady);

        ItemStack summary = new ItemStack(Material.PAPER);
        ItemMeta meta = summary.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a§lHÓA ĐƠN GIAO DỊCH");
            meta.setLore(Arrays.asList(
                    "§e<========",
                    "§f▶ §b" + p1Name + "§f: §a0 Đồng",
                    "§f▶ §c" + p2Name + "§f: §a0 Đồng",
                    "§e=========>"
            ));
            summary.setItemMeta(meta);
        }
        inventory.setItem(SUMMARY_SLOT, summary);
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

    @Override public Inventory getInventory() { return inventory; }
    public UUID getPlayer1Id() { return player1Id; }
    public UUID getPlayer2Id() { return player2Id; }
    public String getPlayer1Name() { return p1Name; }
    public String getPlayer2Name() { return p2Name; }
    public boolean isP1Ready() { return p1Ready; }
    public void setP1Ready(boolean p1Ready) { this.p1Ready = p1Ready; }
    public boolean isP2Ready() { return p2Ready; }
    public void setP2Ready(boolean p2Ready) { this.p2Ready = p2Ready; }
    public BukkitTask getCountdownTask() { return countdownTask; }
    public void setCountdownTask(BukkitTask countdownTask) { this.countdownTask = countdownTask; }
    public int getCountdownSeconds() { return countdownSeconds; }
    public void setCountdownSeconds(int countdownSeconds) { this.countdownSeconds = countdownSeconds; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}