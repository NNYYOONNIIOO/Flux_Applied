package com.flux_applied.item;

import com.flux_applied.FluxApplied;
import com.flux_applied.FluxUpgrades;
import com.flux_applied.IFluxUpgradeModule;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemProviderCard extends Item implements IFluxUpgradeModule {

    public ItemProviderCard() {
        setRegistryName(FluxApplied.MODID, "energy_port_card");
        setUnlocalizedName("flux_applied.energy_port_card");
        setCreativeTab(com.flux_applied.FluxAppliedTab.INSTANCE);
        setMaxStackSize(1);
    }

    @Override
    public String getUpgradeTypeId() {
        return FluxUpgrades.ENERGY_PORT_CARD;
    }

    @Override
    public appeng.api.config.Upgrades getType(ItemStack itemstack) {
        return appeng.api.config.Upgrades.CRAFTING;
    }

    @Override
    public int getMaxInstalled() {
        return 1;
    }

    public static int getMode(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != FluxApplied.providerCard) return 1;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) return 1;
        int mode = tag.getInteger("Mode");
        if (mode < 0 || mode > 2) return 1;
        return mode;
    }

    public static void setMode(ItemStack stack, int mode) {
        if (stack.isEmpty() || stack.getItem() != FluxApplied.providerCard) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger("Mode", mode);
    }

    public static void cycleMode(ItemStack stack) {
        int mode = getMode(stack);
        setMode(stack, (mode + 1) % 3);
    }

    public static void sendModeMessage(EntityPlayer player, int mode) {
        String modeKey;
        switch (mode) {
            case 0: modeKey = "flux_applied.message.card_input_only"; break;
            case 1: modeKey = "flux_applied.message.card_output_only"; break;
            default: modeKey = "flux_applied.message.card_bidirectional"; break;
        }
        player.sendStatusMessage(new TextComponentTranslation(modeKey), true);
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, @Nonnull EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            if (!world.isRemote) {
                cycleMode(stack);
                sendModeMessage(player, getMode(stack));
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        int mode = getMode(stack);
        String modeKey;
        switch (mode) {
            case 0: modeKey = "flux_applied.tooltip.card_mode_input"; break;
            case 1: modeKey = "flux_applied.tooltip.card_mode_output"; break;
            default: modeKey = "flux_applied.tooltip.card_mode_bidirectional"; break;
        }
        tooltip.add(I18n.format(modeKey));

        // Show supported devices from FluxUpgrades registration
        Map<ItemStack, Integer> supported = FluxUpgrades.getSupported(FluxUpgrades.ENERGY_PORT_CARD);
        if (!supported.isEmpty()) {
            List<String> textList = new ArrayList<>();
            for (Map.Entry<ItemStack, Integer> entry : supported.entrySet()) {
                String name = entry.getKey().getDisplayName();
                int limit = entry.getValue();
                if (limit > 1) {
                    name = name + " (" + limit + ')';
                }
                if (!textList.contains(name)) {
                    textList.add(name);
                }
            }
            if (!textList.isEmpty()) {
                tooltip.add("");
                tooltip.addAll(textList);
            }
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }
}
