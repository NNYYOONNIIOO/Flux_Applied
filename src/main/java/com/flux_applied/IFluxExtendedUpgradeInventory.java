package com.flux_applied;

/**
 * Extended upgrade inventory interface for Flux Applied custom upgrades.
 * Mixed into UpgradeInventory by MixinUpgradeInventory.
 */
public interface IFluxExtendedUpgradeInventory {

    /**
     * Get the number of installed custom upgrades of the given type.
     */
    int flux_applied$getInstalledUpgrades(IFluxUpgradeModule upgrade);

    /**
     * Get the max number of custom upgrades of the given type that can be installed.
     */
    int flux_applied$getMaxInstalled(IFluxUpgradeModule upgrade);

    boolean flux_applied$isInterfaceDevice();
}
