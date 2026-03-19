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

    // CHUYỂN CÁC BIẾN NÀY THÀNH STATIC ĐỂ DÙNG TRONG HÀM STATIC
    private static final Object[][] TIERS = {
            {"§7", "common", 1.0}, {"§a", "uncommon", 1.2}, {"§b", "rare", 1.5}, {"§5", "epic", 1.8},
            {"§d", "mythical", 2.2}, {"§6", "heroic", 2.8}, {"§c", "legendary", 3.5}, {"§e", "divine", 5.0}
    };

    // Trọng số rớt Tier: Common(5000), Uncommon(2500), Rare(1000), Epic(400), Mythical(150), Heroic(50), Legendary(10), Divine(5)
    private static final int[] TIER_WEIGHTS = { 5000, 2500, 1000, 400, 150, 50, 10, 5 };

    private static final Object[][] QUALITY_DB = {
            {new String[]{"Rusty", "Broken", "Ruined", "Chipped"}, 0.40, 0.70, 100},
            {new String[]{"Old", "Worn", "Dull", "Faded"}, 0.60, 0.85, 80},
            {new String[]{"Normal", "Standard", "Decent", "Common"}, 0.80, 1.00, 60},
            {new String[]{"Fine", "Polished", "Refined", "Sturdy"}, 0.90, 1.05, 40},
            {new String[]{"Reinforced", "Flawless", "Exceptional", "Immaculate"}, 0.95, 1.10, 20},
            {new String[]{"Masterwork", "Heroic", "Glorious", "Majestic"}, 1.00, 1.15, 10},
            {new String[]{"Legendary", "Exquisite", "Perfect", "Peerless"}, 1.05, 1.15, 5},
            {new String[]{"Divine", "Godly", "Ascended", "Transcendent"}, 1.10, 1.20, 2}
    };

    private static final List<String> VALID_CLASSES = Arrays.asList("warrior", "archer", "mage", "assassin", "summoner", "reaper", "guardian");
    private static final List<String> VALID_TIERS = Arrays.asList("common", "uncommon", "rare", "epic", "mythical", "heroic", "legendary", "divine");
    private static final List<String> ALL_LINEAGES = Arrays.asList("vampiric", "experienced", "hunter", "magical", "giant", "armored", "scavenger", "swift", "shattering");
    private static final List<String> WEAPON_LINEAGES = Arrays.asList("vampiric", "experienced", "hunter", "magical", "scavenger", "swift", "shattering");

    private static final Object[][] RANDOM_STATS_DB = {
            {"Tỉ lệ chí mạng", 100, 50}, {"Sát thương chí mạng", 0, 50}, {"Tỉ lệ xuyên giáp", 80, 30},
            {"Tỉ lệ xuyên kháng phép", 80, 30}, {"Tỉ lệ hút máu", 50, 15}, {"Tỉ lệ hút máu phép", 50, 15},
            {"Tỉ lệ đỡ đòn", 50, 60}, {"Tỉ lệ né tránh", 50, 60}, {"Tỉ lệ hồi phục", 0, 100},
            {"Tốc độ đánh", 100, 40}, {"Giảm hồi chiêu", 40, 10}, {"Tốc độ di chuyển", 0, 80},
            {"Kháng hiệu ứng", 80, 30}, {"Tỉ lệ Giáp", 0, 40}, {"Tỉ lệ Kháng phép", 0, 40},
            {"Nhận Kinh nghiệm", 0, 60}, {"Tỉ lệ rớt đồ", 0, 50}, {"Hồi Năng lượng", 0, 70}
    };

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        if (!player.hasPermission("rpgcore.admin")) return true;

        if (args.length < 3 || !args[0].equalsIgnoreCase("roll")) {
            player.sendMessage("§eCú pháp: /eq roll <weapon|armor> <level> [class] [tier] [lineage] [sub:X] [set:1|2|3|4] [num:X]");
            return true;
        }

        try {
            String type = args[1].toLowerCase();
            int level = Integer.parseInt(args[2]);

            String inputClass = null; int inputTier = -1; String inputLineage = null;
            int inputSubStats = -1; int inputArmorPiece = -1; int amount = 1;

            for (int i = 3; i < args.length; i++) {
                String arg = args[i].toLowerCase();
                if (arg.startsWith("sub:")) inputSubStats = Integer.parseInt(arg.substring(4));
                else if (arg.startsWith("set:")) inputArmorPiece = Integer.parseInt(arg.substring(4));
                else if (arg.startsWith("num:")) amount = Integer.parseInt(arg.substring(4));
                else if (VALID_CLASSES.contains(arg)) inputClass = arg;
                else if (VALID_TIERS.contains(arg)) inputTier = VALID_TIERS.indexOf(arg);
                else if (ALL_LINEAGES.contains(arg)) inputLineage = arg;
            }

            for (int n = 0; n < amount; n++) {
                // Gọi hàm tĩnh (Static) để tạo ra món đồ
                ItemStack item = createGachaItem(type, level, inputClass, inputTier, inputLineage, inputSubStats, inputArmorPiece);

                // Nhét vào túi của người gọi lệnh
                player.getInventory().addItem(item);
            }
            player.sendMessage("§aĐã roll thành công " + amount + " trang bị!");
        } catch (Exception e) {
            player.sendMessage("§cLỗi cú pháp! Vui lòng nhập đúng format.");
        }
        return true;
    }

    // =========================================================================
    // API CÔNG KHAI ĐỂ CÁC PLUGIN KHÁC GỌI MÀ KHÔNG BỊ ÉP NHÉT VÀO INVENTORY
    // =========================================================================
    public static ItemStack createGachaItem(String type, int level, String inputClass, int inputTier, String inputLineage, int inputSubStats, int inputArmorPiece) {
        Random rand = new Random();
        boolean isWeapon = type.equals("weapon");

        String rpgClass = inputClass != null ? inputClass : VALID_CLASSES.get(rand.nextInt(VALID_CLASSES.size()));
        // --- FIX: Random Tier theo Trọng số ---
        int tierIndex = inputTier;
        if (tierIndex == -1) {
            int totalWeight = 0;
            for (int w : TIER_WEIGHTS) totalWeight += w;
            int roll = rand.nextInt(totalWeight);
            int currentWeightRoll = 0;
            for (int i = 0; i < TIER_WEIGHTS.length; i++) {
                currentWeightRoll += TIER_WEIGHTS[i];
                if (roll < currentWeightRoll) {
                    tierIndex = i;
                    break;
                }
            }
            if (tierIndex == -1) tierIndex = 0; // Dự phòng
        }
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

        // Gacha Quality
        int totalQualityWeight = 0;
        for (Object[] q : QUALITY_DB) totalQualityWeight += (int) q[3];
        int qualityRoll = rand.nextInt(totalQualityWeight);
        int currentWeight = 0;
        Object[] chosenQuality = QUALITY_DB[0];
        for (Object[] q : QUALITY_DB) {
            currentWeight += (int) q[3];
            if (qualityRoll < currentWeight) { chosenQuality = q; break; }
        }

        String[] qualityNames = (String[]) chosenQuality[0];
        String qualityName = qualityNames[rand.nextInt(qualityNames.length)];
        double minVar = (double) chosenQuality[1];
        double maxVar = (double) chosenQuality[2];
        double qualityMod = minVar + (rand.nextDouble() * (maxVar - minVar));

        Material mat; String baseItemName;
        if (isWeapon) {
            Object[] weaponData = generateWeaponMaterial(rpgClass, tierIndex, rand);
            mat = (Material) weaponData[0]; baseItemName = (String) weaponData[1];
        } else {
            Object[] armorData = generateArmorMaterial(armorPiece, tierIndex);
            mat = (Material) armorData[0]; baseItemName = (String) armorData[1];
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(tierColor + "§l" + qualityName + " " + capitalize(lineage) + " " + baseItemName);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§8▶ Thuộc class: §f" + (isWeapon ? capitalize(rpgClass) : "Tất cả"));
        lore.add("§8▶ Yêu cầu cấp độ: " + level);
        lore.add("");

        lore.add(tierColor + "§l✦ Chỉ số chính:");
        int maxDurability;

        if (isWeapon) {
            double baseAtk = level * 4.5 * tierMulti * qualityMod;
            double minAtk = baseAtk * 0.75;
            double maxAtk = baseAtk * 1.25;
            boolean isMagicWeapon = rpgClass.equalsIgnoreCase("mage") || rpgClass.equalsIgnoreCase("summoner");
            String dmgType = isMagicWeapon ? "Sát thương phép thuật" : "Sát thương vật lý";
            lore.add("§7 - " + dmgType + ": §f" + String.format("%.1f", minAtk) + " §8~ §f" + String.format("%.1f", maxAtk));
            maxDurability = (int) (1500 * (tierMulti * 0.3 + qualityMod * 0.7));
        } else {
            double baseHp = level * 15 * tierMulti * qualityMod;
            double baseDef = level * 1.5 * tierMulti * qualityMod;
            double finalHp = baseHp * (0.85 + (rand.nextDouble() * 0.30));
            lore.add("§7 - Máu tối đa: §a+" + Math.round(finalHp));

            int armorType = rand.nextInt(3);
            if (armorType == 0) {
                double finalArmor = baseDef * (0.85 + (rand.nextDouble() * 0.30));
                lore.add("§7 - Giáp: §f+" + Math.round(finalArmor));
            } else if (armorType == 1) {
                double finalMR = baseDef * (0.85 + (rand.nextDouble() * 0.30));
                lore.add("§7 - Kháng phép: §b+" + Math.round(finalMR));
            } else {
                double finalArmor = (baseDef / 2.0) * (0.85 + (rand.nextDouble() * 0.30));
                double finalMR = (baseDef / 2.0) * (0.85 + (rand.nextDouble() * 0.30));
                lore.add("§7 - Giáp: §f+" + Math.round(finalArmor));
                lore.add("§7 - Kháng phép: §b+" + Math.round(finalMR));
            }
            maxDurability = (int) (2500 * (tierMulti * 0.3 + qualityMod * 0.7));
        }

        lore.add("§7 - Độ bền: §a" + maxDurability + "§8/§a" + maxDurability);
        lore.add("§7 - Cơ hội bảo vệ: §e3§8/§e3");
        lore.add("");

        double levelFactor = Math.min(level, 100) / 100.0;
        double tierFactor = (double) tierIndex / (TIERS.length - 1);
        double qualityFactor = (qualityMod - 0.4) / (1.2 - 0.4);
        double powerRating = (levelFactor * 0.45) + (tierFactor * 0.35) + (qualityFactor * 0.20);
        double statMulti = 1.0 + (14.0 * powerRating);

        lore.add(tierColor + "§l✦ Thuộc tính Dòng dõi (" + capitalize(lineage) + "):");
        List<String> excludedStats = applyLineageStats(lore, lineage.toLowerCase(), statMulti);
        lore.add("");

        if (subStats > 0) {
            lore.add("§d§l✦ Thuộc tính Phụ:");
            List<Integer> availableIndices = new ArrayList<>();
            for (int i = 0; i < RANDOM_STATS_DB.length; i++) {
                String statName = (String) RANDOM_STATS_DB[i][0];
                if (!excludedStats.contains(statName)) availableIndices.add(i);
            }

            List<Integer> selectedIndices = new ArrayList<>();
            int actualSubStats = Math.min(subStats, availableIndices.size());

            for (int i = 0; i < actualSubStats; i++) {
                int totalWeight = 0;
                for (int index : availableIndices) totalWeight += (int) RANDOM_STATS_DB[index][2];

                int roll = rand.nextInt(totalWeight);
                int currentWeightRoll = 0;
                int chosenIndex = -1;

                for (int j = 0; j < availableIndices.size(); j++) {
                    int index = availableIndices.get(j);
                    currentWeightRoll += (int) RANDOM_STATS_DB[index][2];
                    if (roll < currentWeightRoll) {
                        chosenIndex = index;
                        availableIndices.remove(j);
                        break;
                    }
                }
                if (chosenIndex != -1) selectedIndices.add(chosenIndex);
            }

            Collections.sort(selectedIndices);
            for (int index : selectedIndices) {
                String statName = (String) RANDOM_STATS_DB[index][0];
                double value = (0.5 + rand.nextDouble() * 1.25) * statMulti;
                lore.add("§7 - " + statName + ": §e+" + String.format("%.1f", value) + "%");
            }
            lore.add("");
        }

        if (!isWeapon) {
            applySetBonus(lore, lineage.toLowerCase());
            lore.add("");
        }

        meta.setUnbreakable(true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE, org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private static List<String> applyLineageStats(List<String> lore, String lineage, double statMulti) {
        List<String> excluded = new ArrayList<>();
        switch (lineage) {
            case "vampiric":
                lore.add("§c - Tỉ lệ hút máu: +" + String.format("%.1f", 1.5 * statMulti) + "%");
                lore.add("§c - Tỉ lệ hút máu phép: +" + String.format("%.1f", 1.5 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Tỉ lệ hút máu", "Tỉ lệ hút máu phép")); break;
            case "experienced":
                lore.add("§b - Nhận Kinh nghiệm: +" + String.format("%.1f", 2.5 * statMulti) + "%");
                lore.add("§b - Tỉ lệ rớt đồ: +" + String.format("%.1f", 1.5 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Nhận Kinh nghiệm", "Tỉ lệ rớt đồ")); break;
            case "hunter":
                lore.add("§e - Tỉ lệ chí mạng: +" + String.format("%.1f", 1.0 * statMulti) + "%");
                lore.add("§e - Sát thương chí mạng: +" + String.format("%.1f", 4.0 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Tỉ lệ chí mạng", "Sát thương chí mạng")); break;
            case "magical":
                lore.add("§9 - Hồi Năng lượng: +" + String.format("%.1f", 2.5 * statMulti) + "%");
                lore.add("§9 - Giảm hồi chiêu: +" + String.format("%.1f", 1.5 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Hồi Năng lượng", "Giảm hồi chiêu")); break;
            case "giant":
                lore.add("§a - Máu tối đa: +" + String.format("%.1f", 2.5 * statMulti) + "%");
                lore.add("§a - Tỉ lệ hồi phục: +" + String.format("%.1f", 1.5 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Máu tối đa", "Tỉ lệ hồi phục")); break;
            case "armored":
                lore.add("§6 - Tỉ lệ Giáp: +" + String.format("%.1f", 2.0 * statMulti) + "%");
                lore.add("§6 - Tỉ lệ Kháng phép: +" + String.format("%.1f", 2.0 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Tỉ lệ Giáp", "Tỉ lệ Kháng phép")); break;
            case "scavenger":
                lore.add("§e - Tỉ lệ rớt đồ: +" + String.format("%.1f", 2.0 * statMulti) + "%");
                lore.add("§e - Tốc độ di chuyển: +" + String.format("%.1f", 1.5 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Tỉ lệ rớt đồ", "Tốc độ di chuyển")); break;
            case "swift":
                lore.add("§f - Tốc độ di chuyển: +" + String.format("%.1f", 1.5 * statMulti) + "%");
                lore.add("§f - Kháng hiệu ứng: +" + String.format("%.1f", 1.5 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Tốc độ di chuyển", "Kháng hiệu ứng")); break;
            case "shattering":
                lore.add("§4 - Tỉ lệ xuyên giáp: +" + String.format("%.1f", 2.0 * statMulti) + "%");
                lore.add("§4 - Tỉ lệ xuyên kháng phép: +" + String.format("%.1f", 2.0 * statMulti) + "%");
                excluded.addAll(Arrays.asList("Tỉ lệ xuyên giáp", "Tỉ lệ xuyên kháng phép")); break;
        }
        return excluded;
    }

    private static void applySetBonus(List<String> lore, String lineage) {
        switch (lineage) {
            case "vampiric":
                lore.add("§8[Set 3 mảnh]: +15% Hút máu và Hút máu phép");
                lore.add("§8[Set 4 mảnh]: Hút máu dư chuyển thành Máu ảo (Tối đa 20% Max HP)"); break;
            case "experienced":
                lore.add("§8[Set 3 mảnh]: +20% Kinh nghiệm nhận được");
                lore.add("§8[Set 4 mảnh]: Khi nhặt đồ có 5% rơi ra thêm Xu ngẫu nhiên"); break;
            case "hunter":
                lore.add("§8[Set 3 mảnh]: +10% Tỉ lệ chí mạng");
                lore.add("§8[Set 4 mảnh]: Đòn bạo kích tăng 15% Tốc chạy trong 3 giây"); break;
            case "magical":
                lore.add("§8[Set 3 mảnh]: +25% Tốc độ hồi Năng lượng");
                lore.add("§8[Set 4 mảnh]: Kỹ năng tiêu hao giảm 20% Năng lượng"); break;
            case "giant":
                lore.add("§8[Set 3 mảnh]: +20% Máu tối đa");
                lore.add("§8[Set 4 mảnh]: Đứng yên 3s sẽ tự động hồi 2% Máu/s"); break;
            case "armored":
                lore.add("§8[Set 3 mảnh]: +20% Tỉ lệ Giáp và Kháng Phép");
                lore.add("§8[Set 4 mảnh]: Chặn 1 đòn sát thương chí mạng mỗi 30 giây"); break;
            case "scavenger":
                lore.add("§8[Set 3 mảnh]: +25% Tỉ lệ rớt đồ");
                lore.add("§8[Set 4 mảnh]: Có 10% tỉ lệ x2 số lượng vật phẩm nhặt được"); break;
            case "swift":
                lore.add("§8[Set 3 mảnh]: +15% Kháng hiệu ứng");
                lore.add("§8[Set 4 mảnh]: Miễn nhiễm hoàn toàn với Trói Chân và Làm Chậm"); break;
            case "shattering":
                lore.add("§8[Set 3 mảnh]: +15% Xuyên Giáp và Xuyên Kháng Phép");
                lore.add("§8[Set 4 mảnh]: Gây thêm 10% Sát thương chuẩn (Bỏ qua mọi phòng ngự)"); break;
        }
    }

    private static Object[] generateWeaponMaterial(String rpgClass, int tier, Random rand) {
        String[] swordPrefixes = {"WOODEN_", "STONE_", "COPPER_", "IRON_", "GOLDEN_", "DIAMOND_", "NETHERITE_", "NETHERITE_"};
        String prefix = swordPrefixes[tier];
        Material mat; String name;
        switch (rpgClass) {
            case "archer":
                boolean isBow = rand.nextBoolean();
                mat = isBow ? Material.BOW : Material.CROSSBOW;
                name = isBow ? "Bow" : "Crossbow"; break;
            case "mage":
                mat = rand.nextBoolean() ? Material.STICK : Material.BLAZE_ROD;
                name = "Staff"; break;
            case "summoner":
                mat = Material.LEAD; name = "Whip"; break;
            case "assassin":
                mat = Material.SHEARS; name = "Dagger"; break;
            case "reaper":
                mat = Material.valueOf(prefix + "HOE"); name = "Scythe"; break;
            default:
                mat = Material.valueOf(prefix + "SWORD"); name = "Sword"; break;
        }
        return new Object[]{mat, name};
    }

    private static Object[] generateArmorMaterial(int piece, int tier) {
        String[] matPrefixes = {"LEATHER_", "COPPER_", "CHAINMAIL_", "IRON_", "GOLDEN_", "DIAMOND_", "NETHERITE_", "NETHERITE_"};
        String prefix = matPrefixes[tier];
        Material mat; String name;
        switch (piece) {
            case 1: mat = Material.valueOf(prefix + "HELMET"); name = "Helmet"; break;
            case 3: mat = Material.valueOf(prefix + "LEGGINGS"); name = "Leggings"; break;
            case 4: mat = Material.valueOf(prefix + "BOOTS"); name = "Boots"; break;
            default: mat = Material.valueOf(prefix + "CHESTPLATE"); name = "Chestplate"; break;
        }
        return new Object[]{mat, name};
    }

    private static String capitalize(String str) { return str == null || str.isEmpty() ? str : str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase(); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) completions.addAll(Arrays.asList("weapon", "armor"));
        else if (args.length == 3) completions.addAll(Arrays.asList("1", "10", "50", "100"));
        else if (args.length >= 4) {
            completions.addAll(VALID_CLASSES); completions.addAll(VALID_TIERS); completions.addAll(ALL_LINEAGES);
        }
        return completions;
    }
}