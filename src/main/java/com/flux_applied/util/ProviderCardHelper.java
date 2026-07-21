package com.flux_applied.util;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.IPart;
import appeng.helpers.IInterfaceHost;
import appeng.me.helpers.MachineSource;
import appeng.parts.misc.PartInterface;
import com.flux_applied.FluxApplied;
import com.flux_applied.ae2.FluxStack;
import com.flux_applied.ae2.FluxStorageChannel;
import com.flux_applied.item.ItemProviderCard;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

public class ProviderCardHelper {

    @Nullable
    public static ItemStack findProviderCard(IPart part) {
        if (part == null) return null;

        ItemStack card = findCardInInventory(getItemUpgradeInventory(part));
        if (card != null) return card;
        card = findCardInInventory(getFluidUpgradeInventory(part));
        if (card != null) return card;
        if (Loader.isModLoaded("mekeng")) return findCardInInventory(getGasUpgradeInventory(part));
        return null;
    }

    @Nullable
    private static ItemStack findCardInInventory(@Nullable IItemHandler inventory) {
        if (inventory == null) return null;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == FluxApplied.providerCard) return stack;
        }
        return null;
    }

    private static IItemHandler getItemUpgradeInventory(IPart part) {
        String className = part.getClass().getName();
        if (className.contains("PartDualInterface") || className.contains("PartTrioInterface")) return getInventoryByReflection(part, "item_upgrades");
        try {
            if (part instanceof IInterfaceHost) return ((IInterfaceHost) part).getInterfaceDuality().getInventoryByName("upgrades");
        } catch (Exception ignored) {}
        return null;
    }

    private static IItemHandler getFluidUpgradeInventory(IPart part) {
        String className = part.getClass().getName();
        if (className.contains("PartDualInterface") || className.contains("PartTrioInterface")) return getInventoryByReflection(part, "fluid_upgrades");
        try {
            if (part instanceof appeng.fluids.helper.IFluidInterfaceHost) return ((appeng.fluids.helper.IFluidInterfaceHost) part).getDualityFluidInterface().getInventoryByName("upgrades");
        } catch (Exception ignored) {}
        return null;
    }

    private static IItemHandler getGasUpgradeInventory(IPart part) {
        if (!Loader.isModLoaded("mekeng")) return null;
        String className = part.getClass().getName();
        if (className.contains("PartTrioInterface")) {
            IItemHandler inventory = getInventoryByReflection(part, "gas_upgrades");
            return inventory != null ? inventory : getInventoryByReflection(part, "upgrades");
        }
        if (className.contains("PartGasInterface")) return getInventoryByReflection(part, "upgrades");
        return null;
    }

    private static IItemHandler getInventoryByReflection(Object object, String name) {
        try {
            Method m = ReflectionCache.getMethod(object.getClass(), "getInventoryByName", String.class);
            return (IItemHandler) ReflectionCache.invokeMethod(m, object, name);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    public static IItemHandler getUpgradeInventory(IPart part) {
        IItemHandler item = getItemUpgradeInventory(part);
        IItemHandler fluid = getFluidUpgradeInventory(part);
        IItemHandler gas = Loader.isModLoaded("mekeng") ? getGasUpgradeInventory(part) : null;
        if (item instanceof IItemHandlerModifiable && fluid instanceof IItemHandlerModifiable) {
            return gas instanceof IItemHandlerModifiable
                    ? new CombinedInvWrapper((IItemHandlerModifiable) item, (IItemHandlerModifiable) fluid, (IItemHandlerModifiable) gas)
                    : new CombinedInvWrapper((IItemHandlerModifiable) item, (IItemHandlerModifiable) fluid);
        }
        if (item != null) return item;
        if (fluid != null) return fluid;
        return gas;
    }

    /**
     * Find the Provider Card in a block-form interface TE's upgrade slots.
     */
    @Nullable
    public static ItemStack findProviderCardInBlockTE(net.minecraft.tileentity.TileEntity te) {
        String name = te.getClass().getName();
        if (name.contains("TileDualInterface") || name.contains("TileTrioInterface")) {
            ItemStack card = findCardInInventory(getInventoryByReflection(te, "item_upgrades"));
            if (card != null) return card;
            card = findCardInInventory(getInventoryByReflection(te, "fluid_upgrades"));
            if (card != null) return card;
            if (Loader.isModLoaded("mekeng")) return findCardInInventory(getInventoryByReflection(te, "gas_upgrades"));
            return null;
        }
        if (name.contains("TileGasInterface")) return findCardInInventory(getInventoryByReflection(te, "upgrades"));
        return findCardInInventory(getUpgradeInventoryFromBlockTE(te));
    }

    /**
     * Get the upgrade inventory from a block-form interface TE.
     */
    @Nullable
    public static IItemHandler getUpgradeInventoryFromBlockTE(net.minecraft.tileentity.TileEntity te) {
        try {
            String name = te.getClass().getName();
            if (name.contains("TileDualInterface") || name.contains("TileTrioInterface")) {
                IItemHandler item = getInventoryByReflection(te, "item_upgrades");
                IItemHandler fluid = getInventoryByReflection(te, "fluid_upgrades");
                IItemHandler gas = Loader.isModLoaded("mekeng") ? getInventoryByReflection(te, "gas_upgrades") : null;
                if (item != null) return item;
                if (fluid != null) return fluid;
                if (gas != null) return gas;
            }
            if (name.contains("TileGasInterface")) return getInventoryByReflection(te, "upgrades");
            if (te instanceof IInterfaceHost) {
                return ((IInterfaceHost) te).getInterfaceDuality().getInventoryByName("upgrades");
            }
            if (te instanceof appeng.fluids.helper.IFluidInterfaceHost) {
                return ((appeng.fluids.helper.IFluidInterfaceHost) te).getDualityFluidInterface().getInventoryByName("upgrades");
            }
            // ae2fcr block Dual/Trio Interface - implements IInterfaceHost, already handled above
            // mekeng block Gas Interface - does NOT implement IInterfaceHost
            String className = te.getClass().getName();
            if (className.contains("TileDualInterface") || className.contains("TileTrioInterface")) {
                Method m = ReflectionCache.getMethod(te.getClass(), "getInventoryByName", String.class);
                IItemHandler inv = (IItemHandler) ReflectionCache.invokeMethod(m, te, "upgrades");
                if (inv != null) return inv;
            }
            if (className.contains("TileGasInterface")) {
                Method m = ReflectionCache.getMethod(te.getClass(), "getInventoryByName", String.class);
                IItemHandler inv = (IItemHandler) ReflectionCache.invokeMethod(m, te, "upgrades");
                if (inv != null) return inv;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static boolean isInterfaceWithProviderCard(IPart part) {
        return findProviderCard(part) != null;
    }

    public static int getProviderCardMode(IPart part) {
        ItemStack card = findProviderCard(part);
        if (card == null) return 1;
        return ItemProviderCard.getMode(card);
    }

    public static boolean isInterfacePart(IPart part) {
        if (part instanceof PartInterface) return true;
        if (part instanceof IInterfaceHost) return true;
        if (part instanceof appeng.fluids.helper.IFluidInterfaceHost) return true;
        String className = part.getClass().getName();
        return className.contains("PartDualInterface")
                || className.contains("PartTrioInterface")
                || className.contains("PartGasInterface")
                || className.contains("PartFluidInterface");
    }

    @Nullable
    public static IGridNode getGridNode(IPart part) {
        try {
            if (part instanceof IActionHost) {
                IGridNode node = ((IActionHost) part).getActionableNode();
                if (node != null) return node;
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Nullable
    public static IStorageGrid getStorageGrid(IPart part) {
        IGridNode node = getGridNode(part);
        if (node == null) return null;
        try {
            IGrid grid = node.getGrid();
            if (grid != null) {
                return grid.getCache(IStorageGrid.class);
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static long getNetworkEnergyStored(IPart part) {
        IStorageGrid storage = getStorageGrid(part);
        if (storage == null) return 0;
        try {
            MachineSource source = new MachineSource((IActionHost) part);
            FluxStack request = new FluxStack(Long.MAX_VALUE);
            FluxStack extracted = storage.getInventory(FluxStorageChannel.INSTANCE)
                    .extractItems(request, Actionable.SIMULATE, source);
            return extracted != null ? extracted.getStackSize() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static long injectEnergy(IPart part, long amount, boolean simulate) {
        IStorageGrid storage = getStorageGrid(part);
        if (storage == null) return 0;
        try {
            MachineSource source = new MachineSource((IActionHost) part);
            FluxStack toInsert = new FluxStack(amount);
            FluxStack remaining = storage.getInventory(FluxStorageChannel.INSTANCE)
                    .injectItems(toInsert, simulate ? Actionable.SIMULATE : Actionable.MODULATE, source);
            return remaining != null ? remaining.getStackSize() : 0;
        } catch (Exception e) {
            return amount;
        }
    }

    public static long extractEnergy(IPart part, long amount, boolean simulate) {
        IStorageGrid storage = getStorageGrid(part);
        if (storage == null) return 0;
        try {
            MachineSource source = new MachineSource((IActionHost) part);
            FluxStack request = new FluxStack(amount);
            FluxStack extracted = storage.getInventory(FluxStorageChannel.INSTANCE)
                    .extractItems(request, simulate ? Actionable.SIMULATE : Actionable.MODULATE, source);
            return extracted != null ? extracted.getStackSize() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
