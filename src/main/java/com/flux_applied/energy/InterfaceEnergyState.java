package com.flux_applied.energy;

import appeng.api.networking.security.IActionHost;
import com.flux_applied.FluxApplied;
import com.flux_applied.ModConfig;
import com.flux_applied.item.ItemProviderCard;
import com.flux_applied.util.ProviderCardHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.IItemHandler;

public final class InterfaceEnergyState {
    private final IActionHost host;
    private final NetworkEnergyStorage storage;
    private ItemStack card = ItemStack.EMPTY;
    private boolean cardKnown;

    public InterfaceEnergyState(IActionHost host) {
        this.host = host;
        this.storage = new NetworkEnergyStorage(
                () -> this.host,
                () -> ItemProviderCard.getMode(this.card),
                this::hasCard,
                ModConfig::getEnergyPortTransferRate,
                this::currentTick);
    }

    public NetworkEnergyStorage storage() {
        return storage;
    }

    public boolean hasCard() {
        if (!cardKnown) {
            refreshCard();
        }
        return !card.isEmpty();
    }

    public void refreshCard() {
        IItemHandler upgrades = ProviderCardHelper.getUpgradeInventory(host);
        ItemStack found = ItemStack.EMPTY;
        if (upgrades != null) {
            for (int slot = 0; slot < upgrades.getSlots(); slot++) {
                ItemStack candidate = upgrades.getStackInSlot(slot);
                if (!candidate.isEmpty() && candidate.getItem() == FluxApplied.providerCard) {
                    found = candidate;
                    break;
                }
            }
        }
        card = found;
        cardKnown = true;
    }

    private long currentTick() {
        TileEntity tile = ProviderCardHelper.tile(host);
        return tile == null || tile.getWorld() == null ? 0 : tile.getWorld().getGameTime();
    }
}
