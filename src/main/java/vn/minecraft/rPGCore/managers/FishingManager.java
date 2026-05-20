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

    // [CẬP NHẬT] Thêm tham số org.bukkit.entity.FishHook hook
    public void startMinigame(Player player, String rodId, ItemStack baitItem, org.bukkit.Location hookLoc, org.bukkit.entity.FishHook hook) {
        MuseDashMinigame minigame = new MuseDashMinigame(player, rodId, baitItem, hookLoc, hook);
        activeMinigames.put(player.getUniqueId(), minigame);
        minigame.start();
    }

    private enum GameState { STAGE_1_DEMO, STAGE_1_PLAY, SURPRISE_WARN, STAGE_2_DEMO, STAGE_2_PLAY, ENDED }
    private class Note { boolean isLeftClick; long delayMs; public Note(boolean isLeftClick, long delayMs) { this.isLeftClick = isLeftClick; this.delayMs = delayMs; } }

    private class MuseDashMinigame {
        Player player; String rodId; ItemStack baitItem; org.bukkit.Location hookLoc;
        org.bukkit.entity.FishHook hook; // Lưu tham chiếu đến phao câu
        GameState state; boolean isBigFish;
        List<Note> stage1 = new ArrayList<>(); List<Note> stage2 = new ArrayList<>(); List<Note> currentStage;
        int currentNoteIdx = 0; long lastClickMs = 0; long debounceMs = 0; long startTime = 0;

        long toleranceMs = 300;
        double trashMod = 0, treasureMod = 0, secondChance = 0;
        boolean gMonster = false, gX3 = false, gTreasure = false, isAutoCatch = false;

        public MuseDashMinigame(Player player, String rodId, ItemStack baitItem, org.bukkit.Location hookLoc, org.bukkit.entity.FishHook hook) {
            this.player = player; this.rodId = rodId; this.baitItem = baitItem; this.hookLoc = hookLoc;
            this.hook = hook; // Khởi tạo biến hook

            double bigFishChance = 0.05;
            String currentBiome = getCurrentBiomeType(hookLoc);

            if (baitItem != null && baitItem.hasItemMeta()) {
                org.bukkit.persistence.PersistentDataContainer pdc = baitItem.getItemMeta().getPersistentDataContainer();

                bigFishChance += getStat(pdc, "bait_big_fish", currentBiome) / 100.0;
                toleranceMs += (long) getStat(pdc, "bait_tolerance", currentBiome);
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

            int s1Len = 3 + rand.nextInt(2);
            for (int i = 0; i < s1Len; i++) stage1.add(new Note(rand.nextBoolean(), 400 + rand.nextInt(3) * 100));
            if (isBigFish) {
                int s2Len = 6 + rand.nextInt(3);
                for (int i = 0; i < s2Len; i++) stage2.add(new Note(rand.nextBoolean(), 200 + rand.nextInt(4) * 50));
            }
        }

        private double getStat(org.bukkit.persistence.PersistentDataContainer pdc, String baseKey, String biome) {
            double total = 0;
            if (baseKey.equals("bait_tolerance")) {
                total += pdc.getOrDefault(getKey(baseKey, "ALL"), PersistentDataType.INTEGER, 0);
                total += pdc.getOrDefault(getKey(baseKey, biome), PersistentDataType.INTEGER, 0);
            } else {
                total += pdc.getOrDefault(getKey(baseKey, "ALL"), PersistentDataType.DOUBLE, 0.0);
                total += pdc.getOrDefault(getKey(baseKey, biome), PersistentDataType.DOUBLE, 0.0);
            }
            return total;
        }

        public void start() {
            if (isAutoCatch) {
                player.sendMessage("§b✨ [Bùa Lười Biếng] §fĐã tự động kéo cá lên bờ!");
                winMinigame(isBigFish);
                return;
            }

            startTime = System.currentTimeMillis();
            state = GameState.STAGE_1_DEMO;
            currentStage = stage1;
            player.sendTitle("§e♪ Lắng nghe... ♪", (baitItem != null ? "§d(Có Mồi Đặc Chế)" : "§7(Không Mồi)"), 5, 40, 5);
            playDemoSequence();
        }

        private void playDemoSequence() {
            currentNoteIdx = 0;
            renderActionBar(true);
            playNextNote(0);
        }

        private void playNextNote(int idx) {
            if (activeMinigames.get(player.getUniqueId()) != this) return;
            if (idx >= currentStage.size()) { switchToPlayerTurn(); return; }

            Note n = currentStage.get(idx);
            if (n.isLeftClick) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.2f, 1.5f);
                player.getWorld().spawnParticle(Particle.NOTE, player.getLocation().add(0, 2.2, 0), 1, 0.2, 0, 0.2, 5);
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.5f, 0.5f);
                player.getWorld().spawnParticle(Particle.NOTE, player.getLocation().add(0, 2.2, 0), 1, 0.2, 0, 0.2, 1);
            }

            currentNoteIdx = idx + 1;
            renderActionBar(true);

            long nextDelay = (idx + 1 < currentStage.size()) ? currentStage.get(idx + 1).delayMs / 50L : 10L;
            Bukkit.getScheduler().runTaskLater(plugin, () -> playNextNote(idx + 1), nextDelay);
        }

        private void switchToPlayerTurn() {
            currentNoteIdx = 0;
            if (state == GameState.STAGE_1_DEMO) state = GameState.STAGE_1_PLAY;
            else if (state == GameState.STAGE_2_DEMO) state = GameState.STAGE_2_PLAY;

            player.sendTitle("§a§lTỚI LƯỢT BẠN!", "§fLặp lại nhịp điệu (Trái/Phải)", 2, 30, 5);
            renderActionBar(false);
            startTimeoutTask(2000);
        }

        public void playerClick(boolean isLeftClick) {
            long now = System.currentTimeMillis();
            if (now - debounceMs < 50) return; debounceMs = now;
            if (now - startTime < 800) return;

            if (state == GameState.STAGE_1_DEMO || state == GameState.STAGE_2_DEMO) { loseMinigame("§cQuá vội vàng! Phải chờ gõ nhịp xong."); return; }
            if (state != GameState.STAGE_1_PLAY && state != GameState.STAGE_2_PLAY) return;

            if (currentNoteIdx == 0) { lastClickMs = now; checkNoteAccuracy(isLeftClick, 0); return; }

            long diffMs = now - lastClickMs;
            long targetMs = currentStage.get(currentNoteIdx).delayMs;

            if (Math.abs(diffMs - targetMs) > toleranceMs) loseMinigame("§cSai nhịp điệu! Cá đã quẫy đứt dây...");
            else { lastClickMs = now; checkNoteAccuracy(isLeftClick, Math.abs(diffMs - targetMs)); }
        }

        private void checkNoteAccuracy(boolean isLeftClick, long errorMs) {
            Note targetNote = currentStage.get(currentNoteIdx);
            if (targetNote.isLeftClick != isLeftClick) { loseMinigame("§cSai thao tác!"); return; }

            if (errorMs < 100 && currentNoteIdx > 0) player.sendTitle("", "§e§lPERFECT!", 0, 10, 5);
            else if (currentNoteIdx > 0) player.sendTitle("", "§a§lGOOD!", 0, 10, 5);

            if (isLeftClick) player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.2f, 1.5f);
            else player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.5f, 0.5f);

            currentNoteIdx++;
            renderActionBar(false);

            if (currentNoteIdx >= currentStage.size()) handleStageComplete();
            else startTimeoutTask(currentStage.get(currentNoteIdx).delayMs + toleranceMs + 500);
        }

        private void handleStageComplete() {
            // Hủy task cũ tránh chạy ngầm
            stop();
            if (state == GameState.STAGE_1_PLAY) {
                if (isBigFish) {
                    state = GameState.SURPRISE_WARN;
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.8f);
                    player.sendTitle("§c§l!!! CÁ LỚN QUẪY ĐUÔI !!!", "§fChuẩn bị Combo liên hoàn...", 5, 40, 10);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (state == GameState.ENDED) return;
                        state = GameState.STAGE_2_DEMO; currentStage = stage2; playDemoSequence();
                    }, 50L);
                } else winMinigame(false);
            } else if (state == GameState.STAGE_2_PLAY) winMinigame(true);
        }

        private void startTimeoutTask(long msLimit) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (state == GameState.STAGE_1_PLAY || state == GameState.STAGE_2_PLAY) {
                    loseMinigame("§cQuá chậm! Cá đã bơi mất.");
                }
            }, msLimit / 50L);
        }

        public void stop() {
            state = GameState.ENDED;
        }

        private void winMinigame(boolean isBigReward) {
            stop();
            activeMinigames.remove(player.getUniqueId());

            // [CẬP NHẬT] Xóa phao câu khi kết thúc
            if (hook != null && !hook.isDead()) {
                hook.remove();
            }

            player.sendTitle("§a§lHOÀN HẢO!", "§fKéo cá thành công!", 5, 30, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

            String caughtItemId;
            double roll = rand.nextDouble() * 100.0;
            double baseTrash = 30.0 + trashMod;
            double baseTreasure = 2.0 + treasureMod;

            if (gTreasure || roll < baseTreasure) {
                String[] treasurePool = {"bag_weapon_common", "bag_armor_common", "misc_job_change_card"};
                caughtItemId = treasurePool[rand.nextInt(treasurePool.length)];
                player.sendMessage("§6§l[Kho Báu] §eBạn vừa kéo lên một vật phẩm giá trị!");
            } else if (!gMonster && roll < baseTrash) {
                String[] trashPool = {"monster_flesh", "misc_soft_string", "misc_leather", "misc_pollen"};
                caughtItemId = trashPool[rand.nextInt(trashPool.length)];
                player.sendMessage("§7§l[Mắc Cạn] §8Chỉ là một món đồ vụn vặt trôi dạt...");
            } else {
                String[] normalPool = {"fish_cod_small", "fish_cod_med", "fish_salmon_small", "fish_tropical"};
                String[] bigPool = {"fish_cod_large", "fish_salmon_med", "fish_salmon_large"};
                caughtItemId = isBigReward ? bigPool[rand.nextInt(bigPool.length)] : normalPool[rand.nextInt(normalPool.length)];
                player.sendMessage("§b§l[Câu Cá] §fTuyệt vời!");
            }

            int amount = gX3 ? 3 : 1;
            ItemStack item = itemManager.buildItem(caughtItemId, amount);
            if (item != null) {
                org.bukkit.Location dropLoc = player.getLocation();
                org.bukkit.entity.Item droppedItem = dropLoc.getWorld().dropItemNaturally(dropLoc, item);

                // Set owner cho item để tránh bị ks
                droppedItem.setOwner(player.getUniqueId());
                // Không cho nhặt ngay để có animation rớt đồ (mặc định của mc)

                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, dropLoc.add(0, 1, 0), 10);
            }
        }

        private void loseMinigame(String reason) {
            if (secondChance > 0 && rand.nextDouble() * 100.0 < secondChance) {
                player.sendMessage("§a♻ [Lông Phượng Hoàng] §fDây cước đã được phục hồi kỳ diệu! Thử lại ngay!");
                player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
                state = GameState.STAGE_1_DEMO; currentStage = stage1;
                playDemoSequence();
                return;
            }

            stop();
            activeMinigames.remove(player.getUniqueId());

            // [CẬP NHẬT] Xóa phao câu khi thất bại
            if (hook != null && !hook.isDead()) {
                hook.remove();
            }

            player.sendTitle("§c§lTHẤT BẠI", reason, 5, 40, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
        }

        private void renderActionBar(boolean isDemo) {
            StringBuilder sb = new StringBuilder("§8Nhịp: ");
            for (int i = 0; i < currentStage.size(); i++) {
                Note n = currentStage.get(i);
                String symbol = n.isLeftClick ? "[T]" : "[P]";
                String color = n.isLeftClick ? "§b" : "§e";

                if (isDemo) {
                    if (i == currentNoteIdx) sb.append("§f§l").append(symbol).append(" ");
                    else if (i < currentNoteIdx) sb.append("§7").append(symbol).append(" ");
                    else sb.append(color).append(symbol).append(" ");
                } else {
                    if (i < currentNoteIdx) sb.append("§a✔ ");
                    else if (i == currentNoteIdx) sb.append("§f§n").append(symbol).append("§r ");
                    else sb.append(color).append(symbol).append(" ");
                }
            }
            player.sendActionBar(sb.toString());
        }
    }
}