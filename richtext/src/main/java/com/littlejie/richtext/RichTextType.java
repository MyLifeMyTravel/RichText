package com.littlejie.richtext;

public enum RichTextType {
    NONE(""),
    HREF("href"), //超链接
    TEXT_COLOR("text-color"), // 字体颜色
    TEXT_DECORATION_LINE("text-decoration-line"), //是否有下划线, true/false
    FONT_FAMILY("font-family"), //字体
    FONT_SIZE("font-size"), //字体大小,单位px
    FONT_WEIGHT("font-weight"); //字体样式, bold/italic/bold_italic

    private String name;

    RichTextType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static RichTextType fromName(String name) {
        for (RichTextType type : values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return NONE;
    }
}
