package vn.minecraft.rPGCore.jobs.core;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import vn.minecraft.rPGCore.database.MongoManager;

import java.util.*;

public class JobManager {
    private static final Map<String, RPGJob> registeredJobs = new HashMap<>();
    private static final Map<UUID, PlayerJobData> playerData = new HashMap<>();

    // Lấy Collection từ MongoDB
    private static MongoCollection<Document> getCollection() {
        return MongoManager.getInstance().getDatabase().getCollection("player_jobs");
    }

    public static void registerJob(RPGJob job) {
        registeredJobs.put(job.getId().toUpperCase(), job);
    }

    public static Collection<RPGJob> getAllJobs() {
        return registeredJobs.values();
    }

    public static PlayerJobData getPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerData.containsKey(uuid)) {
            loadPlayerData(uuid);
        }
        return playerData.get(uuid);
    }

    public static RPGJob getPlayerJob(Player player) {
        PlayerJobData data = getPlayerData(player);
        return registeredJobs.get(data.getJobId().toUpperCase());
    }

    public static int getPlayerJobLevel(Player player) {
        return getPlayerData(player).getLevel();
    }

    // ==========================================
    // CÔNG THỨC TÍNH EXP YÊU CẦU CỦA NGHỀ
    // ==========================================
    public static double getRequiredJobExp(int currentLevel) {
        return (Math.pow(currentLevel, 2) * 50) + 500;
    }

    // ==========================================
    // XỬ LÝ CỘNG EXP VÀ TỰ ĐỘNG THĂNG CẤP / NHẬN JP
    // ==========================================
    public static void addExp(Player player, double amount) {
        PlayerJobData data = getPlayerData(player);
        RPGJob job = getPlayerJob(player);
        if (job == null) return;

        int currentLevel = data.getLevel();
        int maxLevel = job.getMaxLevel();

        double currentExp = data.getExp() + amount;
        data.setExp(job.getId(), currentExp);
        player.sendMessage("§a[Nghề Phụ] §fBạn vừa nhận được §e" + String.format("%.1f", amount) + " EXP §fnghề §b" + job.getName() + "§f!");

        boolean leveledUp = false;
        int jpGained = 0;
        double requiredExp = getRequiredJobExp(currentLevel);

        while (currentExp >= requiredExp) {
            currentExp -= requiredExp;
            if (currentLevel < maxLevel) {
                currentLevel++;
                leveledUp = true;
                requiredExp = getRequiredJobExp(currentLevel);
            } else {
                jpGained++;
                requiredExp = getRequiredJobExp(maxLevel);
            }
        }

        data.setExp(job.getId(), currentExp);

        if (leveledUp) {
            data.setLevel(job.getId(), currentLevel);
            player.sendMessage("");
            player.sendMessage("§e✨ §lCHÚC MỪNG! §fKỹ năng nghề §b" + job.getName() + " §fcủa bạn đã thăng lên §aCấp " + currentLevel + "§f!");
            player.sendMessage("§7(Sử dụng lệnh /job menu để xem các chỉ số mới)");
            player.sendMessage("");
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }

        if (jpGained > 0) {
            data.addJobPoints(job.getId(), jpGained);
            player.sendMessage("");
            player.sendMessage("§d✨ §lĐỘT PHÁ TỐI THƯỢNG! §fBạn vừa nhận được §d+" + jpGained + " Điểm Kỹ Năng (JP) §fcho nghề §b" + job.getName() + "§f!");
            player.sendMessage("§7(Hãy mở Cây Kỹ Năng để học các nội tại ẩn)");
            player.sendMessage("");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        }
    }

    // ==========================================
    // [MỚI]: HỆ THỐNG LƯU TRỮ MONGODB
    // ==========================================
    public static void loadPlayerData(UUID uuid) {
        Document doc = getCollection().find(Filters.eq("_id", uuid.toString())).first();
        PlayerJobData data = new PlayerJobData();

        if (doc != null) {
            data.setJobId(doc.getString("currentJob") != null ? doc.getString("currentJob") : "NONE");
            data.addBlacksmithScraps(doc.getInteger("scraps", 0));

            // Load Maps
            Document levels = (Document) doc.get("levels");
            if (levels != null) for (String k : levels.keySet()) data.setLevel(k, levels.getInteger(k));

            Document exps = (Document) doc.get("exps");
            if (exps != null) for (String k : exps.keySet()) data.setExp(k, ((Number) exps.get(k)).doubleValue());

            Document qProgress = (Document) doc.get("questProgress");
            if (qProgress != null) for (String k : qProgress.keySet()) data.setQuestProgress(k, qProgress.getInteger(k));

            Document qClaimed = (Document) doc.get("questClaimed");
            if (qClaimed != null) for (String k : qClaimed.keySet()) data.setQuestClaimed(k, qClaimed.getBoolean(k));

            Document jPoints = (Document) doc.get("jobPoints");
            if (jPoints != null) for (String k : jPoints.keySet()) data.setJobPoints(k, jPoints.getInteger(k));

            Document uSkills = (Document) doc.get("unlockedSkills");
            if (uSkills != null) {
                for (String k : uSkills.keySet()) {
                    List<String> skills = uSkills.getList(k, String.class);
                    if (skills != null) for (String s : skills) data.unlockSkill(k, s);
                }
            }

            Document sPurchases = (Document) doc.get("shopPurchases");
            if (sPurchases != null) {
                for (String k : sPurchases.keySet()) data.getShopMonthlyPurchases().put(k, sPurchases.getInteger(k));
            }

            data.setLastMonthlyShopReset(doc.get("lastMonthlyShopReset") != null ? ((Number) doc.get("lastMonthlyShopReset")).longValue() : System.currentTimeMillis());
            data.setActiveDailyQuests(doc.getList("activeDaily", String.class) != null ? doc.getList("activeDaily", String.class) : new ArrayList<>());
            data.setActiveWeeklyQuests(doc.getList("activeWeekly", String.class) != null ? doc.getList("activeWeekly", String.class) : new ArrayList<>());
            data.setDailyMilestone(doc.getInteger("dailyMilestone", 0));
            data.setLastDailyReset(doc.get("lastDailyReset") != null ? ((Number) doc.get("lastDailyReset")).longValue() : System.currentTimeMillis());
            data.setLastWeeklyReset(doc.get("lastWeeklyReset") != null ? ((Number) doc.get("lastWeeklyReset")).longValue() : System.currentTimeMillis());
            data.setMerchantAccumulatedCoins(doc.get("merchantAccumulatedCoins") != null ? ((Number) doc.get("merchantAccumulatedCoins")).longValue() : 0L);
        }

        playerData.put(uuid, data);
    }

    // ==========================================
    // [ĐÃ SỬA]: Thêm tham số isAsync
    // ==========================================
    public static void savePlayerData(UUID uuid, boolean isAsync) {
        PlayerJobData data = playerData.get(uuid);
        if (data == null) return;

        Document doc = new Document("_id", uuid.toString())
                .append("currentJob", data.getJobId())
                .append("scraps", data.getBlacksmithScraps())
                .append("levels", new Document(data.getJobLevelsMap()))
                .append("exps", new Document(data.getJobExpsMap()))
                .append("questProgress", new Document(data.getQuestProgressMap()))
                .append("questClaimed", new Document(data.getQuestClaimedMap()))
                .append("jobPoints", new Document(data.getJobPointsMap()))
                .append("unlockedSkills", new Document(data.getUnlockedSkillsMap()))
                .append("shopPurchases", new Document(data.getShopMonthlyPurchases()))
                .append("lastMonthlyShopReset", data.getLastMonthlyShopReset())
                .append("activeDaily", data.getActiveDailyQuests())
                .append("activeWeekly", data.getActiveWeeklyQuests())
                .append("dailyMilestone", data.getDailyMilestone())
                .append("lastDailyReset", data.getLastDailyReset())
                .append("lastWeeklyReset", data.getLastWeeklyReset())
                .append("merchantAccumulatedCoins", data.getMerchantAccumulatedCoins());

        // Nếu isAsync = true -> Chạy ngầm không làm lag server
        if (isAsync) {
            Bukkit.getScheduler().runTaskAsynchronously(vn.minecraft.rPGCore.RPGCore.getInstance(), () -> {
                getCollection().replaceOne(Filters.eq("_id", uuid.toString()), doc, new ReplaceOptions().upsert(true));
            });
        }
        // Nếu isAsync = false -> Ép chạy đồng bộ (bắt server chờ lưu xong mới tắt)
        else {
            getCollection().replaceOne(Filters.eq("_id", uuid.toString()), doc, new ReplaceOptions().upsert(true));
        }
    }

    // [ĐÃ SỬA]: Cập nhật lại saveAll để dùng cho lệnh tắt Server
    public static void saveAll(boolean isAsync) {
        for (UUID uuid : playerData.keySet()) {
            savePlayerData(uuid, isAsync);
        }
    }

    // ==========================================
    // TỰ ĐỘNG LƯU KHI NGƯỜI CHƠI THOÁT GAME
    // ==========================================
    public static class JobDataListener implements Listener {
        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            loadPlayerData(player.getUniqueId());

            Bukkit.getScheduler().runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), () -> {
                vn.minecraft.rPGCore.jobs.core.JobQuestManager.checkAndResetQuests(player);
            }, 20L);
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            UUID uuid = event.getPlayer().getUniqueId();
            savePlayerData(uuid, true); // Thêm chữ true
            playerData.remove(uuid);
        }
    }

    /* =========================================
     * HOOK API DÀNH CHO CÁC CLASS BÊN NGOÀI
     * ========================================= */
    public static double getForgeBonus(Player player, int itemLevel) {
        RPGJob job = getPlayerJob(player);
        return (job != null) ? job.getForgeSuccessBonus(getPlayerJobLevel(player), itemLevel) : 0.0;
    }
    public static double getLineageBonus(Player player) {
        RPGJob job = getPlayerJob(player);
        return (job != null) ? job.getLineageStarBonus(getPlayerJobLevel(player)) : 0.0;
    }
    public static double getFreshnessBuff(Player player) {
        RPGJob job = getPlayerJob(player);
        return (job != null) ? job.getFreshnessMultiplier(getPlayerJobLevel(player)) : 1.0;
    }

    public static void processMerchantExp(Player player, long amount) {
        PlayerJobData data = getPlayerData(player);
        if (data == null || !data.getJobId().equalsIgnoreCase("MERCHANT")) return;
        int level = data.getLevel("MERCHANT");
        double expPerCoin = 0.1 + (level * 0.002);
        double totalExp = amount * expPerCoin;
        addExp(player, totalExp);

        long currentAcc = data.getMerchantAccumulatedCoins();
        long totalCoins = currentAcc + amount;
        long goldUnits = totalCoins / 4096;
        long leftoverCoins = totalCoins % 4096;

        data.setMerchantAccumulatedCoins(leftoverCoins);

        if (goldUnits > 0) {
            double tokenDropChance = Math.min(level * 0.5, 25.0);
            double totalExpected = goldUnits * (tokenDropChance / 100.0);
            int tokens = (int) totalExpected;
            if (Math.random() < (totalExpected - tokens)) tokens++;

            if (tokens > 0) {
                org.bukkit.inventory.ItemStack tokenItem = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager().buildItem("misc_merchant_token", tokens);
                if (tokenItem != null) {
                    java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> leftOver = player.getInventory().addItem(tokenItem);
                    for (org.bukkit.inventory.ItemStack drop : leftOver.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    player.sendMessage("§e§l[Uy Tín Thương Trường] §fDòng tiền giao dịch đạt §6" + goldUnits + " Vàng§f, bạn nhận được §a+" + tokens + " Chứng Nhận Thương Nhân§f!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                }
            }
        }
    }

    public static void tryDropManaEssence(Player player, double baseChance) {
        PlayerJobData data = getPlayerData(player);
        if (data == null || !data.getJobId().equalsIgnoreCase("ALCHEMIST")) return;
        int level = data.getLevel("ALCHEMIST");
        double finalChance = baseChance + (level * 0.1);

        if (Math.random() * 100.0 <= finalChance) {
            vn.minecraft.rPGCore.managers.ItemManager im = vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager();
            org.bukkit.inventory.ItemStack essenceItem = im.buildItem("essence_mana", 1);
            if (essenceItem != null) {
                java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> leftOver = player.getInventory().addItem(essenceItem);
                for (org.bukkit.inventory.ItemStack drop : leftOver.values()) player.getWorld().dropItemNaturally(player.getLocation(), drop);
                player.sendMessage("§b§l[Tinh Tế] §fTrong lúc làm việc, bạn đã tích tụ thành công §b+1 Tinh Chất Ma Lực§f!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.COLLECT_MANA_ESSENCE, 1);
            }
        }
    }

    public static void processAlchemistHarvestExp(Player player, String rarity) {
        PlayerJobData data = getPlayerData(player);
        if (data == null || !data.getJobId().equalsIgnoreCase("ALCHEMIST")) return;

        double baseExp = 15.0;
        double multiplier = 1.0;
        if (rarity != null) {
            switch (rarity.toUpperCase()) {
                case "COMMON": multiplier = 1.1; break;
                case "UNCOMMON": multiplier = 1.25; break;
                case "RARE": multiplier = 1.5; break;
                case "EPIC": multiplier = 1.75; break;
                case "MYTHIC": multiplier = 2.0; break;
                case "LEGENDARY": multiplier = 2.5; break;
            }
        }
        addExp(player, baseExp * multiplier);
        vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.GATHER_ITEM, 1);
        if (rarity != null && (rarity.equalsIgnoreCase("RARE") || rarity.equalsIgnoreCase("EPIC") || rarity.equalsIgnoreCase("MYTHIC") || rarity.equalsIgnoreCase("LEGENDARY"))) {
            vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.ALCH_HARVEST_RARE, 1);
        }
    }

    public static void processAlchemistBrewExp(Player player, vn.minecraft.rPGCore.brewing.PotionData result, List<vn.minecraft.rPGCore.managers.ItemManager.ItemData> ingredients) {
        PlayerJobData data = getPlayerData(player);
        if (data == null || !data.getJobId().equalsIgnoreCase("ALCHEMIST")) return;

        double finalExp = 15.0;
        if (result.isRuined) {
            finalExp = 10.0;
            vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.BREW_FAIL, 1);
        } else {
            if (ingredients != null) {
                for (vn.minecraft.rPGCore.managers.ItemManager.ItemData itemData : ingredients) {
                    if (itemData == null || itemData.rarity == null) continue;
                    switch (itemData.rarity) {
                        case COMMON: finalExp += 2.0; break;
                        case UNCOMMON: finalExp += 5.0; break;
                        case RARE: finalExp += 10.0; break;
                        case EPIC: finalExp += 20.0; break;
                        case MYTHIC: finalExp += 40.0; break;
                        case LEGENDARY: finalExp += 80.0; break;
                    }
                }
            }
            double flavorBonus = 1.0;
            if (result.flavorTags.contains("§a[Cân Bằng Hoàn Hảo]")) flavorBonus += 0.15;
            if (result.isEnhanced) flavorBonus += 0.15;
            finalExp *= flavorBonus;

            vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.BREW_SUCCESS, 1);
            if (result.flavorTags.contains("§a[Cân Bằng Hoàn Hảo]") || result.isEnhanced) {
                vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.BREW_PERFECT, 1);
            }
        }
        vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.BREW_ANY, 1);
        addExp(player, Math.max(5.0, finalExp));
    }
}