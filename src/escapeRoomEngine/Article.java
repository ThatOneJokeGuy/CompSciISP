package escapeRoomEngine;


/*
 * Article
 * - name : String
 * - desc : String
 * + getName() : String
 * + getDesc() : String
 * + setName(name : String) : void
 * + setDesc(desc : String) : void
 */
public class Article {
    private String name;
    private String desc;

    public Article() {
        this.name = "";
        this.desc = "";
    }

    public Article(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
