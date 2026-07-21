package com.flux_applied;

import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom upgrade registration system for Flux Applied.
 * Similar to AE2's Upgrades enum but uses string IDs,
 * allowing us to control which devices accept our upgrades.
 */
public class FluxUpgrades {

    public static final String ENERGY_PORT_CARD = "energy_port_card";

    private static final Map<String, Map<ItemStack, Integer>> SUPPORTED = new LinkedHashMap<>();

    /**
     * Register a machine as supporting a custom upgrade type.
     */
    public static void registerItem(String upgradeTypeId, ItemStack machine, int maxSupported) {
        if (machine == null || machine.isEmpty()) return;
        SUPPORTED.computeIfAbsent(upgradeTypeId, k -> new LinkedHashMap<>()).put(machine, maxSupported);
    }

    /**
     * Get all machines supporting a custom upgrade type.
     */
    public static Map<ItemStack, Integer> getSupported(String upgradeTypeId) {
        return SUPPORTED.getOrDefault(upgradeTypeId, Collections.emptyMap());
    }

    /**
     * Check if a machine ItemStack supports the given upgrade type.
     */
    public static boolean isSupported(String upgradeTypeId, ItemStack machine) {
        Map<ItemStack, Integer> supported = getSupported(upgradeTypeId);
        for (ItemStack is : supported.keySet()) {
            if (ItemStack.areItemsEqual(is, machine)) return true;
        }
        return false;
    }

    /**
     * Get the max number of a custom upgrade type that a machine supports.
     */
    public static int getMaxInstalled(String upgradeTypeId, ItemStack machine) {
        Map<ItemStack, Integer> supported = getSupported(upgradeTypeId);
        for (Map.Entry<ItemStack, Integer> entry : supported.entrySet()) {
            if (ItemStack.areItemsEqual(entry.getKey(), machine)) return entry.getValue();
        }
        return 0;
    }

    /**
     * Count how many of a custom upgrade type are installed in an upgrade inventory.
     */
    public static int countInstalled(String upgradeTypeId, net.minecraftforge.items.IItemHandler upgradeInv) {
        int count = 0;
        for (int i = 0; i < upgradeInv.getSlots(); i++) {
            ItemStack stack = upgradeInv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IFluxUpgradeModule) {
                IFluxUpgradeModule mod = (IFluxUpgradeModule) stack.getItem();
                if (mod.getUpgradeTypeId().equals(upgradeTypeId)) {
                    count++;
                }
            }
        }
        return count;
    }
}
