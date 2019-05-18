package com.lucianms.client.inventory;

import com.lucianms.constants.ItemConstants;

/**
 * @author kevin
 */
public class ModifyInventory {

    private int mode;
    private Item item;
    private short oldPos;

    /**
     * mode types
     * <ol start=0>
     * <li>add</li>
     * <li>update</li>
     * <li value=3>remove</li>
     * </ol>
     */
    public ModifyInventory(final int mode, final Item item) {
        this.mode = mode;
        this.item = item.copy();
    }

    public ModifyInventory(final int mode, final Item item, final short oldPos) {
        this.mode = mode;
        this.item = item.copy();
        this.oldPos = oldPos;
    }

    public final int getMode() {
        return mode;
    }

    public final int getInventoryType() {
        return ItemConstants.getInventoryType(item.getItemId()).getType();
    }

    public final short getPosition() {
        return item.getPosition();
    }

    public final short getOldPosition() {
        return oldPos;
    }

    public final short getQuantity() {
        return item.getQuantity();
    }

    public final Item getItem() {
        return item;
    }

    public final void clear() {
        this.item = null;
    }
}