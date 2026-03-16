package vn.minecraft.rPGCore.player;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.managers.FoodBuffManager;
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
    private long lastCombatTime = 0;
    private long balance = 0;

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
        public Map<String, Integer> setCounts = new HashMap<>();
    }

    private PlayerStats currentStats = new PlayerStats();

    public RPGPlayer(Player player, RPGClass defaultClass) {
        this.bukkitPlayer = player;
        this.currentClass = defaultClass;
        this.currentResource = defaultClass.getMaxResource();
        this.attributeData = new PlayerAttributeData(this);
        this.skillData = new PlayerSkillData();
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

    public void setInCombat() { this.lastCombatTime = System.currentTimeMillis(); }
    public boolean isInCombat() { return (System.currentTimeMillis() - this.lastCombatTime) <= 10000; }

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

        boolean isArcher = currentClass != null && currentClass.getId().equalsIgnoreCase("archer");
        boolean isMage = currentClass != null && currentClass.getId().equalsIgnoreCase("mage");

        int str = attributeData.getAttribute("STR");
        int intel = attributeData.getAttribute("INT");
        int vit = attributeData.getAttribute("VIT");
        int agi = attributeData.getAttribute("AGI");
        int luk = attributeData.getAttribute("LUK");

        // ==========================================
        // 1. SÁT THƯƠNG [ĐÃ CHỈNH SỬA SCALE THEO %]
        // Mỗi điểm STR = +0.5% Sát thương vật lý (Dmg Multiplier)
        // Mỗi điểm INT = +0.5% Sát thương phép thuật (Dmg Multiplier)
        // ==========================================
        double strMultiplier = 1.0 + (str * 0.005);
        double intMultiplier = 1.0 + (intel * 0.005);
        double foodMultiplier = 1.0 + (foodManager.getBonus(uuid, FoodBuffManager.StatType.DMG) / 100.0);

        boolean isMagicalWeapon = currentStats.maxMagicalDamage > 0;
        boolean isPhysicalWeapon = currentStats.maxPhysicalDamage > 0;

        if (isPhysicalWeapon || !isMagicalWeapon) {
            double minP = currentStats.minPhysicalDamage > 0 ? currentStats.minPhysicalDamage : 5.0;
            double maxP = currentStats.maxPhysicalDamage > 0 ? currentStats.maxPhysicalDamage : 10.0;

            map.put("Sát thương vật lý (Nhỏ nhất)", minP * strMultiplier * foodMultiplier);
            map.put("Sát thương vật lý (Lớn nhất)", maxP * strMultiplier * foodMultiplier);
        }

        if (isMagicalWeapon) {
            double minM = currentStats.minMagicalDamage > 0 ? currentStats.minMagicalDamage : 5.0;
            double maxM = currentStats.maxMagicalDamage > 0 ? currentStats.maxMagicalDamage : 10.0;

            map.put("Sát thương phép thuật (Nhỏ nhất)", minM * intMultiplier * foodMultiplier);
            map.put("Sát thương phép thuật (Lớn nhất)", maxM * intMultiplier * foodMultiplier);
        }

        // 2. PHÒNG THỦ & MÁU
        map.put("Máu tối đa", getTotalMaxHealth());
        map.put("Giáp", currentStats.armor * currentStats.armorMultiplier);
        map.put("Kháng phép", currentStats.magicResistance * currentStats.magicResistMultiplier);

        // 3. BẠO KÍCH (Tích hợp Nội tại Assassin: Dư 1% Crit = +2% Crit Damage)
        boolean isAssassin = currentClass != null && currentClass.getId().equalsIgnoreCase("assassin");

        double critRate = currentStats.critRate + (luk * 0.2) + foodManager.getBonus(uuid, FoodBuffManager.StatType.CRIT_RATE);
        if (isArcher) critRate += (skillData.getSkillLevel("archer_thien_xa") * 2.0);
        if (isAssassin) critRate += 15.0; // Nội tại 1: +15% Tỉ lệ chí mạng

        double critDamage = currentStats.critDamage;

        // Nội tại 2 Sát thủ: Chuyển hóa chí mạng dư thừa
        if (isAssassin && critRate > 100.0) {
            double excessCrit = critRate - 100.0;
            critDamage += (excessCrit * 2.0);
            critRate = 100.0; // Đưa về Cap 100%
        } else {
            critRate = Math.min(critRate, 100.0);
        }

        map.put("Tỉ lệ chí mạng", critRate);
        map.put("Sát thương chí mạng", critDamage);

        // 4. XUYÊN GIÁP & XUYÊN PHÉP
        double armorPen = currentStats.armorPen + foodManager.getBonus(uuid, FoodBuffManager.StatType.ARMOR_PEN);
        if (isArcher) armorPen += 25.0;
        map.put("Tỉ lệ xuyên giáp", Math.min(armorPen, isArcher ? 80.0 : 50.0));

        double magicArmorPen = currentStats.magicArmorPen;
        if (isMage) magicArmorPen += 25.0;
        map.put("Tỉ lệ xuyên kháng phép", Math.min(magicArmorPen, isMage ? 80.0 : 50.0));

        // 5. CHỈ SỐ SINH TỒN
        double dodgeRate = currentStats.dodgeRate + (agi * 0.1);
        map.put("Tỉ lệ né tránh", Math.min(dodgeRate, 30.0));

        double lifeSteal = currentStats.lifeSteal + foodManager.getBonus(uuid, FoodBuffManager.StatType.LIFE_STEAL);
        map.put("Tỉ lệ hút máu", Math.min(lifeSteal, 50.0));
        map.put("Tỉ lệ hút máu phép", Math.min(currentStats.magicLifeSteal, 50.0));

        double blockRate = currentStats.blockRate + (str * 0.1);
        map.put("Tỉ lệ đỡ đòn", Math.min(blockRate, 30.0));

        double regenRate = currentStats.regenRate + (vit * 0.1) + foodManager.getBonus(uuid, FoodBuffManager.StatType.REGEN);
        map.put("Tỉ lệ hồi phục", regenRate);
        map.put("Kháng hiệu ứng", Math.min(currentStats.effectResistance, 80.0));

        // 6. TỐC ĐỘ VÀ PHỤ TRỢ
        double atkSpeed = currentStats.attackSpeed + foodManager.getBonus(uuid, FoodBuffManager.StatType.ATK_SPEED);
        map.put("Tốc độ đánh", Math.min(atkSpeed, 100.0));

        double cdr = currentStats.cooldownReduction + (intel * 0.1) + foodManager.getBonus(uuid, FoodBuffManager.StatType.CDR);
        map.put("Giảm hồi chiêu", Math.min(cdr, 40.0));

        double moveSpeed = currentStats.moveSpeed + (agi * 0.5) + foodManager.getBonus(uuid, FoodBuffManager.StatType.MOVE_SPEED);

        // Đọc buff từ các kỹ năng tăng tốc (VD: Khinh Công Ám Ảnh)
        if (bukkitPlayer.hasMetadata("Skill_MoveSpeed_Bonus")) {
            moveSpeed += bukkitPlayer.getMetadata("Skill_MoveSpeed_Bonus").get(0).asDouble();
        }

        // GIỚI HẠN TỐC ĐỘ CHẠY (CAP) - Tránh bị scale quá mức
        moveSpeed = Math.min(moveSpeed, 400.0); // Ví dụ: Max +400% tốc độ
        map.put("Tốc độ di chuyển", moveSpeed);

        map.put("Nhận Kinh nghiệm", currentStats.expBonus);
        map.put("Hồi Năng lượng", currentStats.manaRegen);
        map.put("Tỉ lệ rớt đồ", currentStats.dropRate + (luk * 0.1) + foodManager.getBonus(uuid, FoodBuffManager.StatType.DROP_RATE));

        return map;
    }

    public void updateEquipmentStats() {
        this.currentStats = new PlayerStats();
        ItemStack[] armor = bukkitPlayer.getInventory().getArmorContents();
        ItemStack mainHand = bukkitPlayer.getInventory().getItemInMainHand();
        ItemStack[] allEquips = new ItemStack[5];
        System.arraycopy(armor, 0, allEquips, 0, 4);
        allEquips[4] = mainHand;

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
        ItemStack[] armor = bukkitPlayer.getInventory().getArmorContents();
        for (ItemStack item : armor) {
            if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) continue;
            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.getLore();
            boolean updated = false;

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
        double totalMaxHp = getTotalMaxHealth();
        AttributeInstance maxHpAttr = this.bukkitPlayer.getAttribute(Attribute.MAX_HEALTH);
        if (maxHpAttr != null) maxHpAttr.setBaseValue(totalMaxHp);
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
        double foodMaxHp = vn.minecraft.rPGCore.RPGCore.getInstance().getFoodBuffManager().getBonus(this.bukkitPlayer.getUniqueId(), FoodBuffManager.StatType.MAX_HP);
        return baseHp + levelBonus + vitBonus + equipHp + foodMaxHp;
    }

    public double getRequiredExp() {
        int level = this.bukkitPlayer.getLevel();
        return 500 + (level * 100) + Math.pow(level, 3) * level/4;
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
        this.bukkitPlayer.sendMessage("§c§l☠ BẠN ĐÃ TỬ VONG!");
        this.bukkitPlayer.sendMessage("§cHình phạt cấp độ > 20: Mất §e" + (int)lostAmount + " EXP §c(30% tiến độ).");
    }
    public void heal(double amount) {
        if (!this.bukkitPlayer.isDead()) {
            if (this.bukkitPlayer.hasMetadata("Assassin_AntiHeal")) {
                long expire = this.bukkitPlayer.getMetadata("Assassin_AntiHeal").get(0).asLong();
                if (System.currentTimeMillis() < expire) return;
            }
            AttributeInstance maxAttr = this.bukkitPlayer.getAttribute(Attribute.MAX_HEALTH);
            if (maxAttr == null) return;
            double newHp = Math.min(this.bukkitPlayer.getHealth() + amount, maxAttr.getValue());
            this.bukkitPlayer.setHealth(newHp);
        }
    }
    public void setClass(RPGClass newClass) {
        this.currentClass = newClass;
        this.currentResource = getTotalMaxResource();
        updateMaxStats();
        this.bukkitPlayer.setHealth(getTotalMaxHealth());
    }
    public void setCurrentResource(double currentResource) {
        this.currentResource = Math.max(0, Math.min(currentResource, getTotalMaxResource()));
    }

    // ==========================================
    // [ĐÃ CHỈNH SỬA] Tăng điểm Mana/Stamina từ 0.5 lên 1.0 cho mỗi STR / INT
    // ==========================================
    public double getTotalMaxResource() {
        double baseRes = this.currentClass != null ? this.currentClass.getMaxResource() : 100.0;
        boolean isMana = this.currentClass != null && this.currentClass.getResourceType().name().equals("MANA");
        double attrBonus = isMana ? (this.attributeData.getAttribute("INT") * 1.0) : (this.attributeData.getAttribute("STR") * 1.0);
        return baseRes + attrBonus;
    }
}