package com.flux_applied.part;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.parts.PartItemStack;
import appeng.parts.BasicStatePart;
import appeng.util.SettingsFrom;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;

/** AE2 8's base part keeps its settings methods private, so preserve our custom fields here. */
public abstract class FluxPart extends BasicStatePart {
    protected FluxPart(ItemStack stack) { super(stack); }
    @Override public boolean useStandardMemoryCard() { return false; }
    protected CompoundNBT downloadSettings(SettingsFrom from, CompoundNBT output) { return output; }
    public void uploadSettings(SettingsFrom from, CompoundNBT tag, PlayerEntity player) {}
    @Override public boolean onPartShiftActivate(PlayerEntity player, Hand hand, Vector3d hit) {
        return useFluxMemoryCard(player, hand);
    }
    protected boolean useFluxMemoryCard(PlayerEntity player, Hand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (!(held.getItem() instanceof IMemoryCard)) return false;
        if (isRemote()) return true;
        IMemoryCard card = (IMemoryCard) held.getItem();
        String name = getItemStack(PartItemStack.NETWORK).getTranslationKey();
        if (player.isSneaking()) {
            card.setMemoryCardContents(held, name, downloadSettings(SettingsFrom.MEMORY_CARD, new CompoundNBT()));
            card.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED);
        } else if (name.equals(card.getSettingsName(held))) {
            uploadSettings(SettingsFrom.MEMORY_CARD, card.getData(held), player);
            saveChanges();
            getHost().markForUpdate();
            card.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
        } else {
            card.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
        }
        return true;
    }
}
