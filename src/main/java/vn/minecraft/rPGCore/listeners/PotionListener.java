package vn.minecraft.rPGCore.listeners;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.brewing.PotionData;
import vn.minecraft.rPGCore.managers.AlchemistBuffManager;
import vn.minecraft.rPGCore.managers.BrewingManager;
import vn.minecraft.rPGCore.managers.ToxicityManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.UUID;

public class PotionListener implements Listener {

    private final BrewingManager brewingManager;
    private final ToxicityManager toxManager;
    private final AlchemistBuffManager buffManager;
    private final NamespacedKey UUID_KEY;

    public PotionListener(JavaPlugin plugin, BrewingManager brewingManager) {
        this.brewingManager = brewingManager;
        this.toxManager = ((RPGCore) plugin).getToxicityManager();
        this.buffManager = ((RPGCore) plugin).getAlchemistBuffManager();
        this.UUID_KEY = new NamespacedKey(plugin, "potion_uuid");
    }

    @EventHandler
    public void onPotionDrink(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.POTION || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(UUID_KEY, PersistentDataType.STRING)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        String potionUuid = meta.getPersistentDataContainer().get(UUID_KEY, PersistentDataType.STRING);

        PotionData data = brewingManager.loadPotionFromFile(potionUuid);
        if (data == null) {
            player.sendMessage("§cBình thuốc này đã hết hạn hoặc bị lỗi!");
            item.setAmount(item.getAmount() - 1);
            return;
        }

        item.setAmount(item.getAmount() - 1);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));

        if (data.isRuined) {
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1f, 1f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 0.5f);
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 1));
            player.sendMessage("§cỌe... Đau bụng quá!");
            brewingManager.deletePotionFile(potionUuid);
            return;
        }

        double effectMultiplier = 1.0;

        // Chỉ phạt nếu Sốc Thuốc
        if (toxManager.isOverdosed(playerUuid)) {
            effectMultiplier *= 0.5;
            player.sendMessage("§cCơ thể bạn đang bị Sốc Thuốc do uống quá nhiều! (Giảm 50% hiệu quả)");
        }

        toxManager.addToxicity(playerUuid, data.toxicityPoints);

        if (data.isManaPotion) {
            toxManager.addManaSickness(playerUuid);
            player.sendMessage("§5Hấp thụ Ma lực nhân tạo gây ức chế mạch phép thuật! Sát thương phép bị giảm.");
        }

        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1f, 1f);

        // ==========================================
        // 1. ÁP DỤNG HÌNH PHẠT HUYẾT TẾ NẾU CÓ TAG
        // ==========================================
        if (data.flavorTags.contains("§4[Huyết Tế]")) {
            double ripHp = player.getHealth() * 0.9;
            vn.minecraft.rPGCore.managers.StatusEffectManager.applyTrueDamage(player, ripHp); //[cite: 6]
            vn.minecraft.rPGCore.managers.StatusEffectManager.applyAntiHeal(player, 300); // 15s (300 ticks)[cite: 6]
        }

        // ==========================================
        // 2. TRUYỀN DƯỢC LỰC CHO BẢN THÂN
        // ==========================================
        applyDose(player, data, effectMultiplier);
        player.sendMessage("§a[+] Đã hấp thụ Dược lực thành công.");

        // ==========================================
        // 3. TRUYỀN DƯỢC LỰC CHO ĐỒNG MINH (LAN TỎA)
        // ==========================================
        if (data.flavorTags.contains("§a[Lan Tỏa Đồng Minh]")) {
            double radius = 5.0; // 5 block
            for (org.bukkit.entity.Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof Player) {
                    Player ally = (Player) entity;

                    // Nếu KHÔNG THỂ target -> Đó là Đồng Minh
                    if (!vn.minecraft.rPGCore.managers.CombatManager.canTarget(player, ally)) {
                        applyDose(ally, data, effectMultiplier);
                        ally.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, ally.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0);
                        ally.sendMessage("§a§l[!] §fBạn nhận được dược tính chia sẻ từ §e" + player.getName());
                    }
                }
            }
        }

        brewingManager.deletePotionFile(potionUuid);
    }

    // ==========================================
    // HÀM HELPER: ÁP DỤNG CHỈ SỐ LÊN MỘT THỰC THỂ
    // ==========================================
    private void applyDose(Player target, PotionData data, double effectMultiplier) {
        UUID targetUuid = target.getUniqueId();
        RPGPlayer rpgTarget = RPGCore.getInstance().getPlayerManager().getRPGPlayer(targetUuid);
        if (rpgTarget == null) return;

        // Xử lý Máu tức thì
        if (data.customStats.containsKey("Hồi máu tức thì (%)")) {
            double healPercent = (data.customStats.get("Hồi máu tức thì (%)") * effectMultiplier) / 100.0;
            rpgTarget.heal(rpgTarget.getTotalMaxHealth() * healPercent);
            target.getWorld().spawnParticle(org.bukkit.Particle.HEART, target.getLocation().add(0, 2, 0), 5);
        }

        // Xử lý Mana tức thì
        if (data.customStats.containsKey("Hồi năng lượng tức thì (%)")) {
            double manaPercent = (data.customStats.get("Hồi năng lượng tức thì (%)") * effectMultiplier) / 100.0;
            double manaAmount = rpgTarget.getUsableMaxResource() * manaPercent;
            rpgTarget.setCurrentResource(rpgTarget.getCurrentResource() + manaAmount);
            target.playSound(target.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1f);
        }

        // Buff chỉ số qua thời gian
        for (String statKey : data.customStats.keySet()) {
            if (statKey.contains("tức thì")) continue;
            double finalVal = data.customStats.get(statKey) * effectMultiplier;
            buffManager.applyBuff(targetUuid, statKey, finalVal, data.durationMinutes);
        }

        // Áp dụng Potion Vanilla (Sửa lỗi parse cấp độ)
        int durationTicks = data.durationMinutes * 60 * 20;
        for (String eff : data.potionEffects) {
            String[] parts = eff.split(":");
            PotionEffectType type = PotionEffectType.getByName(parts[0]);
            int amplifier = (parts.length > 1) ? Integer.parseInt(parts[1]) - 1 : 0;

            if (type != null) {
                target.addPotionEffect(new PotionEffect(type, durationTicks, Math.max(0, amplifier), false, false, true));
            }
        }

        // Xử lý Special Effects (Đồng bộ tag BẤT_TỬ và IMMORTAL)[cite: 7]
        for (String spec : data.specialEffects) {
            if (spec.equals("BẤT_TỬ") || spec.equals("IMMORTAL")) {
                target.setMetadata("Alchemist_Immortal", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 10000L));
            }
            else if (spec.equals("THANH_TẨY") || spec.equals("CLEANSE")) {
                // Sử dụng hàm giải trừ từ Manager[cite: 6, 7]
                vn.minecraft.rPGCore.managers.StatusEffectManager.cleanseNegativeEffects(target);
            }
        }
    }
}