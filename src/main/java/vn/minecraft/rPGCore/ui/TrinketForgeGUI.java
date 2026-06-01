package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.ItemManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class TrinketForgeGUI implements CommandExecutor, Listener {

    public static final String GUI_NAME = "§d§lGia Công Trang Sức";
    private final Random rand = new Random();

    private final double[] RATES = {70.0, 50.0, 35.0, 20.0, 10.0};
    private final int[] MASTER_COSTS = {2, 4, 8, 12, 20};

    public TrinketForgeGUI(ItemManager ignoredManager) {
    }

    private ItemManager getItemManager() {
        return RPGCore.getInstance().getItemManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) openGUI((Player) sender);
        return true;
    }

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_NAME);
        ItemStack glass = createButton(Material.PINK_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 27; i++) {
            if (i == 10 || (i >= 12 && i <= 14) || i == 16) continue;
            inv.setItem(i, glass);
        }

        inv.setItem(15, glass);
        updateButtons(inv);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
    }

    private ItemStack createButton(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (loreLines.length > 0) meta.setLore(Arrays.asList(loreLines));
        item.setItemMeta(meta);
        return item;
    }

    private void updateButtons(Inventory inv) {
        ItemStack trinket = inv.getItem(10);
        int currentLevel = 0;
        boolean hasItem = false;

        if (trinket != null && trinket.hasItemMeta() && trinket.getItemMeta().getPersistentDataContainer().has(new org.bukkit.NamespacedKey(RPGCore.getInstance(), "trinket_type"), PersistentDataType.STRING)) {
            currentLevel = getUpgradeLevel(trinket);
            hasItem = true;
        }

        List<String> lore1 = new ArrayList<>(Arrays.asList("§7Tiêu hao: 1 Tinh Thể Pha Lê"));
        List<String> lore10 = new ArrayList<>(Arrays.asList("§7Tiêu hao: Tối đa 10 Tinh Thể", "§7(Sẽ dừng lại ngay khi lên cấp)"));
        List<String> lore100 = new ArrayList<>(Arrays.asList("§7Tiêu hao: Tối đa 100 Tinh Thể", "§7(Sẽ dừng lại ngay khi lên cấp)"));

        if (hasItem && currentLevel < 5) {
            String rateStr = "§fTỉ lệ cơ bản: §a" + RATES[currentLevel] + "%";

            lore1.addAll(Arrays.asList("", rateStr));
            lore10.addAll(Arrays.asList("", rateStr));
            lore100.addAll(Arrays.asList("", rateStr));

            inv.setItem(16, createButton(Material.AMETHYST_CLUSTER, "§d[★] Cường Hóa Bậc Thầy",
                    "§7Đột phá thẳng lên §d[+" + (currentLevel + 1) + "]",
                    "§7Tiêu hao: §e" + MASTER_COSTS[currentLevel] + " Tinh Thể Pha Lê",
                    "",
                    "§fTỉ lệ cơ bản: §a100.0%"
            ));
        } else {
            lore1.addAll(Arrays.asList("", "§cChưa có trang sức", "§choặc trang sức đã max cấp."));
            lore10.addAll(Arrays.asList("", "§cChưa có trang sức", "§choặc trang sức đã max cấp."));
            lore100.addAll(Arrays.asList("", "§cChưa có trang sức", "§choặc trang sức đã max cấp."));

            inv.setItem(16, createButton(Material.AMETHYST_CLUSTER, "§d[★] Cường Hóa Bậc Thầy", "§cChưa có trang sức hoặc đã max cấp."));
        }

        inv.setItem(12, createButton(Material.LIME_STAINED_GLASS_PANE, "§a[+] Đập 1 Lần", lore1.toArray(new String[0])));
        inv.setItem(13, createButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§9[+] Đập Tự Động (Max 10)", lore10.toArray(new String[0])));
        inv.setItem(14, createButton(Material.PURPLE_STAINED_GLASS_PANE, "§5[+] Đập Tự Động (Max 100)", lore100.toArray(new String[0])));
    }

    @EventHandler
    public void onGrindstoneInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        org.bukkit.block.Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        Player player = event.getPlayer();
        if (player.isSneaking() && clickedBlock.getType() == Material.GRINDSTONE) {
            event.setCancelled(true);
            this.openGUI(player);
            player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.5f, 1.5f);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;

        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        // ==========================================
        // 1. [SỬA LỖI]: CHẶN TUYỆT ĐỐI SHIFT-CLICK VÀ PHÍM TẮT
        // ==========================================
        if (event.getClick().isShiftClick() || event.getClick().isKeyboardClick()) {
            event.setCancelled(true); // Khóa đứng hành vi nhấc kính/tráo đồ
            return;
        }

        // 2. Cho phép người chơi kéo-thả thủ công vào ô 10
        if (slot == 10) {
            Bukkit.getScheduler().runTaskLater(RPGCore.getInstance(), () -> updateButtons(player.getOpenInventory().getTopInventory()), 1L);
            return;
        }

        // 3. Chặn tương tác với kính nền ở các ô khác
        if (slot >= 0 && slot < 27 && (slot < 12 || slot > 14) && slot != 16) {
            event.setCancelled(true);
            return;
        }

        ItemStack trinket = event.getInventory().getItem(10);

        if (slot >= 12 && slot <= 14) {
            event.setCancelled(true);
            if (!isValidTrinket(trinket)) {
                player.sendMessage("§cBạn chưa đặt Trang sức vào ô!");
                return;
            }
            int attempts = (slot == 12) ? 1 : (slot == 13) ? 10 : 100;
            processLoop(player, trinket, attempts);
        } else if (slot == 16) {
            event.setCancelled(true);
            if (!isValidTrinket(trinket)) {
                player.sendMessage("§cBạn chưa đặt Trang sức vào ô!");
                return;
            }

            int currentLevel = getUpgradeLevel(trinket);
            if (currentLevel >= 5) {
                player.sendMessage("§cTrang sức đã đạt sức mạnh tối đa!");
                return;
            }

            int cost = MASTER_COSTS[currentLevel];
            ItemStack crystalTemplate = vn.minecraft.rPGCore.commands.RepairAdminCommand.getStoneItem(6, 1);

            if (getItemManager().countItem(player, crystalTemplate) < cost) {
                player.sendMessage("§c§l[!] §cBạn cần " + cost + " Tinh Thể Pha Lê để Cường hóa Bậc thầy!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            getItemManager().takeItem(player, crystalTemplate, cost);

            currentLevel++;
            if (currentLevel == 1 || currentLevel == 3 || currentLevel == 5) applyTrinketMilestoneBoost(trinket);
            applyTrinketNameChange(trinket, currentLevel);

            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1f, 1.5f);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
            player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            player.sendMessage("§d§l[Gia Công] §fĐột phá Bậc thầy! Trang sức tiến thẳng lên §d[+" + currentLevel + "]");

            if (currentLevel >= 4) {
                Bukkit.broadcastMessage("§d§l[Gia Công] §a" + player.getName() + " §fvừa đột phá Bậc thầy thành công §d" + trinket.getItemMeta().getDisplayName() + "§f!");
            }

            updateButtons(event.getInventory());
        }
    }

    private void processLoop(Player player, ItemStack trinket, int maxAttempts) {
        int currentLevel = getUpgradeLevel(trinket);
        if (currentLevel >= 5) return;

        ItemStack crystalTemplate = vn.minecraft.rPGCore.commands.RepairAdminCommand.getStoneItem(6, 1);
        int crystalsUsed = 0;
        boolean success = false;
        boolean dropped = false;
        boolean savedByCharmTotal = false;

        for (int i = 0; i < maxAttempts; i++) {
            if (currentLevel >= 5) break;
            if (getItemManager().countItem(player, crystalTemplate) <= 0) break;

            getItemManager().takeItem(player, crystalTemplate, 1);
            crystalsUsed++;

            if (rand.nextDouble() * 100.0 <= RATES[currentLevel]) {
                currentLevel++;
                if (currentLevel == 1 || currentLevel == 3 || currentLevel == 5) {
                    applyTrinketMilestoneBoost(trinket);
                }
                applyTrinketNameChange(trinket, currentLevel);
                success = true;
                break;
            } else {
                // Nếu đang ở mốc an toàn thì không làm gì cả, vòng lặp chạy tiếp
                if (currentLevel == 1 || currentLevel == 3 || currentLevel == 5) {
                    // Do nothing, safe milestone
                } else if (currentLevel > 0) {
                    // Kiểm tra Bùa Bảo Vệ khi ở mốc không an toàn (+2, +4)
                    ItemStack charmTemplate = getItemManager().buildItem("misc_protection_charm", 1);
                    boolean savedByCharm = false;

                    if (charmTemplate != null && getItemManager().countItem(player, charmTemplate) > 0) {
                        savedByCharm = true;
                        savedByCharmTotal = true;
                        getItemManager().takeItem(player, charmTemplate, 1);
                        player.sendMessage("§d✨ [Bùa Bảo Vệ] §fSức mạnh ma thuật đã bảo vệ trang sức khỏi rớt cấp!");
                        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1.5f);
                    }

                    if (!savedByCharm) {
                        currentLevel--;
                        applyTrinketNameChange(trinket, currentLevel);
                        dropped = true;
                        break; // Rớt cấp thì bắt buộc dừng vòng lặp Auto
                    }
                }
            }
        }

        if (crystalsUsed > 0) {
            if (success) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                player.sendMessage("§d§l[Gia Công] §aNâng cấp thành công lên §d[+" + currentLevel + "] §8(Tiêu hao " + crystalsUsed + " Tinh thể)");

                if (currentLevel >= 4) {
                    Bukkit.broadcastMessage("§d§l[Gia Công] §a" + player.getName() + " §fvừa cường hóa thành công §d" + trinket.getItemMeta().getDisplayName() + "§f!");
                }
            } else if (dropped) {
                player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1f, 0.5f);
                player.sendMessage("§d§l[Gia Công] §cThất bại! Trang sức rớt xuống §d[+" + currentLevel + "]");
            } else if (savedByCharmTotal) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                player.sendMessage("§d§l[Gia Công] §eThất bại! Đã được bảo vệ bởi Bùa Bảo Vệ.");
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
                player.sendMessage("§d§l[Gia Công] §eThất bại! Rất may đồ được bảo toàn cấp độ.");
            }
            updateButtons(player.getOpenInventory().getTopInventory());
        } else {
            player.sendMessage("§cBạn không có đủ Tinh Thể Pha Lê trong túi!");
        }
    }

    private boolean isValidTrinket(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(new org.bukkit.NamespacedKey(RPGCore.getInstance(), "trinket_type"), PersistentDataType.STRING);
    }

    private int getUpgradeLevel(ItemStack item) {
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[\\+(\\d+)\\]$").matcher(name);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 0;
    }

    private void applyTrinketNameChange(ItemStack item, int newLevel) {
        ItemMeta meta = item.getItemMeta();
        String name = meta.getDisplayName();
        name = name.replaceAll(" (?:§.)?\\[\\+\\d+\\]", "");
        if (newLevel > 0) name = name + " §d[+" + newLevel + "]";
        meta.setDisplayName(name);
        item.setItemMeta(meta);
    }

    private void applyTrinketMilestoneBoost(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return;
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return;

        List<Integer> lineageLines = new ArrayList<>();
        List<Integer> validMagicLines = new ArrayList<>();

        boolean isLineageSection = false;
        boolean isMagicSection = false;

        for (int i = 0; i < lore.size(); i++) {
            String clean = ChatColor.stripColor(lore.get(i)).trim();
            if (clean.contains("Thuộc tính Dòng dõi")) { isLineageSection = true; isMagicSection = false; continue; }
            if (clean.contains("Thuộc tính Ma thuật")) { isMagicSection = true; isLineageSection = false; continue; }
            if (clean.startsWith("✦") || clean.startsWith("[Set")) { isLineageSection = false; isMagicSection = false; }

            if (isLineageSection && clean.startsWith("- ") && !clean.contains("Không có")) lineageLines.add(i);
            if (isMagicSection && clean.startsWith("- ")) validMagicLines.add(i);
        }

        // ==========================================
        // 1. BUFF DÒNG DÕI (Ít nhất 1 dòng, 20% ra 2 dòng, THÊM 1 SAO)
        // ==========================================
        if (!lineageLines.isEmpty()) {
            int index1 = lineageLines.get(rand.nextInt(lineageLines.size()));
            addStarsAndMultiply(lore, index1, 1.25, 1); // Tăng 25% và thêm 1 sao

            // 20% cơ hội tăng thêm 1 dòng nữa (nếu có hơn 1 dòng)
            if (lineageLines.size() > 1 && rand.nextDouble() <= 0.20) {
                int index2;
                do {
                    index2 = lineageLines.get(rand.nextInt(lineageLines.size()));
                } while (index2 == index1); // Phải chọn dòng khác
                addStarsAndMultiply(lore, index2, 1.25, 1); // Tăng 25% và thêm 1 sao
            }
        }

        // ==========================================
        // 2. BUFF DÒNG MA THUẬT (Thêm 1-2 sao)
        // ==========================================
        if (!validMagicLines.isEmpty()) {
            int chosenLineIndex = validMagicLines.get(rand.nextInt(validMagicLines.size()));
            boolean twoStars = (rand.nextInt(100) < 30);
            double multi = twoStars ? 1.6 : 1.3;
            int starsToAdd = twoStars ? 2 : 1;

            addStarsAndMultiply(lore, chosenLineIndex, multi, starsToAdd);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    // ==========================================
    // HÀM PHỤ TRỢ: GỘP CHUNG LOGIC GẮN SAO CHO GỌN CODE
    // ==========================================
    private void addStarsAndMultiply(List<String> lore, int index, double multi, int starsToAdd) {
        String oldLine = lore.get(index);

        // 1. Đếm số sao hiện tại
        int currentStars = 0;
        for (char c : ChatColor.stripColor(oldLine).toCharArray()) {
            if (c == '★') currentStars++;
        }
        int totalStars = currentStars + starsToAdd;

        // 2. Cạo sạch sao và mã màu thừa ở chuỗi cũ
        String cleanLine = oldLine.replace("★", "").trim();
        cleanLine = cleanLine.replaceAll("( §[0-9a-fk-or])+$", "").trim();

        // 3. Xây dựng dải sao mới liền mạch
        StringBuilder stars = new StringBuilder(" §e");
        for(int s = 0; s < totalStars; s++) stars.append("★");

        // 4. Nhân chỉ số và dán dải sao vào
        lore.set(index, multiplyLoreValue(cleanLine, multi, stars.toString()));
    }

    private String multiplyLoreValue(String line, double multi, String appendString) {
        int colonIdx = line.indexOf(":");
        if (colonIdx == -1) return line + appendString;

        String valuePart = line.substring(colonIdx + 1);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([+0-9]*\\.?[0-9]+)").matcher(ChatColor.stripColor(valuePart));

        if (m.find()) {
            String numStr = m.group(1);
            try {
                double oldVal = Double.parseDouble(numStr.replace("+", ""));
                double newVal = oldVal * multi;
                String newNumStr = (numStr.startsWith("+") ? "+" : "") + String.format("%.1f", newVal);
                return line.replaceFirst(java.util.regex.Pattern.quote(numStr), newNumStr) + appendString;
            } catch (Exception ignored) {}
        }
        return line + appendString;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(GUI_NAME)) {
            ItemStack item = event.getInventory().getItem(10);
            if (item != null && !item.getType().isAir()) event.getPlayer().getInventory().addItem(item);
        }
    }
}