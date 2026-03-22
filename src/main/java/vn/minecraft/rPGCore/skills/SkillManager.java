package vn.minecraft.rPGCore.skills;

import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.skills.archer.*;
import vn.minecraft.rPGCore.skills.assassin.*;
import vn.minecraft.rPGCore.skills.summoner.SummonFusionSkill;
import vn.minecraft.rPGCore.skills.summoner.SummonWolfSkill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillManager {
    private final Map<String, ISkill> registeredSkills = new HashMap<>();
    private final JavaPlugin plugin;

    public SkillManager(JavaPlugin plugin) {
        this.plugin = plugin;
        // Nơi đăng ký mọi kỹ năng của Server
        registerSkill(new DoubleShotSkill(plugin));
        registerSkill(new TripleShotSkill(plugin));
        registerSkill(new QuadrupleShotSkill(plugin));
        registerSkill(new RapidFireSkill(plugin));
        registerSkill(new ArrowRainSkill(plugin));
        registerSkill(new HeavyBlowShootSkill(plugin));
        registerSkill(new ArrowCraftingSkill(plugin));
        registerSkill(new DoubleSlashSkill(plugin));
        registerSkill(new SmokeBombSkill(plugin));
        registerSkill(new PoisonDartSkill(plugin));
        registerSkill(new FatalBlowSkill(plugin));
        registerSkill(new ShadowStepSkill(plugin));
        registerSkill(new ShadowDashSkill(plugin));
        registerSkill(new PhantomBladesSkill(plugin));
        registerSkill(new BloodlustStrikeSkill(plugin));
        registerSkill(new ShadowDecoySkill(plugin));
        registerSkill(new KillingIntentSkill(plugin));
        registerSkill(new SummonWolfSkill(plugin));
        registerSkill(new SummonFusionSkill(plugin));
    }

    public void registerSkill(ISkill skill) {
        registeredSkills.put(skill.getId(), skill);
    }

    public ISkill getSkill(String id) {
        return registeredSkills.get(id);
    }

    // Lọc ra các kỹ năng dành riêng cho class của người chơi để vẽ lên UI
    public List<ISkill> getSkillsForClass(String classId) {
        List<ISkill> list = new ArrayList<>();
        for (ISkill skill : registeredSkills.values()) {
            if (skill.getRequiredClass().equalsIgnoreCase(classId)) {
                list.add(skill);
            }
        }
        return list;
    }
}