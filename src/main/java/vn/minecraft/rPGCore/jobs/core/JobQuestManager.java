package vn.minecraft.rPGCore.jobs.core;

import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.jobs.quests.QuestAction;
import vn.minecraft.rPGCore.jobs.quests.RPGQuest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JobQuestManager {

    // ==========================================
    // HÀM ROLL NHIỆM VỤ NGẪU NHIÊN KHI QUA NGÀY/TUẦN
    // ==========================================
    public static void rollNewQuests(Player player, boolean isDaily, boolean isWeekly) {
        PlayerJobData data = JobManager.getPlayerData(player);
        RPGJob currentJob = JobManager.getPlayerJob(player);
        if (currentJob == null) return;

        Map<String, RPGQuest> allQuests = currentJob.getQuests();
        if (allQuests == null || allQuests.isEmpty()) return;

        if (isDaily) {
            // Lọc ra các quest có chữ "DAILY", xáo trộn và lấy 3 cái đầu tiên
            List<String> dailyPool = allQuests.keySet().stream().filter(k -> k.contains("DAILY")).collect(Collectors.toList());
            Collections.shuffle(dailyPool);
            data.setActiveDailyQuests(dailyPool.subList(0, Math.min(3, dailyPool.size())));
            data.setDailyMilestone(0); // Reset điểm năng động

            // Xóa dữ liệu tiến độ cũ
            data.getQuestProgressMap().keySet().removeIf(k -> k.contains("DAILY"));
            data.getQuestClaimedMap().keySet().removeIf(k -> k.contains("DAILY"));
        }

        if (isWeekly) {
            // Tương tự cho Quest Tuần
            List<String> weeklyPool = allQuests.keySet().stream().filter(k -> k.contains("WEEKLY")).collect(Collectors.toList());
            Collections.shuffle(weeklyPool);
            data.setActiveWeeklyQuests(weeklyPool.subList(0, Math.min(3, weeklyPool.size())));

            data.getQuestProgressMap().keySet().removeIf(k -> k.contains("WEEKLY"));
            data.getQuestClaimedMap().keySet().removeIf(k -> k.contains("WEEKLY"));
        }
    }

    // ==========================================
    // HÀM XỬ LÝ TIẾN ĐỘ (ĐÃ LỌC THEO ACTIVE QUESTS)
    // ==========================================
    public static void handleAction(Player player, QuestAction action, Object... args) {
        PlayerJobData data = JobManager.getPlayerData(player);
        RPGJob currentJob = JobManager.getPlayerJob(player);
        if (currentJob == null) return;

        Map<String, RPGQuest> jobQuests = currentJob.getQuests();
        if (jobQuests == null || jobQuests.isEmpty()) return;

        // Gộp danh sách Nhiệm vụ đang mở của người chơi này
        List<String> activeQuests = new ArrayList<>();
        if (data.getActiveDailyQuests() != null) activeQuests.addAll(data.getActiveDailyQuests());
        if (data.getActiveWeeklyQuests() != null) activeQuests.addAll(data.getActiveWeeklyQuests());

        for (String questId : activeQuests) {
            RPGQuest quest = jobQuests.get(questId);
            if (quest == null) continue;

            // Nếu đã nhận thưởng (xong rồi) thì bỏ qua
            if (data.isQuestClaimed(questId)) continue;
            // Nếu đã đầy cây tiến độ (chờ bấm nhận) thì bỏ qua
            if (data.getQuestProgress(questId) >= quest.getGoal()) continue;

            // Đưa qua màng lọc điều kiện của Quest
            if (quest.checkCondition(action, args)) {
                int currentProgress = data.getQuestProgress(questId) + 1;
                data.setQuestProgress(questId, currentProgress);

                // HOÀN THÀNH NHIỆM VỤ
                if (currentProgress >= quest.getGoal()) {
                    // Không phát thưởng luôn nữa, chỉ thông báo hoàn thành để người chơi tự vào Bảng Nhiệm Vụ nhận thưởng Mốc
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    player.sendMessage("§e§l[!] §fBạn đã hoàn thành mục tiêu: §a" + quest.getName());

                    if (questId.contains("DAILY")) {
                        data.addDailyMilestone(1);
                        player.sendMessage("§b+1 Điểm Năng Động Ngày! §7(Hãy kiểm tra Bảng Nhiệm Vụ)");
                    }
                }
            }
        }
    }
    // ==========================================
    // KIỂM TRA VÀ RESET TỰ ĐỘNG (AUTO-RESET)
    // ==========================================
    public static void checkAndResetQuests(Player player) {
        PlayerJobData data = JobManager.getPlayerData(player);
        if (data.getJobId().equals("NONE")) return;

        long now = System.currentTimeMillis();

        // Điều kiện Roll Ngày: Đã qua 00:00 HOẶC chưa có quest nào
        boolean rollDaily = isNewDay(data.getLastDailyReset()) || data.getActiveDailyQuests() == null || data.getActiveDailyQuests().isEmpty();

        // Điều kiện Roll Tuần: Đã qua 00:00 Thứ 2 HOẶC chưa có quest nào
        boolean rollWeekly = isNewWeek(data.getLastWeeklyReset()) || data.getActiveWeeklyQuests() == null || data.getActiveWeeklyQuests().isEmpty();

        if (rollDaily || rollWeekly) {
            rollNewQuests(player, rollDaily, rollWeekly);

            if (rollDaily) {
                data.setLastDailyReset(now);
                player.sendMessage("§e§l[!] §fNhiệm vụ Ngày của bạn đã được làm mới!");
            }
            if (rollWeekly) {
                data.setLastWeeklyReset(now);
                player.sendMessage("§d§l[!] §fThử Thách Tuần của bạn đã được làm mới!");
            }
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }

    private static boolean isNewDay(long lastTime) {
        java.time.LocalDate lastDate = java.time.Instant.ofEpochMilli(lastTime).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        java.time.LocalDate nowDate = java.time.LocalDate.now();
        return nowDate.isAfter(lastDate);
    }

    private static boolean isNewWeek(long lastTime) {
        java.time.ZonedDateTime last = java.time.Instant.ofEpochMilli(lastTime).atZone(java.time.ZoneId.systemDefault());
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now();

        // Tìm ngày Thứ 2 gần nhất (Lúc 00:00)
        java.time.ZonedDateTime currentMonday = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .truncatedTo(java.time.temporal.ChronoUnit.DAYS);

        // Nếu lần reset cuối cùng nằm TRƯỚC Thứ 2 tuần này -> Bắt buộc reset
        return last.isBefore(currentMonday);
    }
}