package com.flux_applied.item;
public final class ItemFluxStorageComponent extends OptionalFluxItem {
    private final int kilobytes;
    public ItemFluxStorageComponent(int kilobytes) { super(false); this.kilobytes = kilobytes; }
    public int getKilobytes() { return kilobytes; }
}
