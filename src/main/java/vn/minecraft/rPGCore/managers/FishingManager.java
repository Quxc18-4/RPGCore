package vn.minecraft.rPGCore.managers;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class FishingManager {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final Map<UUID, MuseDashMinigame> activeMinigames = new HashMap<>();
    private final Random rand = new Random();

    public final NamespacedKey KEY_GUARANTEE_MONSTER, KEY_GUARANTEE_X3, KEY_GUARANTEE_TREASURE, KEY_INSTANT;

    public FishingManager(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;

        KEY_GUARANTEE_MONSTER = new NamespacedKey(plugin, "bait_guarantee_monster");
        KEY_GUARANTEE_X3 = new NamespacedKey(plugin, "bait_guarantee_x3");
        KEY_GUARANTEE_TREASURE = new NamespacedKey(plugin, "bait_guarantee_treasure");
        KEY_INSTANT = new NamespacedKey(plugin, "bait_instant");
    }

    public NamespacedKey getKey(String stat, String biome) {
        return new NamespacedKey(plugin, stat + "_" + biome);
    }

    public String getCurrentBiomeType(org.bukkit.Location loc) {
        String b = loc.getBlock().getBiome().getKey().getKey().toUpperCase();
        if (b.contains("OCEAN") || b.contains("BEACH")) return "OCEAN";
        if (b.contains("RIVER")) return "RIVER";
        if (b.contains("SWAMP")) return "SWAMP";
        if (loc.getY() < 50) return "CAVE";
        return "ALL";
    }

    public int getWaterDepth(org.bukkit.Location loc) {
        int depth = 0;
        org.bukkit.Location check = loc.clone();
        while (depth < 20) {
            check.subtract(0, 1, 0);
            if (check.getBlock().getType() == org.bukkit.Material.WATER) depth++;
            else break;
        }
        return depth;
    }

    public int getWaterVolume(org.bukkit.Location loc) {
        int volume = 0;
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (loc.clone().add(x, y, z).getBlock().getType() == org.bukkit.Material.WATER) volume++;
                }
            }
        }
        return volume;
    }

    // Hàm trừ độ bền (Có tính toán tỉ lệ Bất hoại)
    public void consumeRodDurability(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null || !mainHand.hasItemMeta()) return;
        String rodId = itemManager.getItemId(mainHand);
        if (rodId == null || rodId.equals("tool_fishing_rod_1")) return; // Cần tập sự vô hạn

        org.bukkit.inventory.meta.ItemMeta meta = mainHand.getItemMeta();
        NamespacedKey duraKey = new NamespacedKey(plugin, "rod_durability");
        NamespacedKey unbreakingKey = new NamespacedKey(plugin, "rod_unbreaking");

        if (meta.getPersistentDataContainer().has(unbreakingKey, PersistentDataType.DOUBLE)) {
            double saveChance = meta.getPersistentDataContainer().get(unbreakingKey, PersistentDataType.DOUBLE);
            if (rand.nextDouble() * 100.0 < saveChance) {
                return; // Kích hoạt buff: Không tốn độ bền
            }
        }

        if (meta.getPersistentDataContainer().has(duraKey, PersistentDataType.INTEGER)) {
            int dura = meta.getPersistentDataContainer().get(duraKey, PersistentDataType.INTEGER);
            dura--;
            if (dura <= 0) {
                player.getInventory().setItemInMainHand(null);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                player.sendMessage("§c§l[!] §fCần câu của bạn đã gãy nát vì hết độ bền!");
            } else {
                meta.getPersistentDataContainer().set(duraKey, PersistentDataType.INTEGER, dura);
                List<String> lore = meta.getLore();
                if (lore != null) {
                    for (int i = 0; i < lore.size(); i++) {
                        if (lore.get(i).contains("Độ bền:")) {
                            lore.set(i, "§7Độ bền: §f" + dura + " §8lần câu");
                            break;
                        }
                    }
                }
                meta.setLore(lore);
                mainHand.setItemMeta(meta);
            }
        }
    }

    public boolean isInMinigame(Player player) { return activeMinigames.containsKey(player.getUniqueId()); }
    public void handleInteract(Player player, boolean isLeftClick) { if (isInMinigame(player)) activeMinigames.get(player.getUniqueId()).playerClick(isLeftClick); }
    public void cancelMinigame(Player player) { if (isInMinigame(player)) { activeMinigames.get(player.getUniqueId()).stop(); activeMinigames.remove(player.getUniqueId()); } }

    public void startMinigame(Player player, String rodId, ItemStack baitItem, org.bukkit.Location hookLoc, org.bukkit.entity.FishHook hook) {
        MuseDashMinigame minigame = new MuseDashMinigame(player, rodId, baitItem, hookLoc, hook);
        activeMinigames.put(player.getUniqueId(), minigame);
        minigame.start();
    }

    // [LỐI CHƠI MỚI]: Bỏ hoàn toàn Demo, chỉ còn Play
    private enum GameState { STAGE_1_MEMORIZE, STAGE_1_PLAY, SURPRISE_WARN, STAGE_2_MEMORIZE, STAGE_2_PLAY, ENDED }

    private class Note {
        boolean isLeftClick;
        boolean isHidden = false;
        boolean isMutatedHidden = false;     // [MỚI] Đánh dấu phím bị ẩn do đột biến giữa chừng
        boolean isDifferentFromPrev = false; // [MỚI] Đánh dấu phím này khác với phím ngay trước nó

        public Note(boolean isLeftClick) { this.isLeftClick = isLeftClick; }
    }

    private class MuseDashMinigame {
        Player player; String rodId; ItemStack baitItem; org.bukkit.Location hookLoc;
        org.bukkit.entity.FishHook hook;
        GameState state; boolean isBigFish;
        List<Note> stage1 = new ArrayList<>(); List<Note> stage2 = new ArrayList<>(); List<Note> currentStage;

        int currentNoteIdx = 0; long debounceMs = 0;
        int lives = 1;
        boolean comboMutated = false;
        private int timeoutTaskId = -1;

        double trashMod = 0, treasureMod = 0, secondChance = 0;
        boolean gMonster = false, gX3 = false, gTreasure = false, isAutoCatch = false;

        public MuseDashMinigame(Player player, String rodId, ItemStack baitItem, org.bukkit.Location hookLoc, org.bukkit.entity.FishHook hook) {
            this.player = player; this.rodId = rodId; this.baitItem = baitItem; this.hookLoc = hookLoc;
            this.hook = hook;

            // Mặc định 1 mạng, dùng mồi đặc chế cấp thêm 1 mạng (Tổng 2)
            this.lives = (baitItem != null && baitItem.hasItemMeta()) ? 2 : 1;

            double bigFishChance = 0.05;
            String currentBiome = getCurrentBiomeType(hookLoc);

            // [MỚI]: HOOK HƯỞNG BUFF NGƯ DÂN TỪ HỆ THỐNG NGHỀ
            vn.minecraft.rPGCore.jobs.core.PlayerJobData jobData = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
            if (jobData != null && "FISHERMAN".equals(jobData.getJobId())) {
                int level = jobData.getLevel("FISHERMAN");
                double ultMult = jobData.hasUnlockedSkill("FISHERMAN", "FISH_ULT_1") ? 1.2 : 1.0;

                bigFishChance += (level * 0.5 * ultMult) / 100.0;
                treasureMod += (level * 0.3 * ultMult);

                double autoChance = (level * 0.5 * ultMult);
                if (rand.nextDouble() * 100.0 < autoChance) isAutoCatch = true;
            }

            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand.hasItemMeta()) {
                org.bukkit.persistence.PersistentDataContainer rodPdc = mainHand.getItemMeta().getPersistentDataContainer();
                bigFishChance += rodPdc.getOrDefault(new NamespacedKey(plugin, "rod_big_fish"), PersistentDataType.DOUBLE, 0.0) / 100.0;
                trashMod -= rodPdc.getOrDefault(new NamespacedKey(plugin, "rod_trash"), PersistentDataType.DOUBLE, 0.0);
                treasureMod += rodPdc.getOrDefault(new NamespacedKey(plugin, "rod_treasure"), PersistentDataType.DOUBLE, 0.0);
                secondChance += rodPdc.getOrDefault(new NamespacedKey(plugin, "rod_second_chance"), PersistentDataType.DOUBLE, 0.0);

                double rodAuto = rodPdc.getOrDefault(new NamespacedKey(plugin, "rod_auto_catch"), PersistentDataType.DOUBLE, 0.0);
                if (rodAuto > 0 && rand.nextDouble() * 100.0 < rodAuto) isAutoCatch = true;
            }

            if (baitItem != null && baitItem.hasItemMeta()) {
                org.bukkit.persistence.PersistentDataContainer pdc = baitItem.getItemMeta().getPersistentDataContainer();
                bigFishChance += getStat(pdc, "bait_big_fish", currentBiome) / 100.0;
                trashMod += getStat(pdc, "bait_trash", currentBiome);
                treasureMod += getStat(pdc, "bait_treasure", currentBiome);
                secondChance += getStat(pdc, "bait_second_chance", currentBiome);

                double autoCatchChance = getStat(pdc, "bait_auto_catch", currentBiome);
                if (autoCatchChance > 0 && rand.nextDouble() * 100.0 < autoCatchChance) isAutoCatch = true;

                if (pdc.has(KEY_GUARANTEE_MONSTER, PersistentDataType.BYTE)) gMonster = true;
                if (pdc.has(KEY_GUARANTEE_X3, PersistentDataType.BYTE)) gX3 = true;
                if (pdc.has(KEY_GUARANTEE_TREASURE, PersistentDataType.BYTE)) gTreasure = true;
            }

            // [MỚI] 2. PHÂN TÍCH MÔI TRƯỜNG ĐỘNG
            if (hookLoc.getWorld().hasStorm()) bigFishChance += 0.15; // Mưa
            if (hookLoc.getWorld().getTime() >= 13000) bigFishChance += 0.10; // Đêm

            int depth = getWaterDepth(hookLoc);
            if (depth >= 10) treasureMod += 10.0;
            else if (depth >= 5) treasureMod += 5.0;

            int volume = getWaterVolume(hookLoc);
            if (volume < 40) bigFishChance -= 0.20; // Ao tù (Né treo máy 1x1)
            else if (volume > 100) bigFishChance += 0.10; // Đại dương/Hồ lớn

            this.isBigFish = gMonster || (rand.nextDouble() <= bigFishChance);

            // Sinh chuỗi Stage 1 (4-5 nút)
            int s1Len = 4 + rand.nextInt(2);
            for (int i = 0; i < s1Len; i++) {
                Note n = new Note(rand.nextBoolean());
                if (isBigFish && rand.nextDouble() < 0.20) n.isHidden = true; // Cá hiếm ẩn 20%
                stage1.add(n);
            }

            // Sinh chuỗi Stage 2 (Thủy quái, 6-8 nút)
            if (isBigFish) {
                int s2Len = 6 + rand.nextInt(3);
                for (int i = 0; i < s2Len; i++) {
                    Note n = new Note(rand.nextBoolean());
                    if (rand.nextDouble() < 0.30) n.isHidden = true; // Thủy quái ẩn 30%
                    stage2.add(n);
                }
            }
        }

        private double getStat(org.bukkit.persistence.PersistentDataContainer pdc, String baseKey, String biome) {
            double total = pdc.getOrDefault(getKey(baseKey, "ALL"), PersistentDataType.DOUBLE, 0.0);
            total += pdc.getOrDefault(getKey(baseKey, biome), PersistentDataType.DOUBLE, 0.0);

            // Xử lý hệ số pha chế của Ngư Dân
            double multi = pdc.getOrDefault(new NamespacedKey(plugin, "bait_fisher_multi"), PersistentDataType.DOUBLE, 1.0);
            return total * multi;
        }

        public void start() {
            if (isAutoCatch) {
                player.sendMessage("§b[Bùa Lười Biếng] Đã tự động kéo cá lên bờ!");
                winMinigame(isBigFish);
                return;
            }

            // [FIX]: Giai đoạn Ghi nhớ 3 giây (Vừa để nhìn phím, vừa triệt tiêu click đè)
            state = GameState.STAGE_1_MEMORIZE;
            currentStage = stage1;
            player.sendTitle("§e§lGHI NHỚ PHÍM!", "§fChuẩn bị nhập...", 5, 40, 5);
            renderActionBar(true); // true = Show hết, không giấu phím

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (state == GameState.ENDED) return;
                state = GameState.STAGE_1_PLAY;
                player.sendTitle("§a§lBẮT ĐẦU!", "§fNhập ngay trước khi hết giờ!", 2, 20, 5);
                renderActionBar(false); // false = Bắt đầu giấu các phím [?]
                startStageTimeout(8000);
            }, 60L); // Chờ 3 giây
        }

        public void playerClick(boolean isLeftClick) {
            if (state != GameState.STAGE_1_PLAY && state != GameState.STAGE_2_PLAY) return;

            long now = System.currentTimeMillis();
            if (now - debounceMs < 100) return;
            debounceMs = now;

            Note targetNote = currentStage.get(currentNoteIdx);
            boolean isCorrect = false;

            // [FIX TẬN GỐC]: Đối chiếu điều kiện bấm dựa trên phím gốc đã ghi nhớ và loại đột biến hiển thị
            if (targetNote.isMutatedHidden) {
                if (targetNote.isDifferentFromPrev) {
                    // [⇌] Nghịch: Người chơi nhìn thấy dấu nghịch -> Phải bấm NGƯỢC LẠI phím gốc của nốt này
                    isCorrect = (isLeftClick != targetNote.isLeftClick);
                } else {
                    // [=] Bằng: Người chơi nhìn thấy dấu bằng -> Phải bấm GIỐNG HỆT phím gốc của nốt này
                    isCorrect = (isLeftClick == targetNote.isLeftClick);
                }
            } else {
                // Nốt thường: So sánh trực tiếp với phím gốc ban đầu
                isCorrect = (targetNote.isLeftClick == isLeftClick);
            }

            if (isCorrect) {
                if (isLeftClick) player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.2f, 1.5f);
                else player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.5f, 0.5f);

                currentNoteIdx++;

                // Cơ chế Phá đám: Kích hoạt đột biến ở giữa chuỗi khi chơi cá lớn
                if (isBigFish && !comboMutated && currentNoteIdx == (currentStage.size() / 2)) {
                    comboMutated = true;
                    for (int i = currentNoteIdx; i < currentStage.size(); i++) {
                        // SỬA: Lấy trực tiếp nốt gốc đang có, KHÔNG ĐƯỢC khởi tạo 'new Note' làm mất chuỗi phím người chơi đã nhớ!
                        Note mutatedNote = currentStage.get(i);

                        if (rand.nextDouble() < 0.35) {
                            mutatedNote.isHidden = true;
                            mutatedNote.isMutatedHidden = true; // Đánh dấu ẩn do đột biến

                            // SỬA: Quyết định ngẫu nhiên yêu cầu thực thi:
                            // false = Hiện dấu [=] (Giữ nguyên phím gốc), true = Hiện dấu [⇌] (Nghịch đảo phím gốc)
                            mutatedNote.isDifferentFromPrev = rand.nextBoolean();
                        }
                    }
                    player.sendTitle("", "§c§l⚠️ COMBO BIẾN ĐỔI! ⚠️", 0, 20, 5);
                    player.playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.7f, 1.2f);
                }

                renderActionBar(false);

                if (currentNoteIdx >= currentStage.size()) {
                    handleStageComplete();
                }
            } else {
                deductLife("§cSai phím!");
            }
        }

        private void deductLife(String reason) {
            // [FIX]: VÔ HẠN MẠNG NẾU CÓ ULTIMATE 3
            vn.minecraft.rPGCore.jobs.core.PlayerJobData jobData = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
            if (jobData != null && "FISHERMAN".equals(jobData.getJobId()) && jobData.hasUnlockedSkill("FISHERMAN", "FISH_ULT_3")) {
                player.sendMessage("§d[Ý Chí Kiên Cường] Lỗi sai đã bị bỏ qua!");
                // Không trừ lives
            } else {
                lives--;
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);

            if (lives <= 0) {
                loseMinigame("§cBạn đã hụt quá nhiều lần!");
            } else {
                player.sendMessage("§c" + reason + " Bạn bị trừ 1 mạng! Ghi nhớ lại từ đầu.");
                currentNoteIdx = 0;
                comboMutated = false;
                if (timeoutTaskId != -1) Bukkit.getScheduler().cancelTask(timeoutTaskId);

                // [FIX]: Trở về trạng thái ghi nhớ của Stage hiện tại
                state = (currentStage == stage1) ? GameState.STAGE_1_MEMORIZE : GameState.STAGE_2_MEMORIZE;
                renderActionBar(true);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (state == GameState.ENDED) return;
                    state = (currentStage == stage1) ? GameState.STAGE_1_PLAY : GameState.STAGE_2_PLAY;
                    renderActionBar(false);
                    startStageTimeout(8000);
                }, 60L); // Cho 3 giây để nhìn lại chuỗi mới
            }
        }

        private void handleStageComplete() {
            if (timeoutTaskId != -1) {
                Bukkit.getScheduler().cancelTask(timeoutTaskId);
                timeoutTaskId = -1;
            }

            if (state == GameState.STAGE_1_PLAY) {
                if (isBigFish) {
                    state = GameState.SURPRISE_WARN;
                    comboMutated = false;
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.8f);
                    player.sendTitle("§c§l!!! CÁ LỚN QUẪY ĐUÔI !!!", "§fChuẩn bị Combo liên hoàn...", 5, 40, 10);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (state == GameState.ENDED) return;

                        // [FIX]: Chuyển sang Ghi nhớ Stage 2
                        state = GameState.STAGE_2_MEMORIZE;
                        currentStage = stage2;
                        currentNoteIdx = 0;
                        renderActionBar(true);

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (state == GameState.ENDED) return;
                            state = GameState.STAGE_2_PLAY;
                            renderActionBar(false);
                            startStageTimeout(8000);
                        }, 60L); // 3 Giây nhìn phím Stage 2
                    }, 50L);
                } else {
                    stop(); winMinigame(false);
                }
            } else if (state == GameState.STAGE_2_PLAY) {
                stop(); winMinigame(true);
            }
        }

        private void startStageTimeout(long msLimit) {
            if (timeoutTaskId != -1) Bukkit.getScheduler().cancelTask(timeoutTaskId);
            timeoutTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (state == GameState.STAGE_1_PLAY || state == GameState.STAGE_2_PLAY) {
                    loseMinigame("§cQuá chậm! Cá đã bơi mất.");
                }
            }, msLimit / 50L).getTaskId();
        }

        public void stop() {
            state = GameState.ENDED;
            if (timeoutTaskId != -1) {
                Bukkit.getScheduler().cancelTask(timeoutTaskId);
                timeoutTaskId = -1;
            }
        }

        private void winMinigame(boolean isBigReward) {
            stop();
            activeMinigames.remove(player.getUniqueId());
            if (hook != null && !hook.isDead()) hook.remove();

            consumeRodDurability(player);

            player.sendTitle("§a§lHOÀN HẢO!", "§fKéo cá thành công!", 5, 30, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

            String caughtItemId;
            double roll = rand.nextDouble() * 100.0;
            double baseTrash = 30.0 + trashMod;
            double baseTreasure = 2.0 + treasureMod;
            String currentBiome = getCurrentBiomeType(hookLoc);

            // [FIX]: GỌI HÀM RANDOM CÓ TRỌNG SỐ VÀ TÍNH EXP
            if (gTreasure || roll < baseTreasure) {
                caughtItemId = getWeightedTreasure();
                player.sendMessage("§6[Kho Báu] Bạn vừa kéo lên một vật phẩm giá trị!");
                vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.FISH_TREASURE);
                // Dưới dòng thông báo câu được Kho báu
                Bukkit.getPluginManager().callEvent(new vn.minecraft.rPGCore.api.events.PlayerActionTrackEvent(player, "FISH", 1));
                // [TÍNH EXP RƯƠNG]: Cố định 50 * hệ số hiếm
                vn.minecraft.rPGCore.jobs.core.PlayerJobData jobData = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
                if (jobData != null && "FISHERMAN".equalsIgnoreCase(jobData.getJobId())) {
                    double treasureExp = 50;
                    if (caughtItemId.contains("common")) treasureExp *= 1.25;
                    else if (caughtItemId.contains("uncommon") || caughtItemId.contains("scrap")) treasureExp *= 1.5;
                    else if (caughtItemId.contains("rare") || caughtItemId.contains("hammer")) treasureExp *= 2.0;
                    else if (caughtItemId.contains("card") || caughtItemId.contains("charm")) treasureExp *= 2.5;
                    else treasureExp *= 1.25;
                    vn.minecraft.rPGCore.jobs.core.JobManager.addExp(player, treasureExp);
                }

            } else if (!gMonster && roll < baseTrash) {
                caughtItemId = getWeightedTrash();
                player.sendMessage("§7[Mắc Cạn] Chỉ là một món đồ vụn vặt trôi dạt...");
                vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.FISH_TRASH);
                // Dưới dòng thông báo câu được Rác
                Bukkit.getPluginManager().callEvent(new vn.minecraft.rPGCore.api.events.PlayerActionTrackEvent(player, "FISH", 1));
                // [TÍNH EXP RÁC]: Mặc định 15 EXP
                vn.minecraft.rPGCore.jobs.core.PlayerJobData jobData = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
                if (jobData != null && "FISHERMAN".equalsIgnoreCase(jobData.getJobId())) {
                    vn.minecraft.rPGCore.jobs.core.JobManager.addExp(player, 15.0);
                }

            } else {
                String[] normalPool;
                String[] bigPool;

                switch(currentBiome) {
                    case "OCEAN":
                        normalPool = new String[]{ "fish_cod_small", "fish_cod_med", "fish_tuna_small", "fish_tuna_med", "fish_marlin_small", "fish_starfall_ray_small" };
                        bigPool = new String[]{ "fish_cod_large", "bfish_cod_mutant", "fish_tuna_large", "bfish_tuna_mutant", "fish_marlin_med", "fish_marlin_large", "bfish_marlin_mutant", "fish_shark_small", "fish_shark_med", "bfish_shark_large", "bfish_shark_mutant", "bfish_starfall_ray_med", "bfish_starfall_ray_large", "bfish_starfall_ray_mutant" };
                        break;
                    case "RIVER":
                        normalPool = new String[]{ "fish_salmon_small", "fish_salmon_med", "fish_carp_small", "fish_carp_med", "fish_catfish_small", "fish_sturgeon_small" };
                        bigPool = new String[]{ "fish_salmon_large", "bfish_salmon_mutant", "fish_carp_large", "bfish_carp_mutant", "fish_catfish_med", "fish_catfish_large", "bfish_catfish_mutant", "fish_sturgeon_med", "fish_sturgeon_large", "bfish_sturgeon_mutant", "fish_golden_koi_small", "bfish_golden_koi_med", "bfish_golden_koi_large", "bfish_golden_koi_mutant" };
                        break;
                    case "CAVE":
                        normalPool = new String[]{ "fish_tropical_small", "fish_tropical_med", "fish_blind_tetra_small", "fish_blind_tetra_med", "fish_glow_squid_small" };
                        bigPool = new String[]{ "fish_tropical_large", "bfish_tropical_mutant", "fish_blind_tetra_large", "bfish_blind_tetra_mutant", "fish_glow_squid_med", "fish_glow_squid_large", "bfish_glow_squid_mutant", "fish_abyss_angler_small", "fish_abyss_angler_med", "fish_abyss_angler_large", "bfish_abyss_angler_mutant", "fish_time_weaver_small", "bfish_time_weaver_med", "bfish_time_weaver_large", "bfish_time_weaver_mutant" };
                        break;
                    case "SWAMP":
                        normalPool = new String[]{ "fish_mud_loach_small", "fish_mud_loach_med", "fish_swamp_eel_small", "fish_swamp_eel_med", "fish_toxic_toad_small" };
                        bigPool = new String[]{ "fish_mud_loach_large", "bfish_mud_loach_mutant", "fish_swamp_eel_large", "bfish_swamp_eel_mutant", "fish_toxic_toad_med", "fish_toxic_toad_large", "bfish_toxic_toad_mutant" };
                        break;
                    default:
                        normalPool = new String[]{ "fish_cod_small", "fish_cod_med", "fish_salmon_small", "fish_salmon_med", "fish_tropical_small", "fish_tropical_med" };
                        bigPool = new String[]{ "fish_cod_large", "bfish_cod_mutant", "fish_salmon_large", "bfish_salmon_mutant", "fish_tropical_large", "bfish_tropical_mutant" };
                }

                // GỌI HÀM LỌC TỈ LỆ CÁ
                String[] poolToUse = isBigReward ? bigPool : normalPool;
                caughtItemId = pickWeightedFish(poolToUse);

                // Báo cáo tiến trình nhiệm vụ ngày/tuần
                vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.FISH_CATCH);
                boolean isLargeOrMutant = caughtItemId.contains("large") || caughtItemId.contains("mutant");
                vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.FISH_CATCH, currentBiome, isLargeOrMutant);
                // Dưới dòng báo cáo FISH_CATCH (Cá)
                Bukkit.getPluginManager().callEvent(new vn.minecraft.rPGCore.api.events.PlayerActionTrackEvent(player, "FISH", 1));
                // [CHỐT CHẶN NGHỀ NGƯ DÂN]: Lấy dữ liệu Job lên trước để bao bọc cả phần thưởng Nước Mắt và EXP
                vn.minecraft.rPGCore.jobs.core.PlayerJobData jobData = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
                if (jobData != null && "FISHERMAN".equalsIgnoreCase(jobData.getJobId())) {

                    // 1. XỬ LÝ RỚT NƯỚC MẮT ĐẠI DƯƠNG (Chỉ dành riêng cho Ngư Dân)
                    double dropChance = 0;
                    if (caughtItemId.contains("mutant")) dropChance = 100.0;
                    else if (caughtItemId.contains("large")) dropChance = 70.0;
                    else if (caughtItemId.contains("med")) dropChance = 25.0;
                    else if (caughtItemId.contains("small")) dropChance = 10.0;

                    if (rand.nextDouble() * 100.0 <= dropChance) {
                        int tearAmt = 1 + rand.nextInt(2);
                        player.getInventory().addItem(itemManager.buildItem("misc_ocean_tear", tearAmt));
                        player.sendMessage("§b[Rơi Đồ] Bạn nhận được " + tearAmt + " Nước Mắt Đại Dương!");
                        vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.COLLECT_OCEAN_TEAR);
                    }

                    // 2. TÍNH EXP NGHỀ CÂU CÁ: Cố định 30 * hệ số kích cỡ
                    double fishExp = 30;
                    if (caughtItemId.contains("small")) fishExp *= 1.25;
                    else if (caughtItemId.contains("med")) fishExp *= 1.75;
                    else if (caughtItemId.contains("large")) fishExp *= 2.5;
                    else if (caughtItemId.contains("mutant")) fishExp *= 4.0;
                    vn.minecraft.rPGCore.jobs.core.JobManager.addExp(player, fishExp);
                }

                player.sendMessage("§b[Câu Cá] Thành công!");
            }

            int amount = gX3 ? 3 : 1;
            ItemStack item = itemManager.buildItem(caughtItemId, amount);

            if (item != null) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    for (ItemStack left : leftover.values()) player.getWorld().dropItemNaturally(player.getLocation(), left);
                    player.sendMessage("§cTúi đồ của bạn đã đầy, vật phẩm bị rơi ra dưới chân!");
                }
                player.updateInventory();

                org.bukkit.Location spawnLoc = hookLoc;
                org.bukkit.entity.Item visualFish = spawnLoc.getWorld().dropItem(spawnLoc, item.asOne());
                visualFish.setPickupDelay(32767);

                double dX = player.getLocation().getX() - spawnLoc.getX();
                double dY = player.getLocation().getY() - spawnLoc.getY();
                double dZ = player.getLocation().getZ() - spawnLoc.getZ();

                org.bukkit.util.Vector vec = new org.bukkit.util.Vector(dX * 0.12, dY * 0.1 + 0.5, dZ * 0.12);
                visualFish.setVelocity(vec);
                player.getWorld().spawnParticle(Particle.SPLASH, spawnLoc, 30, 0.5, 0.5, 0.5, 0.1);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!visualFish.isDead()) {
                        visualFish.remove();
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
                    }
                }, 20L);
            }
        }

        // ===============================================
        // BỘ CÔNG CỤ XÚC XẮC TRỌNG SỐ (WEIGHTED DROPS)
        // ===============================================

        // [MỚI]: Hàm Helper dùng chung để bốc thăm theo trọng số
        private String pickFromWeights(Map<String, Double> weights) {
            double totalWeight = 0;
            for (double w : weights.values()) totalWeight += w; // Tự động tính tổng trọng số

            double roll = rand.nextDouble() * totalWeight;
            for (Map.Entry<String, Double> entry : weights.entrySet()) {
                roll -= entry.getValue();
                if (roll <= 0) return entry.getKey();
            }
            // Fallback an toàn (Mặc định lấy món đầu tiên nếu có lỗi toán học nhỏ)
            return weights.keySet().iterator().next();
        }

        private String getWeightedTreasure() {
            Map<String, Double> weights = new HashMap<>();

            // Bạn chỉ cần khai báo "Tỉ lệ % mong muốn" ở đây.
            // Không cần quan tâm tổng có bằng 100 hay không, hệ thống tự động chia lại tỉ lệ chuẩn!
            weights.put("misc_job_change_card", 1.0);       // Epic
            weights.put("misc_protection_charm", 1.0);      // Epic
            weights.put("bag_weapon_epic", 1.0);            // Epic
            weights.put("bag_armor_epic",1.0);              // Epic
            weights.put("bag_weapon_rare", 2.0);            // Rare
            weights.put("bag_armor_rare", 2.0);             // Rare
            weights.put("misc_lucky_hammer", 6.0);          // Rare
            weights.put("bag_weapon_uncommon", 8.0);        // Uncommon
            weights.put("bag_armor_uncommon", 8.0);         // Uncommon
            weights.put("misc_blacksmith_scrap", 12.0);     // Uncommon
            weights.put("bag_weapon_common", 30.0);         // Common
            weights.put("bag_armor_common", 30.0);          // Common

            // VD: Sau này muốn thêm Sách Cường Hóa, chỉ cần thêm 1 dòng:
            // weights.put("enchant_book_rare", 5.0);

            return pickFromWeights(weights);
        }

        private String getWeightedTrash() {
            Map<String, Double> weights = new HashMap<>();

            weights.put("misc_steel_string", 2.0);          // Uncommon
            weights.put("misc_weird_leather", 5.0);         // Uncommon
            weights.put("misc_armor_shard", 8.0);           // Uncommon
            weights.put("misc_pollen", 15.0);               // Common
            weights.put("monster_flesh", 25.0);             // Common
            weights.put("misc_leather", 20.0);              // Common
            weights.put("misc_soft_string", 25.0);          // Common

            return pickFromWeights(weights);
        }

        private String pickWeightedFish(String[] pool) {
            double totalWeight = 0;
            Map<String, Double> weights = new HashMap<>();

            for (String id : pool) {
                double w = 100.0; // Mặc định rất dễ rơi

                // Trọng số theo kích cỡ
                if (id.contains("mutant")) w = 5.0;         // Đột biến (Khó)
                else if (id.contains("large")) w = 20.0;    // Lớn (Khá)
                else if (id.contains("med")) w = 50.0;      // Vừa (Bình thường)
                else if (id.contains("small")) w = 80.0;    // Nhỏ (Dễ)

                // Phạt thêm nếu là loài cá hiếm
                if (id.contains("golden_koi") || id.contains("shark") || id.contains("time_weaver") ||
                        id.contains("abyss_angler") || id.contains("starfall_ray") || id.contains("toxic_toad")) {
                    w *= 0.1; // Hệ số Cực Hiếm (Giảm 10 lần tỉ lệ)
                } else if (id.contains("marlin") || id.contains("sturgeon") || id.contains("glow_squid")) {
                    w *= 0.3; // Hệ số Hiếm (Giảm 3.3 lần tỉ lệ)
                }

                weights.put(id, w);
                totalWeight += w;
            }

            double roll = rand.nextDouble() * totalWeight;
            for (Map.Entry<String, Double> entry : weights.entrySet()) {
                roll -= entry.getValue();
                if (roll <= 0) return entry.getKey();
            }
            return pool[pool.length - 1]; // Fallback phòng hờ
        }

        // ===============================================
        // CÁC HÀM XỬ LÝ KẾT THÚC VÀ RENDER GIỮ NGUYÊN
        // ===============================================

        private void loseMinigame(String reason) {
            if (secondChance > 0 && rand.nextDouble() * 100.0 < secondChance) {
                player.sendMessage("§a♻ [Lông Phượng Hoàng] Dây cước đã được phục hồi kỳ diệu! Thử lại ngay!");
                player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);

                currentStage = stage1;
                currentNoteIdx = 0;
                lives = (baitItem != null && baitItem.hasItemMeta()) ? 2 : 1;
                comboMutated = false;

                state = GameState.STAGE_1_MEMORIZE;
                renderActionBar(true);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (state == GameState.ENDED) return;
                    state = GameState.STAGE_1_PLAY;
                    renderActionBar(false);
                    startStageTimeout(8000);
                }, 60L);
                return;
            }

            stop();
            activeMinigames.remove(player.getUniqueId());

            if (hook != null && !hook.isDead()) {
                hook.remove();
            }

            consumeRodDurability(player);
            // [MỚI]: BÁO CÁO NHIỆM VỤ CÂU THẤT BẠI
            vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.FISH_FAIL);

            player.sendTitle("§c§lTHẤT BẠI", reason, 5, 40, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
        }

        private void renderActionBar(boolean isMemorize) {
            StringBuilder sb = new StringBuilder("§8Mạng: ");
            for(int i = 0; i < lives; i++) sb.append("§c❤ ");
            sb.append(" §8| Chuỗi: ");

            for (int i = 0; i < currentStage.size(); i++) {
                Note n = currentStage.get(i);
                String symbol;
                String color = n.isLeftClick ? "§b" : "§e";

                if (!isMemorize && n.isHidden && i >= currentNoteIdx) {
                    if (n.isMutatedHidden) {
                        symbol = n.isDifferentFromPrev ? "[⇌]" : "[=]";
                        color = n.isDifferentFromPrev ? "§c" : "§a";
                    } else {
                        symbol = "[?]";
                        color = "§7";
                    }
                } else {
                    symbol = n.isLeftClick ? "[T]" : "[P]";
                }

                if (i < currentNoteIdx) sb.append("§a✔ ");
                else if (i == currentNoteIdx) {
                    if (isMemorize) sb.append(color).append(symbol).append(" ");
                    else sb.append(color).append("§n").append(symbol).append("§r ");
                }
                else sb.append(color).append(symbol).append(" ");
            }
            player.sendActionBar(sb.toString());
        }
    }
}