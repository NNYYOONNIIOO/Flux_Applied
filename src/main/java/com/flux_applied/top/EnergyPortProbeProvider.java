package com.flux_applied.top;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import com.flux_applied.FluxApplied;
import com.flux_applied.block.BlockEnergyPort;
import com.flux_applied.tile.TileEntityEnergyProvider;

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
                TileEntityEnergyProvider energyPort = (TileEntityEnergyProvider) te;
                int portMode = energyPort.getMode();
                
                String modeText;
                switch (portMode) {
                    case 0:
                        modeText = "{*flux_applied.top.mode_input_only*}";
                        break;
                    case 1:
                        modeText = "{*flux_applied.top.mode_output_only*}";
                        break;
                    default:
                        modeText = "{*flux_applied.top.mode_bidirectional*}";
                        break;
                }
                
                probeInfo.text("{*flux_applied.top.mode*}: " + modeText);
            }
        }
    }
}