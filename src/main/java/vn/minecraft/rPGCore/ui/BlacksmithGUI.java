package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.commands.RepairAdminCommand;
import vn.minecraft.rPGCore.jobs.core.JobManager;
import vn.minecraft.rPGCore.jobs.core.JobQuestManager;
import vn.minecraft.rPGCore.jobs.core.PlayerJobData;
import vn.minecraft.rPGCore.managers.ItemManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BlacksmithGUI implements Listener {

    public static final String GUI_NAME = "§8Lò Rèn Trang Bị";
    private final double[] RATES = {100, 90, 80, 65, 50, 35, 25, 15, 5, 3, 2, 1, 0.5, 0.3, 0.1};
    private final List<Integer> SAFE_LEVELS = Arrays.asList(0, 3, 6, 9, 12, 15);
    private final Random rand = new Random();

    @EventHandler
    public void onInteractAnvil(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            if (event.getClickedBlock().getType() == Material.ANVIL || event.getClickedBlock().getType() == Material.CHIPPED_ANVIL || event.getClickedBlock().getType() == Material.DAMAGED_ANVIL) {
                event.setCancelled(true);
                openGUI(event.getPlayer());
            }
        }
    }

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_NAME);

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < 27; i++) {
            if (i == 10) continue;
            if (i >= 12 && i <= 16) continue;
            inv.setItem(i, glass);
        }

        inv.setItem(15, glass);

        updateUpgradeButtons(inv, player);
        updateMasterForgeButton(inv, player);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
    }

    private ItemStack createButton(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(loreLines));
        item.setItemMeta(meta);
        return item;
    }

    // ===============================================
    // [MỚI]: UPDATE GIAO DIỆN TỈ LỆ THÀNH CÔNG ĐỘNG
    // ===============================================
    private void updateUpgradeButtons(Inventory inv, Player player) {
        ItemStack equipment = inv.getItem(10);
        int currentLevel = 0;
        boolean hasItem = false;

        if (equipment != null && !equipment.getType().isAir() && equipment.hasItemMeta() && equipment.getItemMeta().hasLore()) {
            currentLevel = getUpgradeLevel(equipment);
            hasItem = true;
        }

        double baseRate = hasItem && currentLevel < 15 ? RATES[currentLevel] : 0;
        double jobBonus = JobManager.getForgeBonus(player);
        double finalRate = baseRate * (1.0 + (jobBonus / 100.0));

        List<String> lore1 = new ArrayList<>(Arrays.asList("§7Tiêu hao: 1 Đá Nâng Cấp"));
        List<String> lore10 = new ArrayList<>(Arrays.asList("§7Tiêu hao: Tối đa 10 Đá Nâng Cấp", "§7(Sẽ dừng lại ngay khi lên cấp)"));
        List<String> lore100 = new ArrayList<>(Arrays.asList("§7Tiêu hao: Tối đa 100 Đá Nâng Cấp", "§7(Sẽ dừng lại ngay khi lên cấp)"));

        if (hasItem && currentLevel < 15) {
            lore1.add(""); lore1.add("§f✦ Tỉ lệ cơ bản: §a" + baseRate + "%");
            lore10.add(""); lore10.add("§f✦ Tỉ lệ cơ bản: §a" + baseRate + "%");
            lore100.add(""); lore100.add("§f✦ Tỉ lệ cơ bản: §a" + baseRate + "%");

            if (jobBonus > 0) {
                lore1.add("§f✦ Tỉ lệ điều chỉnh (Nghề): §e" + String.format("%.2f", finalRate) + "%");
                lore10.add("§f✦ Tỉ lệ điều chỉnh (Nghề): §e" + String.format("%.2f", finalRate) + "%");
                lore100.add("§f✦ Tỉ lệ điều chỉnh (Nghề): §e" + String.format("%.2f", finalRate) + "%");
            }
        } else {
            lore1.add(""); lore1.add("§cChưa có trang bị");
            lore10.add(""); lore10.add("§cChưa có trang bị");
            lore100.add(""); lore100.add("§cChưa có trang bị");
        }

        inv.setItem(12, createButton(Material.LIME_STAINED_GLASS_PANE, "§a[+] Đập 1 Lần", lore1.toArray(new String[0])));
        inv.setItem(13, createButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§9[+] Đập Tự Động (Max 10 Lần)", lore10.toArray(new String[0])));
        inv.setItem(14, createButton(Material.PURPLE_STAINED_GLASS_PANE, "§5[+] Đập Tự Động (Max 100 Lần)", lore100.toArray(new String[0])));
    }

    private int getScrapCost(int currentLevel) {
        int[] costs = {2, 4, 8, 12, 16, 22, 30, 40, 50, 60, 70, 80, 150, 300, 600};
        if (currentLevel >= 0 && currentLevel < costs.length) return costs[currentLevel];
        return -1;
    }

    private void updateMasterForgeButton(Inventory inv, Player player) {
        ItemStack equipment = inv.getItem(10);

        if (equipment == null || equipment.getType().isAir() || !equipment.hasItemMeta() || !equipment.getItemMeta().hasLore()) {
            inv.setItem(16, createButton(Material.YELLOW_STAINED_GLASS_PANE, "§6[★] Rèn Bậc Thầy", "§cHãy đặt trang bị vào để xem chi phí.", "§7Cần nghề phụ Thợ Rèn để sử dụng."));
            return;
        }

        int currentLevel = getUpgradeLevel(equipment);
        if (currentLevel >= 15) {
            inv.setItem(16, createButton(Material.YELLOW_STAINED_GLASS_PANE, "§6[★] Rèn Bậc Thầy", "§cTrang bị đã đạt cấp độ tối đa."));
            return;
        }

        int cost = getScrapCost(currentLevel);

        // [KỸ NĂNG TỐI THƯỢNG]: Bậc Thầy Tiết Kiệm
        PlayerJobData data = JobManager.getPlayerData(player);
        if (data.hasUnlockedSkill("BLACKSMITH", "BS_MASTER_SAVER")) {
            cost = (int) (cost * 0.8);
        }

        inv.setItem(16, createButton(Material.YELLOW_STAINED_GLASS_PANE, "§6[★] Rèn Bậc Thầy",
                "§7Đột phá thẳng lên " + getLevelColor(currentLevel + 1) + "[+" + (currentLevel + 1) + "]",
                "§7Tiêu hao: §e" + cost + " Mạt Thợ Rèn",
                "",
                "§aTỉ lệ thành công: 100%"));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;

        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        if (slot == 10 || event.getClick().isShiftClick()) {
            Bukkit.getScheduler().runTaskLater(RPGCore.getInstance(), () -> {
                if (player.getOpenInventory().getTitle().equals(GUI_NAME)) {
                    Inventory topInv = player.getOpenInventory().getTopInventory();
                    updateMasterForgeButton(topInv, player);
                    updateUpgradeButtons(topInv, player);
                }
            }, 1L);
            return;
        }

        if (slot >= 0 && slot < 27) event.setCancelled(true);
        else return;

        ItemStack equipment = event.getInventory().getItem(10);

        if (slot >= 12 && slot <= 14) {
            if (equipment == null || equipment.getType().isAir() || !equipment.hasItemMeta() || !equipment.getItemMeta().hasLore()) {
                player.sendMessage("§cBạn chưa đặt trang bị RPG vào ô nâng cấp!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            int attempts = 1;
            if (slot == 13) attempts = 10;
            if (slot == 14) attempts = 100;

            processUpgradeLoop(player, equipment, attempts);
        }
        else if (slot == 16) {
            if (equipment == null || equipment.getType().isAir() || !equipment.hasItemMeta() || !equipment.getItemMeta().hasLore()) {
                player.sendMessage("§cBạn chưa đặt trang bị RPG vào ô nâng cấp!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            vn.minecraft.rPGCore.jobs.core.RPGJob currentJob = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerJob(player);
            if (!(currentJob instanceof vn.minecraft.rPGCore.jobs.types.BlacksmithJob)) {
                player.sendMessage("§c§l[!] §cKỹ năng này chỉ dành riêng cho Bậc thầy Thợ Rèn!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            int currentLevel = getUpgradeLevel(equipment);
            if (currentLevel >= 15) {
                player.sendMessage("§cTrang bị này đã đạt cấp độ tối đa (+15)!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            int cost = getScrapCost(currentLevel);
            if (JobManager.getPlayerData(player).hasUnlockedSkill("BLACKSMITH", "BS_MASTER_SAVER")) {
                cost = (int) (cost * 0.8);
            }

            ItemManager itemManager = RPGCore.getInstance().getItemManager();
            ItemStack scrapTemplate = itemManager.buildItem("misc_blacksmith_scrap", 1);

            if (itemManager.countItem(player, scrapTemplate) < cost) {
                player.sendMessage("§c§l[!] §cBạn không có đủ §e" + cost + " Mạt Thợ Rèn §cđể thực hiện Rèn Bậc Thầy!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            itemManager.takeItem(player, scrapTemplate, cost);
            vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.FORGE_ANY, currentLevel);
            vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.USE_MASTER_FORGE);
            vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.FORGE_SUCCESS, currentLevel);

            int newLevel = currentLevel + 1;
            vn.minecraft.rPGCore.jobs.core.JobManager.addExp(player, 100.0 + (newLevel * 10.0));

            if (SAFE_LEVELS.contains(newLevel)) applyMilestoneBonus(player, equipment);
            applyStatsChange(equipment, currentLevel, newLevel);

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            player.sendMessage("§e✨ §l[Rèn Bậc Thầy] §fThao tác thần sầu! Trang bị đột phá thẳng lên " + getLevelColor(newLevel) + "[+" + newLevel + "]");

            updateMasterForgeButton(event.getInventory(), player);
            updateUpgradeButtons(event.getInventory(), player);
        }
    }

    private void processUpgradeLoop(Player player, ItemStack item, int maxAttempts) {
        ItemStack stoneTemplate = RepairAdminCommand.getStoneItem(5, 1);
        int stonesUsed = 0;
        int currentLevel = getUpgradeLevel(item);
        int startLevel = currentLevel;
        int targetLevel = startLevel + 1;

        if (currentLevel >= 15) {
            player.sendMessage("§cTrang bị này đã đạt cấp độ tối đa (+15)!");
            return;
        }

        vn.minecraft.rPGCore.jobs.core.RPGJob currentJob = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerJob(player);
        PlayerJobData data = JobManager.getPlayerData(player);
        boolean hasGuardian = currentJob instanceof vn.minecraft.rPGCore.jobs.types.BlacksmithJob && data.hasUnlockedSkill("BLACKSMITH", "BS_GUARDIAN_CHARM");

        for (int i = 0; i < maxAttempts; i++) {
            if (currentLevel >= 15) break;
            if (countCustomItem(player, stoneTemplate) <= 0) break;

            double saveChance = 0.0;
            if (currentJob instanceof vn.minecraft.rPGCore.jobs.types.BlacksmithJob) {
                int jobLevel = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerJobLevel(player);
                saveChance = ((vn.minecraft.rPGCore.jobs.types.BlacksmithJob) currentJob).getStoneSaveChance(jobLevel);
            }

            if (rand.nextDouble() * 100.0 > saveChance) {
                JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.CONSUME_UPGRADE_STONE);
                removeCustomItem(player, stoneTemplate, 1);
            }
            stonesUsed++;

            JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.FORGE_ANY, currentLevel);

            ItemManager itemManager = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
            org.bukkit.inventory.ItemStack luckyHammerTemplate = itemManager.buildItem("misc_lucky_hammer", 1);
            double luckyBonus = 0.0;

            // LOGIC BÚA MAY MẮN + BÙA HỘ MỆNH
            if (luckyHammerTemplate != null && countCustomItem(player, luckyHammerTemplate) > 0) {
                luckyBonus = 10.0;
                player.sendMessage("§e[Búa May Mắn] §fSức mạnh tỏa ra! Tỉ lệ đập đồ tăng thêm 10%!");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 2f);

                boolean saved = false;
                if (hasGuardian && rand.nextDouble() * 100.0 <= 15.0) {
                    saved = true;
                    player.sendMessage("§d[Bùa Hộ Mệnh] §fÁnh sáng thần thánh bảo vệ Búa May Mắn không bị tiêu hao!");
                }
                if (!saved) removeCustomItem(player, luckyHammerTemplate, 1);
            }

            double jobSuccessBonus = JobManager.getForgeBonus(player);
            double successRate = (RATES[currentLevel] * (1.0 + (jobSuccessBonus / 100.0))) + luckyBonus;
            double roll = rand.nextDouble() * 100.0;

            if (roll <= successRate) {
                JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.FORGE_SUCCESS, currentLevel);
                data.setCurrentForgeStreak(data.getCurrentForgeStreak() + 1);
                vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.FORGE_STREAK, data.getCurrentForgeStreak());

                currentLevel++;
                JobManager.addExp(player, 30.0 + (currentLevel * 5.0));

                if (SAFE_LEVELS.contains(currentLevel)) applyMilestoneBonus(player, item);
                if (currentLevel == targetLevel) break;
            } else {
                JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.FORGE_FAIL, currentLevel);
                data.setCurrentForgeStreak(0);

                org.bukkit.inventory.ItemStack charmTemplate = itemManager.buildItem("misc_protection_charm", 1);
                boolean savedByCharm = false;

                // LOGIC BÙA BẢO VỆ + BÙA HỘ MỆNH
                if (charmTemplate != null && countCustomItem(player, charmTemplate) > 0) {
                    savedByCharm = true;
                    player.sendMessage("§d✨ [Bùa Bảo Vệ] §fSức mạnh ma thuật đã bảo vệ trang bị khỏi rớt cấp!");
                    player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1.5f);

                    boolean saved = false;
                    if (hasGuardian && rand.nextDouble() * 100.0 <= 15.0) {
                        saved = true;
                        player.sendMessage("§d✨ [Bùa Hộ Mệnh] §fÁnh sáng thần thánh bảo vệ Bùa Bảo Vệ không bị tiêu hao!");
                    }
                    if (!saved) removeCustomItem(player, charmTemplate, 1);
                }

                if (!SAFE_LEVELS.contains(currentLevel) && !savedByCharm) currentLevel--;
                JobManager.addExp(player, 15.0);

                // LOGIC TRẢ MẠT VÀ KỸ NĂNG BÀN TAY CỦA CHÚA
                if (currentJob instanceof vn.minecraft.rPGCore.jobs.types.BlacksmithJob) {
                    int jobLevel = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerJobLevel(player);
                    double scrapChance = ((vn.minecraft.rPGCore.jobs.types.BlacksmithJob) currentJob).getScrapRefundChance(jobLevel);

                    if (data.hasUnlockedSkill("BLACKSMITH", "BS_HAND_OF_GOD")) scrapChance += 20.0;

                    if (rand.nextDouble() * 100 <= scrapChance) {
                        player.sendMessage("§cĐập đồ thất bại... §aNhưng bạn thu lại được 1 Mạt Thợ Rèn!");
                        if (itemManager != null) {
                            org.bukkit.inventory.ItemStack scrap = itemManager.buildItem("misc_blacksmith_scrap", 1);
                            if (scrap != null) player.getInventory().addItem(scrap);
                        }
                    }
                }
            }
        }

        if (currentLevel != startLevel) applyStatsChange(item, startLevel, currentLevel);

        if (stonesUsed > 0) {
            if (currentLevel > startLevel) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                player.sendMessage("§a§lNâng cấp thành công! §fTrang bị đạt mức " + getLevelColor(currentLevel) + "[+" + currentLevel + "] §8(Dừng sau " + stonesUsed + " lần đập)");
            } else if (currentLevel < startLevel) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1f, 0.5f);
                player.sendMessage("§c§lNâng cấp thất bại! §fTrang bị rớt xuống " + getLevelColor(currentLevel) + "[+" + currentLevel + "] §8(Tiêu hao " + stonesUsed + " lượt đập)");
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                player.sendMessage("§eĐập " + stonesUsed + " lần thất bại! §fTrang bị giữ nguyên " + getLevelColor(currentLevel) + "[+" + currentLevel + "]");
            }

            updateMasterForgeButton(player.getOpenInventory().getTopInventory(), player);
            updateUpgradeButtons(player.getOpenInventory().getTopInventory(), player);
        } else {
            player.sendMessage("§cBạn không có Đá Nâng Cấp trong túi!");
        }
    }

    private int getUpgradeLevel(ItemStack item) {
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[\\+(\\d+)\\]$").matcher(name);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 0;
    }

    private String getLevelColor(int level) {
        if (level >= 1 && level <= 3) return "§a";
        if (level >= 4 && level <= 6) return "§9";
        if (level >= 7 && level <= 9) return "§5";
        if (level >= 10 && level <= 12) return "§c";
        if (level >= 13 && level <= 15) return "§6";
        return "§e";
    }

    private double getMultiplier(int level) {
        if (level == 0) return 1.0;
        double multi = 1.0;
        multi += Math.min(level, 10) * 0.10;
        if (level > 10) multi += (level - 10) * 0.15;
        return multi;
    }

    private void applyStatsChange(ItemStack item, int oldLevel, int newLevel) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        double oldMulti = getMultiplier(oldLevel);
        double newMulti = getMultiplier(newLevel);

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            String clean = ChatColor.stripColor(line).trim();

            if (clean.startsWith("- Sát thương vật lý:") || clean.startsWith("- Sát thương phép thuật:")) {
                String[] parts = clean.split(":");
                String prefix = line.substring(0, line.indexOf(":") + 1);
                String[] range = parts[1].split("~");

                double oldMin = Double.parseDouble(ChatColor.stripColor(range[0]).replace("+", "").trim());
                double oldMax = Double.parseDouble(ChatColor.stripColor(range[1]).replace("+", "").trim());

                double newMin = (oldMin / oldMulti) * newMulti;
                double newMax = (oldMax / oldMulti) * newMulti;

                lore.set(i, prefix + " §f" + String.format("%.1f", newMin) + " §8~ §f" + String.format("%.1f", newMax));
            }
            else if (clean.startsWith("- Máu tối đa:") || clean.startsWith("- Giáp:") || clean.startsWith("- Kháng phép:")) {
                String[] parts = clean.split(":");
                String prefix = line.substring(0, line.indexOf(":") + 1);
                double oldVal = Double.parseDouble(ChatColor.stripColor(parts[1]).replace("+", "").replace("%", "").trim());

                double newVal = (oldVal / oldMulti) * newMulti;
                String color = clean.startsWith("- Kháng phép:") ? "§b+" : (clean.startsWith("- Giáp:") ? "§f+" : "§a+");
                lore.set(i, prefix + " " + color + Math.round(newVal));
            }
        }

        String name = meta.getDisplayName();
        name = name.replaceAll(" (?:§.)?\\[\\+\\d+\\]", "");
        if (newLevel > 0) name = name + " " + getLevelColor(newLevel) + "[+" + newLevel + "]";
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private void applyMilestoneBonus(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();

        List<Integer> lineageLines = new ArrayList<>();
        List<Integer> subStatLines = new ArrayList<>();

        boolean isLineageSection = false;
        boolean isSubStatSection = false;

        for (int i = 0; i < lore.size(); i++) {
            String clean = ChatColor.stripColor(lore.get(i)).trim();
            if (clean.startsWith("✦ Thuộc tính Dòng dõi")) { isLineageSection = true; continue; }
            if (clean.startsWith("✦ Thuộc tính Phụ:")) { isLineageSection = false; isSubStatSection = true; continue; }
            if (clean.startsWith("✦") || clean.startsWith("[Set")) { isLineageSection = false; isSubStatSection = false; }

            if (isLineageSection && clean.startsWith("- ") && !clean.contains("Không có")) lineageLines.add(i);
            if (isSubStatSection && clean.startsWith("- ")) subStatLines.add(i);
        }

        if (!lineageLines.isEmpty()) {
            double jobLineageBonus = vn.minecraft.rPGCore.jobs.core.JobManager.getLineageBonus(player);
            double lineageChance = 0.50 + jobLineageBonus;

            if (rand.nextDouble() <= lineageChance) {
                int index1 = lineageLines.get(rand.nextInt(lineageLines.size()));
                boostStatLine(lore, index1);

                vn.minecraft.rPGCore.jobs.core.RPGJob currentJob = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerJob(player);
                if (currentJob instanceof vn.minecraft.rPGCore.jobs.types.BlacksmithJob && lineageLines.size() > 1) {
                    if (rand.nextDouble() <= jobLineageBonus) {
                        int index2;
                        do { index2 = lineageLines.get(rand.nextInt(lineageLines.size())); } while (index2 == index1);
                        boostStatLine(lore, index2);
                        player.sendMessage("§e✨ [Thợ Rèn] §fKỹ năng thượng thừa! Đột phá 2 dòng dòng dõi!");
                    }
                }
            }
        }
        if (!subStatLines.isEmpty()) {
            int index = subStatLines.get(rand.nextInt(subStatLines.size()));
            boostStatLine(lore, index);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private void boostStatLine(List<String> lore, int index) {
        String line = lore.get(index);
        int starCount = 0;
        for (char c : line.toCharArray()) if (c == '★') starCount++;

        String clean = ChatColor.stripColor(line).replace("★", "").trim();
        String[] parts = clean.split("\\+");
        if (parts.length < 2) return;

        String prefix = line.substring(0, line.indexOf("+") + 1);
        String valueStr = parts[1].replace("%", "").trim();

        try {
            double val = Double.parseDouble(valueStr);
            double newVal = val * 1.3;
            StringBuilder stars = new StringBuilder(" §e");
            for (int i = 0; i <= starCount; i++) stars.append("★");
            lore.set(index, prefix + String.format("%.1f", newVal) + "%" + stars.toString());
        } catch (Exception ignored) {}
    }

    private int countCustomItem(Player player, ItemStack template) {
        return vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager().countItem(player, template);
    }

    private void removeCustomItem(Player player, ItemStack template, int amount) {
        vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager().takeItem(player, template, amount);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(GUI_NAME)) {
            ItemStack item = event.getInventory().getItem(10);
            if (item != null && !item.getType().isAir()) event.getPlayer().getInventory().addItem(item);
        }
    }
}