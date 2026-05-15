package vn.minecraft.rPGCore.jobs.quests;

import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.jobs.core.JobManager;

public abstract class RPGQuest {
    protected String id;
    protected String name;
    protected String description;
    protected int goal;
    protected QuestAction triggerAction;

    public RPGQuest(String id, String name, String description, int goal, QuestAction triggerAction) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.goal = goal;
        this.triggerAction = triggerAction;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getGoal() { return goal; }
    public String getDescription() { return description; }

    // Hàm kiểm tra xem hành động của người chơi có khớp với Nhiệm vụ không
    public abstract boolean checkCondition(QuestAction action, Object... args);

    // ==========================================
    // TỰ ĐỘNG TÍNH TOÁN PHẦN THƯỞNG DỰA TRÊN ĐỘ KHÓ
    // ==========================================
    public int getDynamicExpReward(int playerJobLevel) {
        // Ví dụ: Độ khó (Goal) càng cao, level càng cao -> EXP càng nhiều
        return (goal * 15) + (playerJobLevel * 10);
    }

    public int getDynamicScrapReward(int playerJobLevel) {
        // Tự động cho Mạt thợ rèn hoặc vật phẩm tương ứng
        return Math.max(1, goal / 5);
    }

    // Hàm phát quà chung
    public void giveRewards(Player player) {
        int level = JobManager.getPlayerJobLevel(player);
        int exp = getDynamicExpReward(level);

        JobManager.addExp(player, exp);
        // Có thể gọi ItemManager để add mạt thợ rèn / thẻ đổi nghề ở đây
        player.sendMessage("§a§l[Nhiệm Vụ] §fBạn đã nhận §e" + exp + " EXP §fnghề từ nhiệm vụ §b" + name + "§f!");
    }
}