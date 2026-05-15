package vn.minecraft.rPGCore.jobs.quests;

import java.util.function.Predicate;

public class AdvancedQuest extends RPGQuest {

    // Điều kiện nâng cao (VD: args[0] là Level đồ phải >= 9)
    private final Predicate<Object[]> extraCondition;

    public AdvancedQuest(String id, String name, String description, int goal, QuestAction triggerAction, Predicate<Object[]> extraCondition) {
        super(id, name, description, goal, triggerAction);
        this.extraCondition = extraCondition;
    }

    @Override
    public boolean checkCondition(QuestAction action, Object... args) {
        // 1. Phải đúng loại hành động (VD: Đập đồ)
        if (this.triggerAction != action) return false;

        // 2. Nếu không có điều kiện phụ -> Mặc định đúng (Quest Cơ bản)
        if (extraCondition == null) return true;

        // 3. Kiểm tra điều kiện nâng cao (Quest Nâng cao)
        return extraCondition.test(args);
    }
}