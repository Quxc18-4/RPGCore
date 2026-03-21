package vn.minecraft.rPGCore.classes;

public enum ResourceType {
    STAMINA("Stamina", org.bukkit.boss.BarColor.YELLOW),
    MANA("Mana", org.bukkit.boss.BarColor.BLUE);

    private final String name;
    private final org.bukkit.boss.BarColor color;

    ResourceType(String name, org.bukkit.boss.BarColor color) {
        this.name = name;
        this.color = color;
    }

    public String getName() { return name; }
    public org.bukkit.boss.BarColor getColor() { return color; }
}