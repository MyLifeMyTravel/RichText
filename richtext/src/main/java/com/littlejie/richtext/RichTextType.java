package com.littlejie.richtext;

public enum RichTextType {
    NONE(""),
    HREF("href"), //超链接
    TEXT_COLOR("text-color"), // 字体颜色
    TEXT_DECORATION_LINE("text-decoration-line"), //是否有下划线
    TEXT_DECORATION_STYLE("text-decoration-style"), //下划线样式，dashed/solid
    TEXT_DECORATION_COLOR("text-decoration-color"), //下划线颜色,#FFFFFF
    FONT_FAMILY("font-family"), //字体
    FONT_SIZE("font-size"), //字体大小,单位px
    FONT_WEIGHT("font-weight"); //字体样式

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
