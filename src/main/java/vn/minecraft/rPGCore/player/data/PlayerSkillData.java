package vn.minecraft.rPGCore.player.data;

import java.util.HashMap;
import java.util.Map;

public class PlayerSkillData {
    private int skillPoints = 0;
    private final Map<String, Integer> skills = new HashMap<>();
    private final Map<String, String> binds = new HashMap<>();

    public PlayerSkillData() {}

    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int sp) { this.skillPoints = sp; }
    public void addSkillPoints(int amount) { this.skillPoints += amount; }

    public int getSkillLevel(String skillId) { return skills.getOrDefault(skillId, 0); }
    public void setSkillLevel(String skillId, int level) { skills.put(skillId, level); }
    public Map<String, Integer> getSkills() { return skills; }

    public String getSkillByCombo(String combo) { return binds.get(combo); }
    public void bindCombo(String combo, String skillId) { binds.put(combo, skillId); }
    public void unbindCombo(String combo) { binds.remove(combo); }
    public Map<String, String> getBinds() { return binds; }

    // ==========================================
    // CÁC HÀM DỌN DẸP DỮ LIỆU KHI ĐỔI CLASS
    // ==========================================
    public void clearAllSkills() {
        this.skills.clear();
    }

    public void clearAllBinds() {
        this.binds.clear();
    }
}