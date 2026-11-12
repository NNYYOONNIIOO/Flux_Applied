package com.flux_applied.block;
import com.flux_applied.FluxApplied;
import com.flux_applied.tile.TileEntityEnergyProvider;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
public final class BlockEnergyPort extends Block {
    public static final IntegerProperty MODE = IntegerProperty.create("mode", 0, 2);
    public BlockEnergyPort() {
        super(AbstractBlock.Properties.create(Material.IRON).hardnessAndResistance(3, 10).sound(SoundType.METAL));
        setDefaultState(stateContainer.getBaseState().with(MODE, 2));
    }
    @Override protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) { builder.add(MODE); }
    @Override public boolean hasTileEntity(BlockState state) { return true; }
    @Override public TileEntity createTileEntity(BlockState state, IBlockReader world) { return new TileEntityEnergyProvider(); }
    @Override public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityEnergyProvider) ((TileEntityEnergyProvider) tile).toggleMode(player);
        }
        return ActionResultType.SUCCESS;
    }
}
