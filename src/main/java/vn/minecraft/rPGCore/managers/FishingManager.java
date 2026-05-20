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

    public boolean isInMinigame(Player player) { return activeMinigames.containsKey(player.getUniqueId()); }
    public void handleInteract(Player player, boolean isLeftClick) { if (isInMinigame(player)) activeMinigames.get(player.getUniqueId()).playerClick(isLeftClick); }
    public void cancelMinigame(Player player) { if (isInMinigame(player)) { activeMinigames.get(player.getUniqueId()).stop(); activeMinigames.remove(player.getUniqueId()); } }

    public void startMinigame(Player player, String rodId, ItemStack baitItem, org.bukkit.Location hookLoc, org.bukkit.entity.FishHook hook) {
        // [ĐÃ SỬA]: Trừ mồi 1 lần duy nhất và chính xác khi game bắt đầu
        if (baitItem != null && player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (offHand.isSimilar(baitItem)) {
                offHand.setAmount(offHand.getAmount() - 1);
            }
        }

        MuseDashMinigame minigame = new MuseDashMinigame(player, rodId, baitItem, hookLoc, hook);
        activeMinigames.put(player.getUniqueId(), minigame);
        minigame.start();
    }

    // [LỐI CHƠI MỚI]: Bỏ hoàn toàn Demo, chỉ còn Play
    private enum GameState { STAGE_1_MEMORIZE, STAGE_1_PLAY, SURPRISE_WARN, STAGE_2_MEMORIZE, STAGE_2_PLAY, ENDED }

    private class Note {
        boolean isLeftClick;
        boolean isHidden = false;
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

            if (rodId.endsWith("_2")) bigFishChance += 0.05;
            else if (rodId.endsWith("_3")) bigFishChance += 0.15;
            if (hookLoc.getWorld().hasStorm()) bigFishChance += 0.15;
            if (hookLoc.getY() < 50) bigFishChance += 0.10;

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
            return total;
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
            // [FIX]: Chặn đứng mọi click lỡ tay trong 3 giây ghi nhớ
            if (state != GameState.STAGE_1_PLAY && state != GameState.STAGE_2_PLAY) return;

            long now = System.currentTimeMillis();
            if (now - debounceMs < 100) return;
            debounceMs = now;

            Note targetNote = currentStage.get(currentNoteIdx);

            if (targetNote.isLeftClick == isLeftClick) {
                if (isLeftClick) player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.2f, 1.5f);
                else player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.5f, 0.5f);

                currentNoteIdx++;

                // Cơ chế Phá đám: Đang spam thì đổi phím
                if (isBigFish && !comboMutated && currentNoteIdx == (currentStage.size() / 2)) {
                    comboMutated = true;
                    for (int i = currentNoteIdx; i < currentStage.size(); i++) {
                        Note mutatedNote = new Note(rand.nextBoolean());
                        if (rand.nextDouble() < 0.35) mutatedNote.isHidden = true;
                        currentStage.set(i, mutatedNote);
                    }
                    player.sendTitle("", "§c§l⚠️ ĐỔI PHÍM ĐỘT NGỘT! ⚠️", 0, 20, 5);
                    player.playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.7f, 1.2f);
                }

                renderActionBar(false); // Đang Play nên giấu phím

                if (currentNoteIdx >= currentStage.size()) {
                    handleStageComplete();
                }
            } else {
                deductLife("§cSai phím!");
            }
        }

        private void deductLife(String reason) {
            lives--;
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

            player.sendTitle("§a§lHOÀN HẢO!", "§fKéo cá thành công!", 5, 30, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

            String caughtItemId;
            double roll = rand.nextDouble() * 100.0;
            double baseTrash = 30.0 + trashMod;
            double baseTreasure = 2.0 + treasureMod;

            if (gTreasure || roll < baseTreasure) {
                String[] treasurePool = {"bag_weapon_common", "bag_armor_common", "misc_job_change_card"};
                caughtItemId = treasurePool[rand.nextInt(treasurePool.length)];
                player.sendMessage("§6[Kho Báu] Bạn vừa kéo lên một vật phẩm giá trị!");
            } else if (!gMonster && roll < baseTrash) {
                String[] trashPool = {"monster_flesh", "misc_soft_string", "misc_leather", "misc_pollen"};
                caughtItemId = trashPool[rand.nextInt(trashPool.length)];
                player.sendMessage("§7[Mắc Cạn] Chỉ là một món đồ vụn vặt trôi dạt...");
            } else {
                String[] normalPool = {"fish_cod_small", "fish_cod_med", "fish_salmon_small", "fish_tropical"};
                String[] bigPool = {"fish_cod_large", "fish_salmon_med", "fish_salmon_large"};
                caughtItemId = isBigReward ? bigPool[rand.nextInt(bigPool.length)] : normalPool[rand.nextInt(normalPool.length)];
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

        private void loseMinigame(String reason) {
            if (secondChance > 0 && rand.nextDouble() * 100.0 < secondChance) {
                player.sendMessage("§a♻ [Lông Phượng Hoàng] Dây cước đã được phục hồi kỳ diệu! Thử lại ngay!");
                player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);

                currentStage = stage1;
                currentNoteIdx = 0;
                lives = (baitItem != null && baitItem.hasItemMeta()) ? 2 : 1;
                comboMutated = false;

                // [FIX]: Trở về Ghi nhớ Stage 1
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

            player.sendTitle("§c§lTHẤT BẠI", reason, 5, 40, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
        }

        private void renderActionBar(boolean isMemorize) {
            StringBuilder sb = new StringBuilder("§8Mạng: ");
            for(int i = 0; i < lives; i++) sb.append("§c❤ ");
            sb.append(" §8| Chuỗi: ");

            for (int i = 0; i < currentStage.size(); i++) {
                Note n = currentStage.get(i);

                // [FIX]: Nếu đang ghi nhớ (isMemorize) thì show phím thật, nếu đang Play thì mới hiện [?]
                String symbol = (!isMemorize && n.isHidden && i >= currentNoteIdx) ? "[?]" : (n.isLeftClick ? "[T]" : "[P]");
                String color = n.isLeftClick ? "§b" : "§e";

                if (i < currentNoteIdx) sb.append("§a✔ ");
                else if (i == currentNoteIdx) {
                    if (isMemorize) sb.append(color).append(symbol).append(" ");
                    else sb.append("§f§n").append(symbol).append("§r ");
                }
                else sb.append(color).append(symbol).append(" ");
            }
            player.sendActionBar(sb.toString());
        }
    }
}