package vn.minecraft.rPGCore.player;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.managers.FoodBuffManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.data.PlayerAttributeData;
import vn.minecraft.rPGCore.player.data.PlayerSkillData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RPGPlayer {
    private final Player bukkitPlayer;
    private RPGClass currentClass;

    private double currentResource;
    private double currentExp = 0;
    // 1. Khai báo biến lưu trữ thời điểm cuối cùng phát sinh giao tranh
    private long lastCombatTime = 0;
    private long balance = 0;

    // [MỚI]: Biến lưu trữ tổng số Năng lượng đang bị thế chấp bởi thú triệu hồi
    private double reservedResource = 0;

    // [MỚI]: Danh sách lưu trữ các Nguyên tố đã mở khóa (Dành riêng cho Mage)
    private java.util.List<String> unlockedElements = new java.util.ArrayList<>();

    private final PlayerAttributeData attributeData;
    private final PlayerSkillData skillData;

    public static class PlayerStats {
        public double minPhysicalDamage = 0.0; public double maxPhysicalDamage = 0.0;
        public double minMagicalDamage = 0.0; public double maxMagicalDamage = 0.0;
        public double maxHealth = 0.0; public double hpMultiplier = 1.0;

        public double armor = 0.0; public double armorMultiplier = 1.0;
        public double magicResistance = 0.0; public double magicResistMultiplier = 1.0;

        public double critRate = 5.0; public double critDamage = 0.0;
        public double armorPen = 0.0; public double magicArmorPen = 0.0;
        public double lifeSteal = 0.0; public double magicLifeSteal = 0.0;

        public double dodgeRate = 0.0; public double blockRate = 0.0;
        public double regenRate = 0.0; public double attackSpeed = 0.0;
        public double cooldownReduction = 0.0; public double moveSpeed = 0.0;
        public double effectResistance = 0.0;

        public double expBonus = 0.0; public double manaRegen = 0.0; public double dropRate = 0.0;

        // ==========================================
        // [TRINKET]: KHAI BÁO CÁC CHỈ SỐ ĐỘC QUYỀN
        // ==========================================
        public double trueDamageRate = 0.0;     // Sát thương chuẩn
        public double bossDamageBonus = 0.0;    // Sát thương lên Boss
        public double thornsDamage = 0.0;       // Phản sát thương
        public double periodicShield = 0.0;     // Hộ Thể Tuần Hoàn
        public double manaCostReduction = 0.0;  // Giảm tiêu hao NL
        public double staticManaRegen = 0.0;    // Hồi Năng lượng Tĩnh

        public Map<String, Integer> setCounts = new HashMap<>();
    }

    private PlayerStats currentStats = new PlayerStats();

    public RPGPlayer(Player player, RPGClass defaultClass) {
        this.bukkitPlayer = player;
        this.currentClass = defaultClass;
        this.currentResource = defaultClass.getMaxResource();
        this.attributeData = new PlayerAttributeData(this);
        this.skillData = new PlayerSkillData();
        // ---> THÊM ĐOẠN CODE NÀY VÀO ĐÂY (TRƯỚC DẤU ĐÓNG NGOẶC CỦA HÀM) <---
        // ==========================================
        // [TÂN THỦ]: KHỞI TẠO CẤP ĐỘ VÀ KỸ NĂNG CƠ BẢN
        // ==========================================
        if (this.bukkitPlayer.getLevel() == 0) {
            this.bukkitPlayer.setLevel(1);
            this.skillData.setSkillPoints(1); // Cấp sẵn 1 SP để học chiêu đầu tiên
        }
    }

    public PlayerAttributeData getAttributeData() { return attributeData; }
    public PlayerSkillData getSkillData() { return skillData; }
    public RPGClass getCurrentClass() { return currentClass; }
    public Player getBukkitPlayer() { return bukkitPlayer; }
    public double getCurrentResource() { return currentResource; }
    public double getCurrentHealth() { return this.bukkitPlayer.getHealth(); }
    public double getCurrentExp() { return currentExp; }
    public void setCurrentExp(double exp) { this.currentExp = exp; }
    public PlayerStats getStats() { return currentStats; }

    // ==========================================
    // [MỚI]: GETTER/SETTER CHO CẤP ĐỘ (LEVEL)
    // ==========================================
    public int getLevel() { return this.bukkitPlayer.getLevel(); }
    public void setLevel(int level) { this.bukkitPlayer.setLevel(level); }

    // 2. Hàm KÍCH HOẠT (Reset) đồng hồ giao tranh
    // Bất cứ khi nào người chơi đánh/bị đánh, ta sẽ gọi hàm này.
    public void setInCombat() {
        this.lastCombatTime = System.currentTimeMillis();
    }
    // 3. Hàm KIỂM TRA trạng thái giao tranh
    // Trả về true nếu chưa qua 10 giây kể từ lần cuối đánh nhau.
    public boolean isInCombat() {
        return (System.currentTimeMillis() - this.lastCombatTime) <= 10000L;
    }
    // 4. Hàm ÉP THOÁT giao tranh (Tùy chọn cực kỳ hữu ích)
    // Dùng khi Sát thủ dùng Bom Khói muốn tàng hình và xóa aggro ngay lập tức.
    public void removeCombat() {
        this.lastCombatTime = 0;
    }

    public long getBalance() { return this.balance; }
    public void setBalance(long amount) { this.balance = Math.max(0, amount); }
    public void addBalance(long amount) { if (amount > 0) this.balance += amount; }
    public boolean withdraw(long amount) {
        if (amount > 0 && this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    // ==========================================
    // TÍNH TOÁN TỔNG TẤT CẢ CHỈ SỐ
    // ==========================================
    public Map<String, Double> getTotalStatsMap() {
        Map<String, Double> map = new java.util.LinkedHashMap<>();
        UUID uuid = bukkitPlayer.getUniqueId();
        FoodBuffManager foodManager = vn.minecraft.rPGCore.RPGCore.getInstance().getFoodBuffManager();

        // [THÊM 2 MANAGER CỦA DƯỢC SƯ]
        vn.minecraft.rPGCore.managers.AlchemistBuffManager potionManager = vn.minecraft.rPGCore.RPGCore.getInstance().getAlchemistBuffManager();
        vn.minecraft.rPGCore.managers.ToxicityManager toxManager = vn.minecraft.rPGCore.RPGCore.getInstance().getToxicityManager();

        boolean isArcher = currentClass != null && currentClass.getId().equalsIgnoreCase("archer");
        boolean isMage = currentClass != null && currentClass.getId().equalsIgnoreCase("mage");

        int str = attributeData.getAttribute("STR");
        int intel = attributeData.getAttribute("INT");
        int vit = attributeData.getAttribute("VIT");
        int agi = attributeData.getAttribute("AGI");
        int luk = attributeData.getAttribute("LUK");

        // ==========================================
        // 1. SÁT THƯƠNG
        // ==========================================
        double strMultiplier = 1.0 + (str * 0.005);
        double intMultiplier = 1.0 + (intel * 0.005);
        double foodMultiplier = 1.0 + (foodManager.getBonus(uuid, FoodBuffManager.StatType.DMG) / 100.0);

        // [THÊM MULTIPLIER TỪ THUỐC VÀ TRÚNG ĐỘC MANA]
        double potionDmgMultiplier = 1.0 + (potionManager.getBonus(uuid, "Sát thương (%)") / 100.0);
        double manaSicknessMult = Math.max(0.0, 1.0 - toxManager.getManaSicknessDebuff(uuid));

        boolean isMagicalWeapon = currentStats.maxMagicalDamage > 0;
        boolean isPhysicalWeapon = currentStats.maxPhysicalDamage > 0;

        if (isPhysicalWeapon || !isMagicalWeapon) {
            double minP = currentStats.minPhysicalDamage > 0 ? currentStats.minPhysicalDamage : 5.0;
            double maxP = currentStats.maxPhysicalDamage > 0 ? currentStats.maxPhysicalDamage : 10.0;

            map.put("Sát thương vật lý (Nhỏ nhất)", minP * strMultiplier * foodMultiplier * potionDmgMultiplier);
            map.put("Sát thương vật lý (Lớn nhất)", maxP * strMultiplier * foodMultiplier * potionDmgMultiplier);
        }

        if (isMagicalWeapon) {
            double minM = currentStats.minMagicalDamage > 0 ? currentStats.minMagicalDamage : 5.0;
            double maxM = currentStats.maxMagicalDamage > 0 ? currentStats.maxMagicalDamage : 10.0;

            map.put("Sát thương phép thuật (Nhỏ nhất)", minM * intMultiplier * foodMultiplier * potionDmgMultiplier * manaSicknessMult);
            map.put("Sát thương phép thuật (Lớn nhất)", maxM * intMultiplier * foodMultiplier * potionDmgMultiplier * manaSicknessMult);
        }

        // 2. PHÒNG THỦ & MÁU
        map.put("Máu tối đa", getTotalMaxHealth());
        map.put("Giáp", currentStats.armor * (currentStats.armorMultiplier + (potionManager.getBonus(uuid, "Tỉ lệ Giáp") / 100.0)));
        map.put("Kháng phép", currentStats.magicResistance * (currentStats.magicResistMultiplier + (potionManager.getBonus(uuid, "Tỉ lệ Kháng phép") / 100.0)));

        // 3. BẠO KÍCH
        double critRate = currentStats.critRate + (luk * 0.2) + foodManager.getBonus(uuid, FoodBuffManager.StatType.CRIT_RATE) + potionManager.getBonus(uuid, "Tỉ lệ chí mạng");
        double critDamage = currentStats.critDamage;
        map.put("Tỉ lệ chí mạng", critRate);
        map.put("Sát thương chí mạng", critDamage);

        // 4. XUYÊN GIÁP & XUYÊN PHÉP
        double armorPen = currentStats.armorPen + foodManager.getBonus(uuid, FoodBuffManager.StatType.ARMOR_PEN) + potionManager.getBonus(uuid, "Tỉ lệ xuyên giáp");
        map.put("Tỉ lệ xuyên giáp", armorPen);

        double magicArmorPen = currentStats.magicArmorPen + potionManager.getBonus(uuid, "Tỉ lệ xuyên kháng phép");
        map.put("Tỉ lệ xuyên kháng phép", magicArmorPen);

        // 5. CHỈ SỐ SINH TỒN
        double dodgeRate = currentStats.dodgeRate + (agi * 0.1) + potionManager.getBonus(uuid, "Tỉ lệ né tránh");
        map.put("Tỉ lệ né tránh", dodgeRate);

        double lifeSteal = currentStats.lifeSteal + foodManager.getBonus(uuid, FoodBuffManager.StatType.LIFE_STEAL) + potionManager.getBonus(uuid, "Tỉ lệ hút máu");
        map.put("Tỉ lệ hút máu", lifeSteal);
        map.put("Tỉ lệ hút máu phép", currentStats.magicLifeSteal);

        double blockRate = currentStats.blockRate + (str * 0.1) + potionManager.getBonus(uuid, "Tỉ lệ đỡ đòn");
        map.put("Tỉ lệ đỡ đòn", blockRate);

        double regenRate = currentStats.regenRate + (vit * 0.1) + foodManager.getBonus(uuid, FoodBuffManager.StatType.REGEN) + potionManager.getBonus(uuid, "Tỉ lệ hồi phục");
        map.put("Tỉ lệ hồi phục", regenRate);
        map.put("Kháng hiệu ứng", currentStats.effectResistance + potionManager.getBonus(uuid, "Kháng hiệu ứng"));

        // 6. TỐC ĐỘ VÀ PHỤ TRỢ
        double atkSpeed = currentStats.attackSpeed + foodManager.getBonus(uuid, FoodBuffManager.StatType.ATK_SPEED) + potionManager.getBonus(uuid, "Tốc độ đánh");
        map.put("Tốc độ đánh", atkSpeed);

        double cdr = currentStats.cooldownReduction + (intel * 0.1) + foodManager.getBonus(uuid, FoodBuffManager.StatType.CDR) + potionManager.getBonus(uuid, "Giảm hồi chiêu");
        map.put("Giảm hồi chiêu", cdr);

        double moveSpeed = currentStats.moveSpeed + (agi * 0.5) + foodManager.getBonus(uuid, FoodBuffManager.StatType.MOVE_SPEED) + potionManager.getBonus(uuid, "Tốc độ di chuyển");
        if (bukkitPlayer.hasMetadata("Skill_MoveSpeed_Bonus")) {
            moveSpeed += bukkitPlayer.getMetadata("Skill_MoveSpeed_Bonus").get(0).asDouble();
        }
        map.put("Tốc độ di chuyển", moveSpeed);

        map.put("Nhận Kinh nghiệm", currentStats.expBonus + potionManager.getBonus(uuid, "Nhận Kinh nghiệm"));
        map.put("Hồi Năng lượng", currentStats.manaRegen + potionManager.getBonus(uuid, "Hồi Năng lượng"));
        map.put("Tỉ lệ rớt đồ", currentStats.dropRate + (luk * 0.1) + foodManager.getBonus(uuid, FoodBuffManager.StatType.DROP_RATE));

        // ==========================================
        // [TRINKET]: ĐẨY CHỈ SỐ MỚI VÀO MAP XUẤT RA
        // ==========================================
        map.put("Sát thương chuẩn", currentStats.trueDamageRate);
        map.put("Sát thương lên Boss", currentStats.bossDamageBonus);
        map.put("Phản sát thương", currentStats.thornsDamage);
        map.put("Hộ Thể Tuần Hoàn", currentStats.periodicShield);
        map.put("Giảm tiêu hao Năng lượng", currentStats.manaCostReduction);
        map.put("Hồi Năng lượng cố định", currentStats.staticManaRegen);

        if (currentClass != null) {
            currentClass.applyPassiveBonusStats(this, map);
        }
        return map;
    }

    public void updateEquipmentStats() {
        this.currentStats = new PlayerStats();
        ItemStack mainHand = bukkitPlayer.getInventory().getItemInMainHand();

        ItemStack[] allEquips = getAllEquippedItems();

        String playerClassId = this.currentClass != null ? this.currentClass.getId() : "none";
        String playerClassName = this.currentClass != null ? ChatColor.stripColor(this.currentClass.getDisplayName()) : "None";

        for (ItemStack item : allEquips) {
            if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) continue;
            if (item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().startsWith("§8§m")) continue;

            List<String> lore = item.getItemMeta().getLore();
            boolean isWeapon = (item.equals(mainHand));
            boolean isValidClass = false;

            for (String line : lore) {
                String cleanLine = ChatColor.stripColor(line).trim();
                if (cleanLine.startsWith("▶ Thuộc class:")) {
                    String reqClass = cleanLine.replace("▶ Thuộc class:", "").trim();
                    if (reqClass.equalsIgnoreCase("Tất cả") || reqClass.equalsIgnoreCase(playerClassId) || reqClass.equalsIgnoreCase(playerClassName)) {
                        isValidClass = true;
                    }
                    break;
                }
            }
            if (!isValidClass) continue;

            for (String line : lore) {
                String clean = ChatColor.stripColor(line).trim();
                if (clean.isEmpty()) continue;

                if (!isWeapon && clean.startsWith("✦ Thuộc tính Dòng dõi (")) {
                    String lineage = clean.substring(clean.indexOf("(") + 1, clean.indexOf(")")).toLowerCase();
                    currentStats.setCounts.put(lineage, currentStats.setCounts.getOrDefault(lineage, 0) + 1);
                    continue;
                }
                if (clean.startsWith("- ") && clean.contains(":")) {
                    String[] parts = clean.substring(2).split(":");
                    if (parts.length == 2) {
                        String statName = parts[0].trim();
                        boolean isPercent = parts[1].contains("%");
                        String valStr = parts[1].split("\\(")[0].replaceAll("[+%★\\s]", "");

                        try {
                            if (valStr.contains("~")) {
                                String[] range = valStr.split("~");
                                if (statName.contains("vật lý")) {
                                    currentStats.minPhysicalDamage += Double.parseDouble(range[0]);
                                    currentStats.maxPhysicalDamage += Double.parseDouble(range[1]);
                                } else if (statName.contains("phép thuật")) {
                                    currentStats.minMagicalDamage += Double.parseDouble(range[0]);
                                    currentStats.maxMagicalDamage += Double.parseDouble(range[1]);
                                }
                            } else {
                                if (statName.equals("Độ bền") || statName.equals("Cơ hội bảo vệ")) continue;
                                double val = Double.parseDouble(valStr);

                                switch (statName) {
                                    case "Máu tối đa":
                                        if (isPercent) currentStats.hpMultiplier += (val / 100.0);
                                        else currentStats.maxHealth += val; break;
                                    case "Giáp": currentStats.armor += val; break;
                                    case "Tỉ lệ Giáp": case "Tổng Giáp": currentStats.armorMultiplier += (val / 100.0); break;
                                    case "Kháng phép": currentStats.magicResistance += val; break;
                                    case "Tỉ lệ Kháng phép": currentStats.magicResistMultiplier += (val / 100.0); break;
                                    case "Tỉ lệ chí mạng": currentStats.critRate += val; break;
                                    case "Sát thương chí mạng": currentStats.critDamage += val; break;
                                    case "Tỉ lệ xuyên giáp": case "Xuyên giáp": currentStats.armorPen += val; break;
                                    case "Tỉ lệ xuyên kháng phép": currentStats.magicArmorPen += val; break;
                                    case "Tỉ lệ hút máu": case "Hút máu": currentStats.lifeSteal += val; break;
                                    case "Tỉ lệ hút máu phép": currentStats.magicLifeSteal += val; break;
                                    case "Tỉ lệ né tránh": currentStats.dodgeRate += val; break;
                                    case "Tỉ lệ đỡ đòn": currentStats.blockRate += val; break;
                                    case "Tỉ lệ hồi phục": currentStats.regenRate += val; break;
                                    case "Tốc độ đánh": currentStats.attackSpeed += val; break;
                                    case "Giảm hồi chiêu": currentStats.cooldownReduction += val; break;
                                    case "Tốc độ di chuyển": case "Tốc độ chạy": currentStats.moveSpeed += val; break;
                                    case "Kháng hiệu ứng": currentStats.effectResistance += val; break;
                                    case "Nhận Kinh nghiệm": currentStats.expBonus += val; break;
                                    case "Hồi Năng lượng": currentStats.manaRegen += val; break;
                                    case "Tỉ lệ rớt đồ": currentStats.dropRate += val; break;
                                    // ==========================================
                                    // [TRINKET]: ĐỌC CHỈ SỐ MỚI TỪ LORE
                                    // ==========================================
                                    case "Sát thương chuẩn": currentStats.trueDamageRate += val; break;
                                    case "Sát thương lên Boss": currentStats.bossDamageBonus += val; break;
                                    case "Phản sát thương": currentStats.thornsDamage += val; break;
                                    case "Hộ Thể Tuần Hoàn": currentStats.periodicShield += val; break;
                                    case "Giảm tiêu hao Năng lượng": currentStats.manaCostReduction += val; break;
                                    case "Hồi Năng lượng cố định": currentStats.staticManaRegen += val; break;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        applySetBonuses();
        updateSetVisuals();
        updateMaxStats();
        updateMoveSpeed();
    }

    private void applySetBonuses() {
        int vampiric = currentStats.setCounts.getOrDefault("vampiric", 0);
        int hunter = currentStats.setCounts.getOrDefault("hunter", 0);
        int giant = currentStats.setCounts.getOrDefault("giant", 0);
        int armored = currentStats.setCounts.getOrDefault("armored", 0);
        int swift = currentStats.setCounts.getOrDefault("swift", 0);
        int shattering = currentStats.setCounts.getOrDefault("shattering", 0);
        int experienced = currentStats.setCounts.getOrDefault("experienced", 0);
        int magical = currentStats.setCounts.getOrDefault("magical", 0);
        int scavenger = currentStats.setCounts.getOrDefault("scavenger", 0);

        if (vampiric >= 3) { currentStats.lifeSteal += 15.0; currentStats.magicLifeSteal += 15.0; }
        if (hunter >= 3) currentStats.critRate += 10.0;
        if (giant >= 3) currentStats.hpMultiplier += 0.20;
        if (armored >= 3) { currentStats.armorMultiplier += 0.20; currentStats.magicResistMultiplier += 0.20; }
        if (swift >= 3) currentStats.effectResistance += 15.0;
        if (shattering >= 3) { currentStats.armorPen += 15.0; currentStats.magicArmorPen += 15.0; }
        if (experienced >= 3) currentStats.expBonus += 20.0;
        if (magical >= 3) currentStats.manaRegen += 25.0;
        if (scavenger >= 3) currentStats.dropRate += 25.0;
    }

    private void updateSetVisuals() {
        ItemStack[] allEquips = getAllEquippedItems();

        for (ItemStack item : allEquips) {
            if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) continue;

            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.getLore();

            // ==========================================
            // [MÀNG LỌC BẢO VỆ]: Phân biệt Trang bị vs Vật phẩm
            // ==========================================
            boolean isEquipment = false;
            for (String l : lore) {
                if (ChatColor.stripColor(l).trim().startsWith("▶ Thuộc class:")) {
                    isEquipment = true;
                    break;
                }
            }

            // Nếu không phải là Trang Bị/Vũ Khí -> THA MẠNG (Bỏ qua luôn)
            if (!isEquipment) continue;

            boolean updated = false;

            // Tự động ép cờ ẩn các thông số mặc định của Minecraft
            if (!meta.hasItemFlag(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)) {
                meta.addItemFlags(
                        org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                        org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS,
                        org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE
                );
                updated = true;
            }

            String currentLineage = null;
            for (String l : lore) {
                String clean = ChatColor.stripColor(l).trim();
                if (clean.startsWith("✦ Thuộc tính Dòng dõi (")) {
                    currentLineage = clean.substring(clean.indexOf("(") + 1, clean.indexOf(")")).toLowerCase();
                    break;
                }
            }

            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                String clean = ChatColor.stripColor(line).trim();
                if (clean.startsWith("[Set 3 mảnh]:")) {
                    if (currentLineage != null && currentStats.setCounts.getOrDefault(currentLineage, 0) >= 3) {
                        if (!line.startsWith("§a")) { lore.set(i, "§a" + clean); updated = true; }
                    } else {
                        if (!line.startsWith("§8")) { lore.set(i, "§8" + clean); updated = true; }
                    }
                } else if (clean.startsWith("[Set 4 mảnh]:")) {
                    if (currentLineage != null && currentStats.setCounts.getOrDefault(currentLineage, 0) >= 4) {
                        if (!line.startsWith("§a")) { lore.set(i, "§a" + clean); updated = true; }
                    } else {
                        if (!line.startsWith("§8")) { lore.set(i, "§8" + clean); updated = true; }
                    }
                }
            }

            if (updated) {
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
    }

    public void updateMaxStats() {
        // Hàm get bây giờ đã an toàn tuyệt đối
        double safeMaxHealth = getTotalMaxHealth();

        org.bukkit.attribute.AttributeInstance maxHpAttr = this.bukkitPlayer.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (maxHpAttr != null) maxHpAttr.setBaseValue(safeMaxHealth);

        // Vẫn phải ép máu hiện tại xuống để tránh lỗi vượt trần HP
        if (this.bukkitPlayer.getHealth() > safeMaxHealth) {
            this.bukkitPlayer.setHealth(safeMaxHealth);
        }

        this.bukkitPlayer.setHealthScale(20.0);
        this.bukkitPlayer.setHealthScaled(true);
    }

    public void updateMoveSpeed() {
        AttributeInstance moveSpeedAttr = this.bukkitPlayer.getAttribute(Attribute.MOVEMENT_SPEED);
        if (moveSpeedAttr != null) {
            double totalSpeedBonus = getTotalStatsMap().getOrDefault("Tốc độ di chuyển", 0.0);
            moveSpeedAttr.setBaseValue(0.1 * (1.0 + (totalSpeedBonus / 100.0)));
        }
    }

    public double getTotalMaxHealth() {
        double baseHp = this.currentClass != null ? this.currentClass.getMaxHealth() : 20.0;
        double levelBonus = this.bukkitPlayer.getLevel() * 50.0;
        double vitBonus = this.attributeData.getAttribute("VIT") * 25.0;

        double equipHp = currentStats != null ? (currentStats.maxHealth * currentStats.hpMultiplier) : 0.0;
        double foodMaxHp = vn.minecraft.rPGCore.RPGCore.getInstance().getFoodBuffManager().getBonus(this.bukkitPlayer.getUniqueId(), vn.minecraft.rPGCore.managers.FoodBuffManager.StatType.MAX_HP);

        // [LẤY THÊM MÁU TỪ BÌNH THUỐC]
        double potionFlatHp = vn.minecraft.rPGCore.RPGCore.getInstance().getAlchemistBuffManager().getBonus(this.bukkitPlayer.getUniqueId(), "Máu tối đa");

        double totalMaxHp = baseHp + levelBonus + vitBonus + equipHp + foodMaxHp + potionFlatHp;
        return Math.max(1.0, totalMaxHp);
    }

    public double getRequiredExp() {
        int level = this.bukkitPlayer.getLevel();

        // 1. ĐƯỜNG CONG CƠ SỞ (Tăng trưởng mượt mà, không bị vọt lố)
        double baseExp = 300 + (level * 150) + (Math.pow(level, 3.0) * 1.5);

        // 2. HỆ SỐ NHÂN THEO GIAI ĐOẠN (Ép Hardcore theo Design của bác)
        double stageMultiplier = 1.0; // Lv 1 - 15

        if (level >= 16 && level < 22) {
            stageMultiplier = 1.5;     // Hơi chững lại (1-2 tuần)
        } else if (level >= 22 && level < 32) {
            stageMultiplier = 3.0;     // Bắt đầu khó (> 2 tuần)
        } else if (level >= 32 && level < 50) {
            stageMultiplier = 6.0;     // Cao (1-2 tháng)
        } else if (level >= 50 && level < 75) {
            stageMultiplier = 15.0;    // Cày cuốc (3 tháng)
        } else if (level >= 75 && level < 101) {
            stageMultiplier = 30.0;    // Dài hạn (6 tháng)
        } else if (level >= 101 && level < 150) {
            stageMultiplier = 50.0;    // Rất dài hạn (1 năm)
        } else if (level >= 150) {
            stageMultiplier = 80.0;    // Tối thượng (2 năm)
        }

        // Ví dụ Cấp 180: Cần khoảng ~696 Triệu EXP để lên cấp
        return baseExp * stageMultiplier;
    }
    public void addExp(double amount) {
        this.currentExp += amount;
        while (this.currentExp >= getRequiredExp()) {
            this.currentExp -= getRequiredExp();
            int newLevel = this.bukkitPlayer.getLevel() + 1;
            this.bukkitPlayer.setLevel(newLevel);
            // ==========================================
            // [ĐÃ CHỈNH SỬA] Tăng điểm cộng AP từ 3 lên 4
            // ==========================================
            this.attributeData.addUnspentAttrPoints(4);
            this.skillData.addSkillPoints(1);
            updateMaxStats();
            this.bukkitPlayer.sendMessage("§b§lLEVEL UP! §fBạn đã đạt cấp độ §e" + newLevel);
            this.bukkitPlayer.playSound(this.bukkitPlayer.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        updateExpBar();
    }
    public void updateExpBar() {
        float progress = (float) (this.currentExp / getRequiredExp());
        this.bukkitPlayer.setExp(Math.min(0.999f, progress));
    }
    public void loseDeathExp(double percentage) {
        if (this.currentExp <= 0) return;
        double lostAmount = this.currentExp * percentage;
        this.currentExp = Math.max(0, this.currentExp - lostAmount);
        updateExpBar();
        this.bukkitPlayer.sendMessage("§c§lBẠN ĐÃ TỬ VONG!");
        this.bukkitPlayer.sendMessage("§cHình phạt cấp độ > 20: Mất §e" + (int)lostAmount + " EXP §c(15% tiến độ).");
    }
    public void heal(double amount) {
        if (!this.bukkitPlayer.isDead()) {
            // [KIỂM TRA CHẶN HỒI PHỤC]
            if (StatusEffectManager.isHealingBlocked(this.bukkitPlayer)) {
                // Gửi Action Bar để tránh spam Chat
                this.bukkitPlayer.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§cBạn đang bị vết thương sâu, không thể hồi phục!"));
                return;
            }

            AttributeInstance maxAttr = this.bukkitPlayer.getAttribute(Attribute.MAX_HEALTH);
            if (maxAttr == null) return;
            double newHp = Math.min(this.bukkitPlayer.getHealth() + amount, maxAttr.getValue());
            this.bukkitPlayer.setHealth(newHp);
        }
    }
    // ==========================================
    // 1. HỆ THỐNG ĐỔI CLASS (Chỉ gọi khi người chơi BẤM CHỌN CLASS ở Menu GUI)
    // ==========================================
    public void changeClass(RPGClass newClass) {
        if (newClass == null) return; // Chặn đứng ngay nếu dữ liệu Class truyền vào bị hỏng

        try {
            if (this.currentClass != null && !this.currentClass.getId().equalsIgnoreCase(newClass.getId())) {

                // [MỚI]: Nếu đang là Summoner mà đổi class khác -> Tiêu diệt toàn bộ thú!
                if (this.currentClass instanceof vn.minecraft.rPGCore.classes.impl.SummonerClass) {
                    if (this.bukkitPlayer != null) {
                        ((vn.minecraft.rPGCore.classes.impl.SummonerClass) this.currentClass).clearAllPets(this.bukkitPlayer);
                    }
                }

                // Dọn dẹp tàn dư của Class cũ (Kiểm tra an toàn dữ liệu)
                if (this.skillData != null) {
                    this.skillData.clearAllSkills();
                    this.skillData.clearAllBinds();
                }

                // Backup SP và bắn thông báo
                if (this.bukkitPlayer != null && this.bukkitPlayer.isOnline()) {
                    int currentLevel = this.bukkitPlayer.getLevel();
                    if (this.skillData != null) this.skillData.setSkillPoints(currentLevel);

                    this.bukkitPlayer.sendMessage("§a§l[!] §fBạn đã chuyển sang hệ phái: §e" + newClass.getDisplayName());
                    this.bukkitPlayer.sendMessage("§b⟳ Toàn bộ Kỹ năng và Phím tắt đã được reset! Hoàn trả §e" + currentLevel + " SP§b.");
                }
            }

            // ==========================================
            // [CƠ CHẾ MAGE]: ROLL NGUYÊN TỐ LẦN ĐẦU TIÊN
            // ==========================================
            if (newClass.getId().equalsIgnoreCase("mage")) {
                if (this.unlockedElements == null) {
                    this.unlockedElements = new java.util.ArrayList<>();
                }

                if (this.unlockedElements.isEmpty()) {
                    String[] availableElements = {"FIRE", "WATER", "WIND", "NATURE", "LIGHT", "DARK"};
                    java.util.Random rand = new java.util.Random();
                    int roll = rand.nextInt(100);
                    int amountToRoll;

                    if (roll < 1) amountToRoll = 6;
                    else if (roll < 3) amountToRoll = 5;
                    else if (roll < 8) amountToRoll = 4;
                    else if (roll < 18) amountToRoll = 3;
                    else if (roll < 50) amountToRoll = 2;
                    else amountToRoll = 1;

                    java.util.List<String> elementList = java.util.Arrays.asList(availableElements);
                    java.util.Collections.shuffle(elementList);
                    for (int i = 0; i < amountToRoll; i++) {
                        this.unlockedElements.add(elementList.get(i));
                    }

                    if (this.bukkitPlayer != null && this.bukkitPlayer.isOnline()) {
                        if (amountToRoll >= 4) {
                            org.bukkit.Bukkit.broadcastMessage("§e§l[Máy Chủ] §fNgười chơi §a" + this.bukkitPlayer.getName() + " §fvừa thức tỉnh §d" + amountToRoll + " Nguyên Tố §fma thuật!");
                            this.bukkitPlayer.playSound(this.bukkitPlayer.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        } else {
                            this.bukkitPlayer.playSound(this.bukkitPlayer.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
                        }
                        this.bukkitPlayer.sendMessage("§d§l[Thức Tỉnh Nguyên Tố] §fMa lực của bạn cộng hưởng với: §a" + String.join(", ", this.unlockedElements));
                    }
                }
            }

            // Gọi hàm Gán Class
            setClass(newClass);

        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger().severe("[RPGCore] Loi nghiem trong khi doi Class cho nguoi choi " + (this.bukkitPlayer != null ? this.bukkitPlayer.getName() : "Unknown"));
            e.printStackTrace();
            if (this.bukkitPlayer != null && this.bukkitPlayer.isOnline()) {
                this.bukkitPlayer.sendMessage("§c§l[!] §cĐã xảy ra lỗi hệ thống khi chuyển phái. Vui lòng báo cáo cho Admin!");
            }
        }
    }

    // ==========================================
    // 2. HỆ THỐNG GÁN CLASS (Chạy im lặng khi vào game / Load Data)
    // ==========================================
    public void setClass(RPGClass newClass) {
        if (newClass == null) return;

        try {
            // [QUAN TRỌNG]: Quét dọn sạch sẽ buff/metadata của Class cũ trước khi áp dụng
            cleanUpClassData();

            this.currentClass = newClass;
            this.currentResource = getTotalMaxResource();
            updateMaxStats();

            if (this.bukkitPlayer != null && this.bukkitPlayer.isOnline() && !this.bukkitPlayer.isDead()) {
                double targetHealth = getTotalMaxHealth();
                // [CHỐNG CRASH BUKKIT]: Bukkit sẽ ném IllegalArgumentException nếu setHealth cao hơn Max Health
                org.bukkit.attribute.AttributeInstance maxHpAttr = this.bukkitPlayer.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                if (maxHpAttr != null) {
                    targetHealth = Math.min(targetHealth, maxHpAttr.getValue());
                }
                this.bukkitPlayer.setHealth(Math.max(0.1, targetHealth));
            }
        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger().severe("[RPGCore] Khong the ap dung du lieu Class moi!");
            e.printStackTrace();
        }
    }

    public void setCurrentResource(double currentResource) {
        try {
            // Ép giới hạn an toàn
            this.currentResource = Math.max(0, Math.min(currentResource, getUsableMaxResource()));

            // [FIX BUG UI]: Ép BossBar cập nhật với điều kiện chống Null Pointer toàn diện
            if (this.bukkitPlayer != null && this.bukkitPlayer.isOnline()) {
                vn.minecraft.rPGCore.RPGCore plugin = vn.minecraft.rPGCore.RPGCore.getInstance();
                if (plugin != null && plugin.getBossBarManager() != null) {
                    plugin.getBossBarManager().updateBars(this.bukkitPlayer, this);
                }
            }
        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger().warning("[RPGCore] Loi cap nhat tai nguyen (Mana/No) tren BossBar.");
            e.printStackTrace();
        }
    }

    // ==========================================
    // [DỌN DẸP LỖI CHUYỂN PHÁI]: XÓA METADATA & TASK RÁC
    // ==========================================
    public void cleanUpClassData() {
        if (this.bukkitPlayer == null || !this.bukkitPlayer.isOnline()) return;

        org.bukkit.plugin.Plugin plugin = vn.minecraft.rPGCore.RPGCore.getInstance();

        // [FIX BUG TÀNG HÌNH]: Ép hiện hình lập tức với toàn server trước khi xóa tàn dư
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showPlayer(plugin, this.bukkitPlayer);
        }

        // Danh sách các Key Meta "tồn đọng" dễ gây bug
        String[] keysToClear = {
                // Nhóm Assassin (Thêm key Tàng hình vào để Task của kỹ năng tự động chết)
                "Assassin_Stealth", "Assassin_Smoke_Bomb_Active",
                "Assassin_Bloodlust_Value", "Active_Weapon_Enhancement", "Ghost_L_Blocker",
                // Nhóm Mage
                "Mage_Domain_Channeling", "Mage_Domain_Phase", "Light_Blink_Combo",
                // Nhóm Summoner
                "Resonance_Stats", "RPG_Absorption_Shield", "RPG_Absorption_Shield_Max",
                "Summoner_Pet",
                // Nhóm bard
                "BARD_TONE_VISUALIZER", "BARD_AUTO_TONE_TASK", "BARD_TONES_LIST", "BARD_TONE_COUNT",
                "PASSIVE_RUNNING_bard_maestro_journey", "BARD_PENDING_DIST"
        };

        // 1. Xóa Metadata
        for (String key : keysToClear) {
            if (this.bukkitPlayer.hasMetadata(key)) {
                this.bukkitPlayer.removeMetadata(key, plugin);
            }
        }

        // 2. Reset các hiệu ứng Vanilla (Xóa tàng hình, xóa chậm, xóa Jump Boost)
        for (org.bukkit.potion.PotionEffect effect : this.bukkitPlayer.getActivePotionEffects()) {
            this.bukkitPlayer.removePotionEffect(effect.getType());
        }

        // 3. Reset các chỉ số ngầm của Core
        vn.minecraft.rPGCore.managers.StatusEffectManager.removeAbsoluteRoot(this.bukkitPlayer);
        vn.minecraft.rPGCore.managers.StatusEffectManager.removeCustomHover(this.bukkitPlayer);

        // 4. Giải phóng hoàn toàn Năng lượng thế chấp
        this.reservedResource = 0;
        this.currentResource = 0;
    }

    // ==========================================
    // [ĐÃ CHỈNH SỬA] Tỷ lệ Resource: Mana = 1.0 INT, Rage = 0.5 STR, Khác = 1.0 STR
    // ==========================================
    public double getTotalMaxResource() {
        double baseRes = this.currentClass != null ? this.currentClass.getMaxResource() : 100.0;

        boolean isMana = this.currentClass != null && this.currentClass.getResourceType().name().equals("MANA");
        boolean isRage = this.currentClass != null && this.currentClass.getResourceType().name().equals("RAGE");

        double attrBonus;
        if (isMana) {
            attrBonus = this.attributeData.getAttribute("INT") * 1.0;
        } else if (isRage) {
            // [CƠ CHẾ WARRIOR]: Nộ khí chỉ nhận 0.5 mỗi điểm STR để tránh lạm phát
            attrBonus = this.attributeData.getAttribute("STR") * 0.5;
        } else {
            attrBonus = this.attributeData.getAttribute("STR") * 1.0;
        }
        // [MỚI]: Cộng thêm lượng Năng lượng bonus từ nội tại Class
        double bonusFromPassives = getTotalStatsMap().getOrDefault("Năng lượng tối đa", 0.0);

        return baseRes + attrBonus + bonusFromPassives;
    }

    // ==========================================
    // QUẢN LÝ NĂNG LƯỢNG THẾ CHẤP (MANA RESERVATION)
    // ==========================================
    public double getReservedResource() { return this.reservedResource; }

    public void addReservedResource(double amount) {
        this.reservedResource += amount;
        // Nếu Mana hiện tại đang cao hơn mức cho phép sau khi thế chấp thì ép nó xuống
        setCurrentResource(this.currentResource);
    }

    public void freeReservedResource(double amount) {
        this.reservedResource = Math.max(0, this.reservedResource - amount);
    }

    // Hàm lấy Max Mana thực tế được phép sử dụng
    public double getUsableMaxResource() {
        return Math.max(0, getTotalMaxResource() - this.reservedResource);
    }

    public java.util.List<String> getUnlockedElements() { return unlockedElements; }
    public void setUnlockedElements(java.util.List<String> elements) { this.unlockedElements = elements; }
    public void addElement(String element) {
        if (!this.unlockedElements.contains(element.toUpperCase())) {
            this.unlockedElements.add(element.toUpperCase());
        }
    }
    public boolean hasElement(String element) {
        return this.unlockedElements.contains(element.toUpperCase());
    }
    // ==========================================
    // [TRINKET]: MẢNG LƯU TRỮ 4 Ô TRANG SỨC
    // ==========================================
    private ItemStack[] trinkets = new ItemStack[4];

    public ItemStack[] getTrinkets() { return trinkets; }
    public ItemStack getTrinket(int index) { return trinkets[index]; }
    public void setTrinket(int index, ItemStack item) {
        this.trinkets[index] = item;
        updateEquipmentStats(); // Tự động cập nhật chỉ số khi đeo/tháo trang sức
    }
    // ==========================================
    // HÀM PHỤ: LẤY TOÀN BỘ TRANG BỊ ĐANG ĐEO (GIÁP + VŨ KHÍ + TRANG SỨC)
    // ==========================================
    private ItemStack[] getAllEquippedItems() {
        ItemStack[] armor = bukkitPlayer.getInventory().getArmorContents();
        ItemStack mainHand = bukkitPlayer.getInventory().getItemInMainHand();

        ItemStack[] allEquips = new ItemStack[9];
        System.arraycopy(armor, 0, allEquips, 0, 4);
        allEquips[4] = mainHand;
        System.arraycopy(this.trinkets, 0, allEquips, 5, 4);

        return allEquips;
    }
    // ==========================================
    // KIỂM TRA SỐ LƯỢNG MẢNH TRANG BỊ CỦA SET BỘ
    // ==========================================
    public boolean hasSetBonus(String lineage, int requiredPieces) {
        if (this.currentStats == null || this.currentStats.setCounts == null) return false;
        return this.currentStats.setCounts.getOrDefault(lineage.toLowerCase(), 0) >= requiredPieces;
    }
}