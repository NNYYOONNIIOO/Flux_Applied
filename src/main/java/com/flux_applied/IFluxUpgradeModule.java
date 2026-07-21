package com.flux_applied;

import appeng.api.implementations.items.IUpgradeModule;
import net.minecraft.item.ItemStack;

/**
 * Custom upgrade module interface for Flux Applied upgrades.
 * Uses string-based type IDs instead of AE2's Upgrades enum,
 * allowing fine-grained control over which devices accept which upgrades.
 *
 * IMPORTANT: Implementing classes MUST override getType() to return
 * Upgrades.CRAFTING (or any non-null Upgrades value). Java 8 default
 * methods on interfaces may not work reliably in Forge 1.12.2's
 * class loading environment, causing getType() to return null at
 * runtime, which leads to NPE in AE2's updateUpgradeInfo switch.
 *
 * MixinUpgradeInventory redirects getType() in updateUpgradeInfo
 * to return STICKY for IFluxUpgradeModule items, so they don't
 * pollute AE2's standard upgrade counters. The real counting is
 * done by IFluxExtendedUpgradeInventory.
 */
public interface IFluxUpgradeModule extends IUpgradeModule {

    String getUpgradeTypeId();

    int getMaxInstalled();
}
