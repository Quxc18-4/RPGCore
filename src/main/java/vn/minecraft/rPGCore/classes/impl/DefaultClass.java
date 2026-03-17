package vn.minecraft.rPGCore.classes.impl;

import org.bukkit.Material;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.classes.ResourceType;

import java.util.List;

public class DefaultClass extends RPGClass {

    public DefaultClass(String id, String displayName, Material icon, List<String> description,
                        double maxHealth, double maxResource, ResourceType resourceType,
                        double regenRate, boolean isEnchanted) {
        super(id, displayName, icon, description, maxHealth, maxResource, resourceType, regenRate, isEnchanted);
    }

    // Class mặc định không có kỹ năng đặc thù nào cả, chỉ dùng thông số cơ bản
}