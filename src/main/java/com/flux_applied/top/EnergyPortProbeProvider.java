package com.flux_applied.top;

import appeng.api.networking.IGrid;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.me.GridAccessException;
import com.flux_applied.FluxApplied;
import com.flux_applied.block.BlockEnergyPort;
import com.flux_applied.item.ItemProviderCard;
import com.flux_applied.part.PartEnergyProvider;
import com.flux_applied.tile.TileEntityEnergyProvider;
import com.flux_applied.util.ProviderCardHelper;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.NumberFormat;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.config.Config;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class EnergyPortProbeProvider implements IProbeInfoProvider {

    @Override
    public String getID() {
        return FluxApplied.MODID + ":energy_port_info";
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        if (blockState.getBlock() instanceof BlockEnergyPort) {
            TileEntity te = world.getTileEntity(data.getPos());
            if (te instanceof TileEntityEnergyProvider) {
                TileEntityEnergyProvider ep = (TileEntityEnergyProvider) te;

                boolean active = false;
                try {
                    IGrid grid = ep.getProxy().getGrid();
                    active = grid != null;
                } catch (GridAccessException ignored) {
                }
                probeInfo.text(active ? "{*flux_applied.top.device_on*}" : "{*flux_applied.top.device_off*}");
                probeInfo.text("{*flux_applied.top.mode*}: " + getModeText(ep.getMode()));
            }
        } else {
            TileEntity te = world.getTileEntity(data.getPos());
            if (te instanceof IPartHost) {
                IPartHost host = (IPartHost) te;
                Vec3d hitVec = data.getHitVec().subtract(data.getPos().getX(), data.getPos().getY(), data.getPos().getZ());
                SelectedPart sp = host.selectPart(hitVec);
                IPart hitPart = sp.part;
                if (hitPart instanceof PartEnergyProvider) {
                    PartEnergyProvider ep = (PartEnergyProvider) hitPart;
                    long storedFE = Math.min(ep.getNetworkEnergyStored(), Integer.MAX_VALUE);
                    probeInfo.progress(storedFE, Integer.MAX_VALUE,
                            probeInfo.defaultProgressStyle()
                                    .suffix("FE")
                                    .filledColor(Config.rfbarFilledColor)
                                    .alternateFilledColor(Config.rfbarAlternateFilledColor)
                                    .borderColor(Config.rfbarBorderColor)
                                    .numberFormat(NumberFormat.COMPACT));
                    probeInfo.text("{*flux_applied.top.mode*}: " + getModeText(ep.getMode()));
                } else if (hitPart != null && ProviderCardHelper.isInterfacePart(hitPart)) {
                    ItemStack card = ProviderCardHelper.findProviderCard(hitPart);
                    if (card != null) {
                        int cardMode = ItemProviderCard.getMode(card);
                        long storedFE = Math.min(ProviderCardHelper.getNetworkEnergyStored(hitPart), Integer.MAX_VALUE);
                        probeInfo.progress(storedFE, Integer.MAX_VALUE,
                                probeInfo.defaultProgressStyle()
                                        .suffix("FE")
                                        .filledColor(Config.rfbarFilledColor)
                                        .alternateFilledColor(Config.rfbarAlternateFilledColor)
                                        .borderColor(Config.rfbarBorderColor)
                                        .numberFormat(NumberFormat.COMPACT));
                        probeInfo.text("{*flux_applied.top.mode*}: " + getModeText(cardMode));
                    }
                }
            }
        }
    }

    private String getModeText(int portMode) {
        switch (portMode) {
            case 0: return "{*flux_applied.top.mode_input_only*}";
            case 1: return "{*flux_applied.top.mode_output_only*}";
            default: return "{*flux_applied.top.mode_bidirectional*}";
        }
    }
}
