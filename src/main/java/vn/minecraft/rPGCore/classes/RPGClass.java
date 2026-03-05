package vn.minecraft.rPGCore.classes;

import org.bukkit.Material;
import vn.minecraft.rPGCore.classes.ResourceType;

import java.util.List;

public class RPGClass {
    private final String id;
    private final String displayName;
    private final Material icon;
    private final List<String> description;
    private final double maxHealth;
    private final double maxResource;
    private final ResourceType resourceType;
    private final double regenRate; // Tốc độ hồi phục mỗi giây
    private final boolean isEnchanted;

    public RPGClass(String id, String displayName, Material icon, List<String> description,
                    double maxHealth, double maxResource, ResourceType resourceType,
                    double regenRate, boolean isEnchanted) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.maxHealth = maxHealth;
        this.maxResource = maxResource;
        this.resourceType = resourceType;
        this.regenRate = regenRate;
        this.isEnchanted = isEnchanted;
    }

    // Tạo các hàm Getter (tương tự như getDisplayName(), getMaxHealth()...)
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public List<String> getDescription() { return description; }
    public double getMaxHealth() { return maxHealth; }
    public double getMaxResource() { return maxResource; }
    public ResourceType getResourceType() { return resourceType; }
    public double getRegenRate() { return regenRate; }
    public boolean isEnchanted() { return isEnchanted; }
}