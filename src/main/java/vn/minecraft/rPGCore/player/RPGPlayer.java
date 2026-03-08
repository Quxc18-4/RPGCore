package vn.minecraft.rPGCore.player;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.player.data.PlayerAttributeData;
import vn.minecraft.rPGCore.player.data.PlayerSkillData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RPGPlayer {
    private final Player bukkitPlayer;
    private RPGClass currentClass;

    // Core Stats
    private double currentResource;
    private double currentExp = 0;
    private long lastCombatTime = 0;

    // Các Mô-đun (Components)
    private final PlayerAttributeData attributeData;
    private final PlayerSkillData skillData;

    // --- HỆ THỐNG CHỈ SỐ TRANG BỊ ---
    public static class PlayerStats {
        public double minPhysicalDamage = 0.0; public double maxPhysicalDamage = 0.0;
        public double minMagicalDamage = 0.0; public double maxMagicalDamage = 0.0;
        public double maxHealth = 0.0; public double armor = 0.0;
        public double critRate = 5.0; public double critDamage = 50.0;
        public double armorPen = 0.0; public double lifeSteal = 0.0;
        public double dodgeRate = 0.0; public double blockRate = 0.0;
        public double regenRate = 0.0; public double attackSpeed = 0.0;
        public double cooldownReduction = 0.0; public double moveSpeed = 0.0;

        // THÊM 2 BIẾN NÀY ĐỂ LƯU HỆ SỐ NHÂN TỪ SET BONUS
        public double hpMultiplier = 1.0;
        public double armorMultiplier = 1.0;

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

    // --- CẬP NHẬT CHỈ SỐ TỪ TRANG BỊ VÀO NHÂN VẬT ---
    public void updateEquipmentStats() {
        this.currentStats = new PlayerStats();

        ItemStack[] armor = bukkitPlayer.getInventory().getArmorContents();
        ItemStack mainHand = bukkitPlayer.getInventory().getItemInMainHand();

        ItemStack[] allEquips = new ItemStack[5];
        System.arraycopy(armor, 0, allEquips, 0, 4);
        allEquips[4] = mainHand;

        String playerClassId = this.currentClass != null ? this.currentClass.getId() : "none";
        String playerClassName = this.currentClass != null ? org.bukkit.ChatColor.stripColor(this.currentClass.getDisplayName()) : "None";
        for (ItemStack item : allEquips) {
            if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) continue;

            // KIỂM TRA ĐỒ HỎNG BẰNG TÊN GẠCH NGANG (§8§m)
            if (item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().startsWith("§8§m")) {
                continue; // Đồ hỏng -> Không quét chỉ số, không quét set
            }

            List<String> lore = item.getItemMeta().getLore();
            boolean isWeapon = (item == mainHand);
            boolean isValidClass = false;

            // KIỂM TRA LỚP NHÂN VẬT (Class Lock)
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

            // QUÉT DATA
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
                        String valStr = parts[1].split("\\(")[0].replaceAll("[+%]", "").trim();
                        try {
                            if (valStr.contains("~")) {
                                String[] range = valStr.split("~");
                                if (statName.contains("vật lý")) {
                                    currentStats.minPhysicalDamage += Double.parseDouble(range[0].trim());
                                    currentStats.maxPhysicalDamage += Double.parseDouble(range[1].trim());
                                } else if (statName.contains("phép thuật")) {
                                    currentStats.minMagicalDamage += Double.parseDouble(range[0].trim());
                                    currentStats.maxMagicalDamage += Double.parseDouble(range[1].trim());
                                }
                            } else {
                                if (statName.equals("Độ bền") || statName.equals("Cơ hội bảo vệ")) continue;
                                double val = Double.parseDouble(valStr);
                                switch (statName) {
                                    case "Máu tối đa": currentStats.maxHealth += val; break;
                                    case "Giáp": currentStats.armor += val; break;
                                    case "Tỉ lệ chí mạng": currentStats.critRate += val; break;
                                    case "Sát thương chí mạng": currentStats.critDamage += val; break;
                                    case "Tỉ lệ xuyên giáp": currentStats.armorPen += val; break;
                                    case "Tỉ lệ hút máu": currentStats.lifeSteal += val; break;
                                    case "Tỉ lệ né tránh": currentStats.dodgeRate += val; break;
                                    case "Tỉ lệ đỡ đòn": currentStats.blockRate += val; break;
                                    case "Tỉ lệ hồi phục": currentStats.regenRate += val; break;
                                    case "Tốc độ đánh": currentStats.attackSpeed += val; break;
                                    case "Giảm hồi chiêu": currentStats.cooldownReduction += val; break;
                                    case "Tốc độ di chuyển": currentStats.moveSpeed += val; break;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        applySetBonuses();
        updateSetVisuals();
        updateMaxStats(); // Cập nhật lại thanh Máu Vanilla
        updateMoveSpeed(); // Cập nhật Tốc chạy Vanilla
    }

    private void applySetBonuses() {
        int vampiric = currentStats.setCounts.getOrDefault("vampiric", 0);
        int hunter = currentStats.setCounts.getOrDefault("hunter", 0);
        int giant = currentStats.setCounts.getOrDefault("giant", 0);
        int armored = currentStats.setCounts.getOrDefault("armored", 0);
        int swift = currentStats.setCounts.getOrDefault("swift", 0);
        int shattering = currentStats.setCounts.getOrDefault("shattering", 0);

        if (vampiric >= 3) currentStats.lifeSteal += 12.0;
        if (hunter >= 3) currentStats.critRate += 8.0;
        if (hunter >= 4) currentStats.critDamage += 40.0;
        if (giant >= 3) currentStats.hpMultiplier += 0.25;
        if (armored >= 3) currentStats.armorMultiplier += 0.25;
        if (swift >= 3) currentStats.moveSpeed += 15.0;
        if (shattering >= 3) currentStats.armorPen += 15.0;

        if (currentStats.critRate > 100.0) currentStats.critRate = 100.0;
        if (currentStats.armorPen > 80.0) currentStats.armorPen = 80.0;
        if (currentStats.cooldownReduction > 40.0) currentStats.cooldownReduction = 40.0;
        if (currentStats.lifeSteal > 50.0) currentStats.lifeSteal = 50.0;
        if (currentStats.attackSpeed > 100.0) currentStats.attackSpeed = 100.0;
        if (currentStats.dodgeRate > 50.0) currentStats.dodgeRate = 50.0;
        if (currentStats.blockRate > 50.0) currentStats.blockRate = 50.0;
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

    private void updateMoveSpeed() {
        AttributeInstance moveSpeedAttr = this.bukkitPlayer.getAttribute(Attribute.MOVEMENT_SPEED);
        if (moveSpeedAttr != null) {
            double equipSpeed = currentStats != null ? currentStats.moveSpeed : 0.0;
            moveSpeedAttr.setBaseValue(0.1 * (1.0 + (equipSpeed / 100.0)));
        }
    }

    public double getTotalMaxHealth() {
        double baseHp = this.currentClass != null ? this.currentClass.getMaxHealth() : 20.0;
        double levelBonus = this.bukkitPlayer.getLevel() * 50.0;
        double vitBonus = this.attributeData.getAttribute("VIT") * 15.0;

        // CHỈ NHÂN HỆ SỐ SET BONUS CHO RIÊNG MÁU CỦA TRANG BỊ
        double equipHp = currentStats != null ? (currentStats.maxHealth * currentStats.hpMultiplier) : 0.0;

        // Lượng máu hồi 1%/s sau này của bạn sẽ tính dựa trên tổng của toàn bộ phép cộng này!
        return baseHp + levelBonus + vitBonus + equipHp;
    }

    // --- CÁC HÀM CŨ GIỮ NGUYÊN BÊN DƯỚI ---
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
            this.attributeData.addUnspentAttrPoints(3);
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
    public double getTotalMaxResource() {
        double baseRes = this.currentClass.getMaxResource();
        double intBonus = this.attributeData.getAttribute("INT") * 1.0;
        return baseRes + intBonus;
    }
}