package vn.minecraft.rPGCore.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class EquipmentCommand implements CommandExecutor, TabCompleter {

    private final Object[][] TIERS = {
            {"§7", "common", 1.0, 0.40, 0.70},
            {"§a", "uncommon", 1.2, 0.60, 0.85},
            {"§b", "rare", 1.5, 0.80, 1.00},
            {"§5", "epic", 1.8, 0.90, 1.05},
            {"§d", "mythical", 2.2, 0.95, 1.10},
            {"§6", "heroic", 2.8, 1.00, 1.15},
            {"§c", "legendary", 3.5, 1.05, 1.15},
            {"§e", "divine", 5.0, 1.10, 1.15}
    };

    private final String[][] QUALITIES = {
            {"Rusty", "Broken", "Ruined", "Chipped"},
            {"Old", "Worn", "Dull", "Faded"},
            {"Normal", "Standard", "Decent", "Common"},
            {"Fine", "Polished", "Refined", "Sturdy"},
            {"Reinforced", "Flawless", "Exceptional"},
            {"Masterwork", "Heroic", "Glorious"},
            {"Legendary", "Exquisite", "Perfect"},
            {"Divine", "Godly", "Ascended", "Transcendent"}
    };

    private final List<String> VALID_CLASSES = Arrays.asList("warrior", "archer", "mage", "assassin", "summoner", "reaper", "guardian");
    private final List<String> VALID_TIERS = Arrays.asList("common", "uncommon", "rare", "epic", "mythical", "heroic", "legendary", "divine");
    private final List<String> ALL_LINEAGES = Arrays.asList("vampiric", "experienced", "hunter", "magical", "giant", "armored", "scavenger", "swift", "shattering");
    private final List<String> WEAPON_LINEAGES = Arrays.asList("vampiric", "experienced", "hunter", "magical", "scavenger", "swift", "shattering");

    // DATABASE MỚI: { "Tên chỉ số", Giới_hạn_Max (0=Không giới hạn), Trọng_số_Weight }
    // Weight càng nhỏ -> Càng hiếm ra (Ví dụ: Giảm hồi chiêu cực kỳ khó ra so với Hồi phục)
    private final Object[][] RANDOM_STATS_DB = {
            {"Tỉ lệ chí mạng", 100, 50},       // Rare
            {"Sát thương chí mạng", 0, 50},    // Rare
            {"Tỉ lệ xuyên giáp", 80, 30},      // Epic
            {"Tỉ lệ hút máu", 50, 15},         // Mythic (Rất khó ra)
            {"Tỉ lệ đỡ đòn", 50, 60},          // Uncommon
            {"Tỉ lệ né tránh", 50, 60},        // Uncommon
            {"Tỉ lệ hồi phục", 0, 100},        // Common (Dễ ra nhất)
            {"Tốc độ đánh", 100, 40},          // Rare
            {"Giảm hồi chiêu", 40, 10},        // Legendary (Cực kỳ khó ra)
            {"Tốc độ di chuyển", 0, 80}        // Common (Đã gỡ giới hạn thành 0)
    };

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!player.hasPermission("rpgcore.admin")) {
            player.sendMessage("§cBạn không có quyền dùng lệnh này!");
            return true;
        }

        if (args.length < 3 || !args[0].equalsIgnoreCase("roll")) {
            player.sendMessage("§eCú pháp: /eq roll <weapon|armor> <level> [class] [tier] [lineage] [sub:X] [set:1|2|3|4] [num:X]");
            return true;
        }

        try {
            String type = args[1].toLowerCase();
            int level = Integer.parseInt(args[2]);
            boolean isWeapon = type.equals("weapon");

            String inputClass = null;
            int inputTier = -1;
            String inputLineage = null;
            int inputSubStats = -1;
            int inputArmorPiece = -1;
            int amount = 1;

            for (int i = 3; i < args.length; i++) {
                String arg = args[i].toLowerCase();
                if (arg.startsWith("sub:")) inputSubStats = Integer.parseInt(arg.substring(4));
                else if (arg.startsWith("set:")) inputArmorPiece = Integer.parseInt(arg.substring(4));
                else if (arg.startsWith("num:")) amount = Integer.parseInt(arg.substring(4));
                else if (VALID_CLASSES.contains(arg)) inputClass = arg;
                else if (VALID_TIERS.contains(arg)) inputTier = VALID_TIERS.indexOf(arg);
                else if (ALL_LINEAGES.contains(arg)) inputLineage = arg;
            }

            Random rand = new Random();

            for (int n = 0; n < amount; n++) {

                String rpgClass = inputClass != null ? inputClass : VALID_CLASSES.get(rand.nextInt(VALID_CLASSES.size()));
                int tierIndex = inputTier != -1 ? inputTier : rand.nextInt(TIERS.length);
                int subStats = inputSubStats != -1 ? inputSubStats : rand.nextInt(4) + 1;
                int armorPiece = inputArmorPiece != -1 ? inputArmorPiece : rand.nextInt(4) + 1;

                String lineage = inputLineage;
                if (lineage == null) {
                    List<String> pool = isWeapon ? WEAPON_LINEAGES : ALL_LINEAGES;
                    lineage = pool.get(rand.nextInt(pool.size()));
                } else if (isWeapon && (lineage.equals("giant") || lineage.equals("armored"))) {
                    lineage = WEAPON_LINEAGES.get(rand.nextInt(WEAPON_LINEAGES.size()));
                }

                String tierColor = (String) TIERS[tierIndex][0];
                double tierMulti = (double) TIERS[tierIndex][2];
                double minVar = (double) TIERS[tierIndex][3];
                double maxVar = (double) TIERS[tierIndex][4];

                double qualityMod = minVar + (rand.nextDouble() * (maxVar - minVar));
                String quality = QUALITIES[tierIndex][rand.nextInt(QUALITIES[tierIndex].length)];

                Material mat;
                String baseItemName;

                if (isWeapon) {
                    Object[] weaponData = generateWeaponMaterial(rpgClass, tierIndex, rand);
                    mat = (Material) weaponData[0];
                    baseItemName = (String) weaponData[1];
                } else {
                    Object[] armorData = generateArmorMaterial(armorPiece, tierIndex);
                    mat = (Material) armorData[0];
                    baseItemName = (String) armorData[1];
                }

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();

                meta.setDisplayName(tierColor + "§l" + quality + " " + capitalize(lineage) + " " + baseItemName);

                List<String> lore = new ArrayList<>();

                lore.add("");
                lore.add("§8▶ Thuộc class: §f" + (isWeapon ? capitalize(rpgClass) : "Tất cả"));
                lore.add("§8▶ Yêu cầu cấp độ: " + level);
                lore.add("");

                // CHỈ SỐ CHÍNH VÀ ĐỘ BỀN
                lore.add(tierColor + "§l✦ Chỉ số chính:");
                int maxDurability; // Biến lưu độ bền

                if (isWeapon) {
                    double baseAtk = level * 4.5 * tierMulti * qualityMod;
                    double minAtk = baseAtk * 0.85;
                    double maxAtk = baseAtk * 1.15;
                    String dmgType = rpgClass.equalsIgnoreCase("mage") ? "Sát thương phép thuật" : "Sát thương vật lý";

                    lore.add("§7 - " + dmgType + ": §f" + String.format("%.1f", minAtk) + " §8~ §f" + String.format("%.1f", maxAtk));

                    // Vũ khí có base độ bền là 1500
                    maxDurability = (int) (1500 * (tierMulti * 0.3 + qualityMod * 0.7));
                } else {
                    double baseHp = level * 15 * tierMulti * qualityMod;
                    double baseArmor = level * 1.5 * tierMulti * qualityMod;
                    lore.add("§7 - Máu tối đa: §a+" + Math.round(baseHp));
                    lore.add("§7 - Giáp: §f+" + Math.round(baseArmor));

                    // Giáp có base độ bền là 2500
                    maxDurability = (int) (2500 * (tierMulti * 0.3 + qualityMod * 0.7));
                }

                // Thêm dòng Độ bền ngay dưới chỉ số chính
                lore.add("§7 - Độ bền: §a" + maxDurability + "§8/§a" + maxDurability);
                lore.add("");

                // DÒNG DÕI LẺ
                lore.add(tierColor + "§l✦ Thuộc tính Dòng dõi (" + capitalize(lineage) + "):");
                applyLineageStatsOnly(lore, lineage.toLowerCase(), tierMulti, qualityMod);
                lore.add("");

                // DÒNG PHỤ KÈM HIỂN THỊ MAX CAP & WEIGHTED RANDOM
                if (subStats > 0) {
                    lore.add("§d§l✦ Thuộc tính Phụ:");

                    // Thuật toán Gacha theo Weight
                    List<Integer> availableIndices = new ArrayList<>();
                    for (int i = 0; i < RANDOM_STATS_DB.length; i++) {
                        availableIndices.add(i);
                    }

                    List<Integer> selectedIndices = new ArrayList<>();
                    int actualSubStats = Math.min(subStats, availableIndices.size());

                    for (int i = 0; i < actualSubStats; i++) {
                        // Tính tổng Weight của các dòng chưa được chọn
                        int totalWeight = 0;
                        for (int index : availableIndices) {
                            totalWeight += (int) RANDOM_STATS_DB[index][2];
                        }

                        // Quay số
                        int roll = rand.nextInt(totalWeight);
                        int currentWeight = 0;
                        int chosenIndex = -1;

                        for (int j = 0; j < availableIndices.size(); j++) {
                            int index = availableIndices.get(j);
                            currentWeight += (int) RANDOM_STATS_DB[index][2];
                            if (roll < currentWeight) {
                                chosenIndex = index;
                                availableIndices.remove(j); // Xóa khỏi danh sách để không trùng lặp
                                break;
                            }
                        }

                        if (chosenIndex != -1) {
                            selectedIndices.add(chosenIndex);
                        }
                    }

                    // Sắp xếp lại danh sách đã quay trúng để in ra theo thứ tự cố định
                    Collections.sort(selectedIndices);

                    // Thay thế đoạn code in dòng phụ cũ bằng đoạn này (Bỏ capStr)
                    for (int index : selectedIndices) {
                        String statName = (String) RANDOM_STATS_DB[index][0];
                        double value = (1.0 + rand.nextDouble() * 2.5) * tierMulti * qualityMod;

                        // Đã gỡ bỏ chuỗi capStr để thẻ đồ trông gọn gàng, bí ẩn hơn
                        lore.add("§7 - " + statName + ": §e+" + String.format("%.1f", value) + "%");
                    }
                    lore.add("");
                }

                // SET BONUS
                if (!isWeapon) {
                    applySetBonus(lore, lineage.toLowerCase());
                    lore.add("");
                }

                meta.setLore(lore);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(meta);

                player.getInventory().addItem(item);
            }
            player.sendMessage("§aĐã roll thành công " + amount + " trang bị!");

        } catch (Exception e) {
            player.sendMessage("§cLỗi cú pháp! Vui lòng nhập đúng format.");
            e.printStackTrace();
        }

        return true;
    }

    private Object[] generateWeaponMaterial(String rpgClass, int tier, Random rand) {
        String[] swordPrefixes = {"WOODEN_", "STONE_", "COPPER_", "IRON_", "GOLDEN_", "DIAMOND_", "NETHERITE_", "NETHERITE_"};
        String prefix = swordPrefixes[tier];

        Material mat;
        String name;

        switch (rpgClass) {
            case "archer":
                boolean isBow = rand.nextBoolean();
                mat = isBow ? Material.BOW : Material.CROSSBOW;
                name = isBow ? (rand.nextBoolean() ? "Bow" : "Longbow") : "Crossbow";
                break;
            case "mage":
                int m = rand.nextInt(3);
                if (m == 0) { mat = Material.STICK; name = "Nature Staff"; }
                else if (m == 1) { mat = Material.BLAZE_ROD; name = "Flame Staff"; }
                else { mat = Material.BREEZE_ROD; name = "Wind Staff"; }
                break;
            case "assassin":
                mat = Material.SHEARS; name = "Shears"; break;
            case "summoner":
                mat = Material.LEAD; name = "Lead"; break;
            case "reaper":
                mat = Material.valueOf(prefix + "HOE");
                name = rand.nextBoolean() ? "Sickle" : "Scythe";
                break;
            case "guardian":
                mat = Material.SHIELD; name = "Shield"; break;
            case "warrior":
            default:
                boolean isSword = rand.nextBoolean();
                mat = Material.valueOf(prefix + (isSword ? "SWORD" : "AXE"));
                name = isSword ? "Sword" : "Axe";
                break;
        }
        return new Object[]{mat, name};
    }

    private Object[] generateArmorMaterial(int piece, int tier) {
        String[] matPrefixes = {"LEATHER_", "COPPER_", "CHAINMAIL_", "IRON_", "GOLDEN_", "DIAMOND_", "NETHERITE_", "NETHERITE_"};
        String prefix = matPrefixes[tier];

        Material mat;
        String name;

        switch (piece) {
            case 1: mat = Material.valueOf(prefix + "HELMET"); name = "Helmet"; break;
            case 3: mat = Material.valueOf(prefix + "LEGGINGS"); name = "Leggings"; break;
            case 4: mat = Material.valueOf(prefix + "BOOTS"); name = "Boots"; break;
            case 2:
            default: mat = Material.valueOf(prefix + "CHESTPLATE"); name = "Chestplate"; break;
        }
        return new Object[]{mat, name};
    }

    private void applyLineageStatsOnly(List<String> lore, String lineage, double tierMulti, double qualityMod) {
        switch (lineage) {
            case "vampiric": lore.add("§c - Hút máu: +" + String.format("%.1f", 3 * tierMulti * qualityMod) + "%"); break;
            case "experienced": lore.add("§b - Nhận Kinh nghiệm: +" + String.format("%.1f", 5 * tierMulti * qualityMod) + "%"); break;
            case "hunter":
                lore.add("§e - Tỉ lệ chí mạng: +" + String.format("%.1f", 2 * tierMulti * qualityMod) + "%");
                lore.add("§e - Sát thương chí mạng: +" + String.format("%.1f", 8 * tierMulti * qualityMod) + "%");
                break;
            case "magical": lore.add("§9 - Hồi Năng lượng: +" + String.format("%.1f", 5 * tierMulti * qualityMod) + "%"); break;
            case "giant": lore.add("§a - Máu tối đa: +" + String.format("%.1f", 5 * tierMulti * qualityMod) + "%"); break;
            case "armored": lore.add("§6 - Tổng Giáp: +" + String.format("%.1f", 5 * tierMulti * qualityMod) + "%"); break;
            case "scavenger": lore.add("§e - Tỉ lệ rớt đồ: +" + String.format("%.1f", 4 * tierMulti * qualityMod) + "%"); break;
            case "swift": lore.add("§f - Tốc độ chạy: +" + String.format("%.1f", 3 * tierMulti * qualityMod) + "%"); break;
            case "shattering": lore.add("§4 - Xuyên giáp: +" + String.format("%.1f", 4 * tierMulti * qualityMod) + "%"); break;
            default: lore.add("§7 - Không có thuộc tính bổ sung."); break;
        }
    }

    private void applySetBonus(List<String> lore, String lineage) {
        switch (lineage) {
            case "vampiric":
                lore.add("§8[Set 3 mảnh]: +12% Hút máu");
                lore.add("§8[Set 4 mảnh]: Hút máu dư chuyển thành Giáp ảo (20% Max HP)");
                break;
            case "experienced":
                lore.add("§8[Set 3 mảnh]: +15% Kinh nghiệm nhận được");
                lore.add("§8[Set 4 mảnh]: Giảm 15% mọi nguồn sát thương nhận vào");
                break;
            case "hunter":
                lore.add("§8[Set 3 mảnh]: +8% Tỉ lệ chí mạng");
                lore.add("§8[Set 4 mảnh]: +40% ST Chí mạng. Bạo kích tăng 10% Tốc chạy");
                break;
            case "magical":
                lore.add("§8[Set 3 mảnh]: +20% Tốc độ hồi Mana");
                lore.add("§8[Set 4 mảnh]: Giảm 15% năng lượng tiêu hao");
                break;
            case "giant":
                lore.add("§8[Set 3 mảnh]: +25% Máu tối đa");
                lore.add("§8[Set 4 mảnh]: Rời giao tranh hồi 1% Máu/s");
                break;
            case "armored":
                lore.add("§8[Set 3 mảnh]: +25% Tổng Giáp");
                lore.add("§8[Set 4 mảnh]: Phản lại 15% sát thương gánh chịu");
                break;
            case "scavenger":
                lore.add("§8[Set 3 mảnh]: +20% Tỉ lệ rớt đồ");
                lore.add("§8[Set 4 mảnh]: Có 5% tỉ lệ x2 số lượng vật phẩm");
                break;
            case "swift":
                lore.add("§8[Set 3 mảnh]: +15% Tốc độ di chuyển");
                lore.add("§8[Set 4 mảnh]: Miễn nhiễm hoàn toàn với Làm chậm");
                break;
            case "shattering":
                lore.add("§8[Set 3 mảnh]: +15% Xuyên giáp");
                lore.add("§8[Set 4 mảnh]: Đòn đánh giảm 20% Giáp mục tiêu");
                break;
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            completions.addAll(Arrays.asList("weapon", "armor"));
        } else if (args.length == 3) {
            completions.addAll(Arrays.asList("1", "10", "30", "50", "100"));
        } else if (args.length >= 4) {
            completions.addAll(VALID_CLASSES);
            completions.addAll(VALID_TIERS);
            completions.addAll(ALL_LINEAGES);
            completions.addAll(Arrays.asList("sub:1", "sub:2", "sub:3", "sub:4"));
            completions.addAll(Arrays.asList("set:1", "set:2", "set:3", "set:4"));
            completions.addAll(Arrays.asList("num:1", "num:5", "num:10"));
        }

        String currentWord = args[args.length - 1].toLowerCase();
        List<String> filtered = new ArrayList<>();
        for (String s : completions) {
            if (s.toLowerCase().startsWith(currentWord)) {
                filtered.add(s);
            }
        }
        return filtered;
    }
}