package escapeRoomEngine;

/*
 * Item
 * - breakable : boolean
 * - useCount  : int
 * + use() : void
 * + canBreak() : boolean
 */
public class Item extends Article {
    private boolean breakable;
    private int useCount;

    public Item() {
        super();
        this.breakable = false;
        this.useCount = 0;
    }

    public Item(String name, String desc, boolean breakable) {
        super(name, desc);
        this.breakable = breakable;
        this.useCount = 0;
    }

    public void use() {
        useCount++;
    }

    public boolean canBreak() {
        return breakable;
    }
}
