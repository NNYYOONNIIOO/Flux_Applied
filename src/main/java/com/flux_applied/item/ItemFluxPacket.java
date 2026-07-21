package com.flux_applied.item;

import net.minecraft.util.text.translation.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import com.flux_applied.FluxApplied;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemFluxPacket extends Item {

    public static final String TAG_FE = "fe";
    public static final String TAG_DISPLAY_ONLY = "DisplayOnly";

    private static ItemStack BASE_PACKET = null;

    public ItemFluxPacket() {
        this.setMaxStackSize(1);
        this.setRegistryName(FluxApplied.MODID, "flux_packet");
        this.setUnlocalizedName("flux_applied.flux_packet");
        this.setCreativeTab(null);
    }

    @Override
    public void getSubItems(@Nonnull CreativeTabs tab, net.minecraft.util.NonNullList<ItemStack> items) {
    }

    @Override
    protected boolean isInCreativeTab(CreativeTabs targetTab) {
        return false;
    }

    @Override
    @Nonnull
    public String getItemStackDisplayName(@Nonnull ItemStack stack) {
        if (isDisplayOnly(stack)) {
            return getFEName();
        }
        long fe = getFE(stack);
        return String.format("%s (%,d)", getFEName(), fe);
    }

    @Override
    public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (!isDisplayOnly(stack)) {
            tooltip.add("\u00a77" + I18n.translateToLocal("flux_applied.tooltip.shift_for_details"));
            if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT) || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT)) {
                long fe = getFE(stack);
                tooltip.add(I18n.translateToLocalFormatted("flux_applied.tooltip.stored_fe", fe));
            }
        }
    }

    private static synchronized ItemStack getBasePacket() {
        if (BASE_PACKET == null || BASE_PACKET.isEmpty()) {
            BASE_PACKET = new ItemStack(FluxApplied.fluxPacket);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setLong(TAG_FE, 0);
            tag.setBoolean(TAG_DISPLAY_ONLY, true);
            BASE_PACKET.setTagCompound(tag);
        }
        return BASE_PACKET.copy();
    }

    public static ItemStack create(long fe) {
        ItemStack packet = getBasePacket();
        if (packet.hasTagCompound()) {
            packet.getTagCompound().setLong(TAG_FE, fe);
        }
        return packet;
    }

    public static appeng.api.storage.data.IAEItemStack createAE(long fe) {
        try {
            appeng.api.storage.data.IAEItemStack base = appeng.util.item.AEItemStack.fromItemStack(getBasePacket());
            
            if (base != null && fe > 0) {
                base.setStackSize(fe);
            }
            
            return base;
        } catch (Exception e) {
            FluxApplied.getLogger().error("Failed to create AE item stack for flux packet", e);
            return null;
        }
    }

    public static boolean isFluxPacket(ItemStack is) {
        return !is.isEmpty() && is.getItem() instanceof ItemFluxPacket;
    }

    public static long getFE(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return 0;
        }
        return stack.getTagCompound().getLong(TAG_FE);
    }

    public static boolean isDisplayOnly(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return false;
        }
        return stack.getTagCompound().getBoolean(TAG_DISPLAY_ONLY);
    }

    private String getFEName() {
        return "\u00a76" + I18n.translateToLocal("item.flux_applied.flux_packet.display_name");
    }
}