package com.flux_applied.util;

import appeng.api.networking.security.IActionHost;
import appeng.api.parts.IPart;
import appeng.helpers.IInterfaceHost;
import appeng.me.GridAccessException;
import appeng.me.helpers.IGridProxyable;
import com.flux_applied.FluxApplied;
import com.flux_applied.energy.IFluxInterfaceHost;
import com.flux_applied.energy.InterfaceEnergyState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.items.IItemHandler;
import javax.annotation.Nullable;
import java.util.EnumSet;

public final class ProviderCardHelper {
    private ProviderCardHelper() {}
    public static boolean isInterface(Object host) {
        return host instanceof IInterfaceHost;
    }
    public static boolean isInterfacePart(IPart part) { return isInterface(part); }
    @Nullable public static InterfaceEnergyState state(Object host) {
        return host instanceof IFluxInterfaceHost ? ((IFluxInterfaceHost) host).fluxApplied$getState() : null;
    }
    @Nullable public static IItemHandler getUpgradeInventory(Object host) {
        if (host instanceof IInterfaceHost) return ((IInterfaceHost) host).getInterfaceDuality().getInventoryByName("upgrades");
        return null;
    }
    public static ItemStack findProviderCard(Object host) {
        IItemHandler inventory = getUpgradeInventory(host);
        if (inventory != null) {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty() && stack.getItem() == FluxApplied.providerCard) return stack;
            }
        }
        return ItemStack.EMPTY;
    }
    @Nullable public static TileEntity tile(Object host) {
        if (host instanceof IInterfaceHost) return ((IInterfaceHost) host).getTileEntity();
        return null;
    }
    public static EnumSet<Direction> targets(Object host) {
        if (host instanceof IInterfaceHost) return ((IInterfaceHost) host).getTargets();
        return EnumSet.noneOf(Direction.class);
    }
    public static void upgradesChanged(Object host) {
        InterfaceEnergyState state = state(host);
        if (state == null) return;
        state.refreshCard();
        TileEntity tile = tile(host);
        if (tile == null || tile.getWorld() == null || tile.getWorld().isRemote || !(host instanceof IGridProxyable)) return;
        IGridProxyable proxyHost = (IGridProxyable) host;
        try {
            proxyHost.getProxy().getTick().wakeDevice(((IActionHost) host).getActionableNode());
        } catch (GridAccessException ignored) {}
        proxyHost.saveChanges();
    }

    public static void keepAwake(Object host) {
        InterfaceEnergyState state = state(host);
        TileEntity tile = tile(host);
        if (state == null || !state.hasCard() || tile == null || tile.getWorld() == null
                || tile.getWorld().isRemote || !(host instanceof IGridProxyable)) return;
        IGridProxyable gridHost = (IGridProxyable) host;
        if (gridHost.getProxy().getNode() == null) return;
        try {
            gridHost.getProxy().getTick().wakeDevice(gridHost.getProxy().getNode());
        } catch (GridAccessException ignored) {}
    }
    public static long getNetworkEnergyStored(IPart part) {
        InterfaceEnergyState state = state(part);
        return state == null ? 0 : state.storage().getEnergyStoredLong();
    }
}
