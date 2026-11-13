package com.flux_applied.integration.jade;

import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.api.util.AEPartLocation;
import appeng.me.helpers.IGridProxyable;
import com.flux_applied.energy.InterfaceEnergyState;
import com.flux_applied.item.ItemProviderCard;
import com.flux_applied.part.PartEnergyImportBus;
import com.flux_applied.part.PartEnergyProvider;
import com.flux_applied.part.PartEnergyStorageBus;
import com.flux_applied.tile.TileEntityEnergyProvider;
import com.flux_applied.util.ProviderCardHelper;
import mcp.mobius.waila.api.IComponentProvider;
import mcp.mobius.waila.api.IDataAccessor;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerDataProvider;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.text.NumberFormat;
import java.util.List;

public final class EnergyInfoProvider implements IComponentProvider, IServerDataProvider<TileEntity> {
    private static final String DATA_KEY = "flux_applied:energy_info";

    @Override
    public void appendServerData(CompoundNBT data, ServerPlayerEntity player, World world, TileEntity tile) {
        CompoundNBT info = new CompoundNBT();
        if (tile instanceof IPartHost) {
            IPartHost host = (IPartHost) tile;
            // Jade's request supplies the block position, not the hit vector. Only send
            // this block's six mounted parts; the client selects the exact hit part.
            for (AEPartLocation side : AEPartLocation.SIDE_LOCATIONS) {
                CompoundNBT part = describe(host.getPart(side));
                if (!part.isEmpty()) {
                    info.put(side.name(), part);
                }
            }
        } else {
            info = describe(tile);
        }
        if (!info.isEmpty()) {
            data.put(DATA_KEY, info);
        }
    }

    private static CompoundNBT describe(Object target) {
        CompoundNBT info = new CompoundNBT();
        if (target instanceof TileEntityEnergyProvider) {
            TileEntityEnergyProvider port = (TileEntityEnergyProvider) target;
            info.putInt("mode", port.getMode());
            info.putBoolean("active", port.isNetworkConnected());
            info.putLong("stored", port.getEnergyStoredLong());
        } else if (target instanceof PartEnergyProvider) {
            PartEnergyProvider port = (PartEnergyProvider) target;
            info.putInt("mode", port.getMode());
            info.putLong("stored", port.getNetworkEnergyStored());
        } else if (target instanceof PartEnergyStorageBus) {
            info.putInt("mode", ((PartEnergyStorageBus) target).getMode());
        } else if (target instanceof PartEnergyImportBus) {
            info.putLong("rate", ((PartEnergyImportBus) target).getTransferRate());
        } else if (ProviderCardHelper.isInterface(target)) {
            ItemStack card = ProviderCardHelper.findProviderCard(target);
            InterfaceEnergyState state = ProviderCardHelper.state(target);
            if (card.isEmpty() || state == null) {
                return info;
            }
            info.putInt("mode", ItemProviderCard.getMode(card));
            info.putLong("stored", state.storage().getEnergyStoredLong());
        } else {
            return info;
        }
        if (target instanceof IGridProxyable) {
            info.putBoolean("active", ((IGridProxyable) target).getProxy().isActive());
        }
        return info;
    }

    @Override
    public void appendBody(List<ITextComponent> tooltip, IDataAccessor accessor, IPluginConfig config) {
        CompoundNBT data = accessor.getServerData();
        if (!data.contains(DATA_KEY, Constants.NBT.TAG_COMPOUND)) {
            return;
        }
        CompoundNBT info = data.getCompound(DATA_KEY);
        if (accessor.getTileEntity() instanceof IPartHost) {
            RayTraceResult hit = accessor.getHitResult();
            if (hit == null || hit.getType() != RayTraceResult.Type.BLOCK) {
                return;
            }
            Vector3d relativeHit = hit.getHitVec().subtract(accessor.getPosition().getX(),
                    accessor.getPosition().getY(), accessor.getPosition().getZ());
            SelectedPart selected = ((IPartHost) accessor.getTileEntity()).selectPart(relativeHit);
            if (selected.part == null || selected.side == null) {
                return;
            }
            info = info.getCompound(selected.side.name());
        }
        if (info.contains("active", Constants.NBT.TAG_BYTE)) {
            tooltip.add(new TranslationTextComponent(info.getBoolean("active")
                    ? "flux_applied.jade.device_on" : "flux_applied.jade.device_off"));
        }
        if (info.contains("stored", Constants.NBT.TAG_LONG)) {
            tooltip.add(new TranslationTextComponent("flux_applied.tooltip.stored_fe",
                    NumberFormat.getIntegerInstance().format(info.getLong("stored"))));
        }
        if (info.contains("mode", Constants.NBT.TAG_INT)) {
            tooltip.add(new TranslationTextComponent("flux_applied.jade.mode").appendString(": ")
                    .appendSibling(new TranslationTextComponent(modeKey(info.getInt("mode")))));
        }
        if (info.contains("rate", Constants.NBT.TAG_LONG)) {
            tooltip.add(new TranslationTextComponent("flux_applied.jade.transfer_rate").appendString(": "
                    + NumberFormat.getIntegerInstance().format(info.getLong("rate")) + " FE/t"));
        }
    }

    private static String modeKey(int mode) {
        switch (mode) {
            case 0: return "flux_applied.jade.mode_input_only";
            case 1: return "flux_applied.jade.mode_output_only";
            default: return "flux_applied.jade.mode_bidirectional";
        }
    }
}
