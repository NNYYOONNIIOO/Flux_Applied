package com.flux_applied.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import com.flux_applied.FluxApplied;
import com.flux_applied.FluxAppliedTab;
import com.flux_applied.tile.TileEntityEnergyProvider;

import javax.annotation.Nullable;

public class BlockEnergyPort extends net.minecraft.block.Block
{
    public static final PropertyInteger MODE = PropertyInteger.create("mode", 0, 2);
    
    public BlockEnergyPort() {
        super(Material.IRON);
        setUnlocalizedName("flux_applied.energy_port");
        setRegistryName(FluxApplied.MODID, "energy_port");
        setCreativeTab(FluxAppliedTab.INSTANCE);
        setHardness(3.0F);
        setResistance(10.0F);
        setSoundType(SoundType.METAL);
        setDefaultState(this.blockState.getBaseState().withProperty(MODE, 2));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, MODE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityEnergyProvider) {
            return state.withProperty(MODE, ((TileEntityEnergyProvider) te).getMode());
        }
        return state.withProperty(MODE, 2);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityEnergyProvider();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityEnergyProvider) {
                ((TileEntityEnergyProvider) te).toggleMode(player);
            }
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityEnergyProvider) {
            ((TileEntityEnergyProvider) te).invalidate();
        }
        super.breakBlock(world, pos, state);
    }
}