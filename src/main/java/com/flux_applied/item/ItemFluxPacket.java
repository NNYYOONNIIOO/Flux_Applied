package com.flux_applied.item;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import com.flux_applied.FluxApplied;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.TextFormatting;
public final class ItemFluxPacket extends Item {
    public ItemFluxPacket() { super(new Item.Properties().maxStackSize(64)); }
    // Every amount has the same item identity. The actual quantity lives in IAEItemStack.
    public static ItemStack create(long amount) { return new ItemStack(FluxApplied.fluxPacket); }
    public static IAEItemStack createAE(long amount) {
        IAEItemStack stack = AEItemStack.fromItemStack(create(1));
        return stack == null ? null : stack.setStackSize(amount);
    }
    public static boolean isFluxPacket(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof ItemFluxPacket; }
    public static long getFE(ItemStack stack) { return isFluxPacket(stack) ? stack.getCount() : 0; }
    public static boolean isDisplayOnly(ItemStack stack) { return isFluxPacket(stack); }
    @Override public ITextComponent getDisplayName(ItemStack stack) {
        return new TranslationTextComponent("item.flux_applied.flux_packet.display_name").mergeStyle(TextFormatting.GOLD);
    }
}
