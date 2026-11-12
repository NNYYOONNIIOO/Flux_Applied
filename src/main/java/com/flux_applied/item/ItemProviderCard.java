package com.flux_applied.item;
import com.flux_applied.FluxApplied;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import java.util.List;
import javax.annotation.Nullable;
public final class ItemProviderCard extends Item implements appeng.api.implementations.items.IUpgradeModule {
    public ItemProviderCard() { super(FluxApplied.properties().maxStackSize(1)); }
    @Override public appeng.api.config.Upgrades getType(ItemStack stack) { return appeng.api.config.Upgrades.CRAFTING; }
    public static int getMode(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemProviderCard) || !stack.hasTag() || !stack.getTag().contains("Mode")) return 1;
        int mode = stack.getTag().getInt("Mode");
        return mode >= 0 && mode <= 2 ? mode : 1;
    }
    public static void setMode(ItemStack stack, int mode) { if (stack.getItem() instanceof ItemProviderCard) stack.getOrCreateTag().putInt("Mode", Math.floorMod(mode, 3)); }
    public static void cycleMode(ItemStack stack) { setMode(stack, getMode(stack) + 1); }
    public static void sendModeMessage(PlayerEntity player, int mode) {
        player.sendStatusMessage(new TranslationTextComponent(mode == 0 ? "flux_applied.message.card_input_only" : mode == 1 ? "flux_applied.message.card_output_only" : "flux_applied.message.card_bidirectional"), true);
    }
    @Override public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!player.isSneaking()) return ActionResult.resultPass(stack);
        if (!world.isRemote) { cycleMode(stack); sendModeMessage(player, getMode(stack)); }
        return ActionResult.resultSuccess(stack);
    }
    @Override public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        int mode = getMode(stack);
        tooltip.add(new TranslationTextComponent(mode == 0 ? "flux_applied.tooltip.card_mode_input" : mode == 1 ? "flux_applied.tooltip.card_mode_output" : "flux_applied.tooltip.card_mode_bidirectional"));
        tooltip.add(new TranslationTextComponent("flux_applied.tooltip.card_devices"));
    }
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) { return false; }
}
