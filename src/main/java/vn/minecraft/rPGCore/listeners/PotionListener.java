package vn.minecraft.rPGCore.listeners;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
        RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(playerUuid);

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

        if (data.customStats.containsKey("Hồi máu tức thì (%)")) {
            double healPercent = (data.customStats.get("Hồi máu tức thì (%)") * effectMultiplier) / 100.0;
            rpgPlayer.heal(rpgPlayer.getTotalMaxHealth() * healPercent);
            player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 2, 0), 5);
        }

        if (data.customStats.containsKey("Hồi năng lượng tức thì (%)")) {
            double manaPercent = (data.customStats.get("Hồi năng lượng tức thì (%)") * effectMultiplier) / 100.0;
            double manaAmount = rpgPlayer.getUsableMaxResource() * manaPercent;
            rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() + manaAmount);
            player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1f);
        }

        for (String statKey : data.customStats.keySet()) {
            if (statKey.contains("tức thì")) continue;
            double finalVal = data.customStats.get(statKey) * effectMultiplier;
            buffManager.applyBuff(playerUuid, statKey, finalVal, data.durationMinutes);
        }

        int durationTicks = data.durationMinutes * 60 * 20;
        for (String eff : data.potionEffects) {
            PotionEffectType type = PotionEffectType.getByName(eff);
            if (type != null) player.addPotionEffect(new PotionEffect(type, durationTicks, 0, false, false, true));
        }

        for (String spec : data.specialEffects) {
            if (spec.equals("BẤT_TỬ")) player.setMetadata("Alchemist_Immortal", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 10000L));
            else if (spec.equals("THANH_TẨY")) for (PotionEffect e : player.getActivePotionEffects()) player.removePotionEffect(e.getType());
        }

        player.sendMessage("§a[+] Đã hấp thụ Dược lực thành công.");
        brewingManager.deletePotionFile(potionUuid);
    }
}