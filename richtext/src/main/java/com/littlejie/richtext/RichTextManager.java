package com.littlejie.richtext;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import android.widget.TextView;

import com.littlejie.richtext.span.ClickableSpan;
import com.littlejie.richtext.span.TypefaceSpan;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RichTextManager {

    private static final String TAG = RichTextManager.class.getSimpleName();
    private static final String TAG_ELEMENT_TEXT = "element-text";
    private static final String RICH_TEXT_ELEMENT_NAME = "rich-text";
    private static final String START_TAG = "<" + RICH_TEXT_ELEMENT_NAME;
    private static final String END_TAG = "</" + RICH_TEXT_ELEMENT_NAME + ">";

    private static RichTextManager instance;

    private RichTextManager() {
    }

    public static RichTextManager getInstance() {
        if (instance == null) {
            instance = new RichTextManager();
        }
        return instance;
    }

    public void setSpanText(TextView textView, String text) {
        setSpanText(textView, text, null);
    }

    public void setSpanText(TextView textView, String text,
                            OnHrefClickListener listener) {
        textView.setText(getSpannableString(textView.getContext(), text, listener));
        if (text.contains(RichTextType.HREF.getName())) {
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }
        //去掉按压高亮
        textView.setHighlightColor(Color.parseColor("#00000000"));
    }

    public Spannable getSpannableString(Context context, String text) {
        return getSpannableString(context, text, null);
    }

    public Spannable getSpannableString(Context context, String text,
                                        OnHrefClickListener listener) {
        boolean hasOnlyAttr = text.startsWith(START_TAG) && text.endsWith(END_TAG);
        try {
            SpannableStringBuilder builder = new SpannableStringBuilder();

            int attrStart = text.indexOf(START_TAG);
            int attrEnd = text.indexOf(END_TAG);

            if (attrStart == -1 && attrEnd == -1) {
                builder.append(text);
            }
            //字符串起始和第一个标签间可能有无标签文字
            while (attrStart != -1 && attrEnd != -1) {
                if (attrStart != 0) {
                    builder.append(getSpanText(context,
                            text.substring(0, attrStart),
                            listener));
                }
                builder.append(getSpanText(context,
                        text.substring(attrStart, attrEnd + END_TAG.length()),
                        listener));
                text = text.substring(attrEnd + END_TAG.length());
                attrStart = text.indexOf(START_TAG);
                attrEnd = text.indexOf(END_TAG);
                if (attrStart == -1 && attrEnd == -1) {
                    builder.append(text);
                }
            }
            //只有一个标签时，小米手机无法正常显示
            //结尾添加标记字符串结束
            if (hasOnlyAttr) {
                builder.append("\0");
            }
            return builder;
        } catch (IOException e) {
            e.printStackTrace();
            return new SpannableString(text);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            return new SpannableString(text);
        }
    }

    private CharSequence getSpanText(final Context context, String words,
                                     OnHrefClickListener listener)
            throws IOException, XmlPullParserException {
        if (TextUtils.isEmpty(words)) {
            return "";
        }
        if (!words.contains(END_TAG)) {
            return words;
        }

        Map<String, String> spanMap = parserAttribute(words);
        String realText = spanMap.get(TAG_ELEMENT_TEXT);
        if (TextUtils.isEmpty(realText)) {
            return "";
        }

        int length = realText.length();
        Spannable wordsToSpan = new SpannableString(realText);
        for (final String key : spanMap.keySet()) {
            final String value = spanMap.get(key);
            if (TextUtils.isEmpty(value)) {
                continue;
            }
            RichTextType richTextType = RichTextType.fromName(key);
            switch (richTextType) {
                case HREF:
                case TEXT_COLOR:
                case TEXT_DECORATION_LINE:
                    setTextSpan(context, wordsToSpan, length, listener, spanMap);
                    break;
                case FONT_FAMILY:
                    setFontFamilySpan(context, wordsToSpan, length, value);
                    break;
                case FONT_SIZE:
                    setSizeSpan(wordsToSpan, length, value);
                    break;
                case FONT_WEIGHT:
                    setTypefaceStyleSpan(wordsToSpan, length, value);
                    break;
                default:
                    break;
            }
        }
        return wordsToSpan;
    }

    // 返回去除标签的文本，为了本文宽高
    public String getTextWithoutRichTag(String words) {
        StringBuilder sb = new StringBuilder();
        List<String> splitList = split(words);
        for (String subString : splitList) {
            sb.append(subString);
        }
        return sb.toString();
    }

    private List<String> split(String words) {
        List<String> splitList = new ArrayList<>();
        if (words.contains(START_TAG)) {
            String[] split = words.split(String.format("(%s).*?(>)", START_TAG));
            for (String sub : split) {
                splitList.addAll(split(sub));
            }
        } else if (words.contains(END_TAG)) {
            String[] split = words.split(END_TAG);
            for (String sub : split) {
                splitList.addAll(split(sub));
            }
        } else {
            splitList.add(words);
        }
        return splitList;
    }

    private void setTextSpan(final Context context, Spannable wordsToSpan,
                             int length, OnHrefClickListener listener,
                             Map<String, String> spanMap) {
        int textColor = getPrimaryColor(context);
        boolean hasUnderline = false;
        for (String key : spanMap.keySet()) {
            final String value = spanMap.get(key);
            if (TextUtils.isEmpty(value)) {
                continue;
            }
            RichTextType type = RichTextType.fromName(key);
            switch (type) {
                case TEXT_COLOR:
                    textColor = Color.parseColor(value);
                    spanMap.remove(key);
                    break;
                case TEXT_DECORATION_LINE:
                    hasUnderline = Boolean.parseBoolean(value);
                    spanMap.remove(key);
                    break;
                default:
                    break;
            }
        }

        String url = null;
        //有href标签，即代表有下划线。
        for (String key : spanMap.keySet()) {
            final String value = spanMap.get(key);
            if (TextUtils.isEmpty(value)) {
                continue;
            }
            if (RichTextType.HREF.getName().equals(key)) { //超链接
                url = value;
                hasUnderline = true;
                spanMap.remove(key);
            }
        }

        if (!TextUtils.isEmpty(url)) {
            wordsToSpan.setSpan(new ClickableSpan(url, textColor, listener),
                    0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (hasUnderline) {
            wordsToSpan.setSpan(new UnderlineSpan(), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            //设置字体颜色
            wordsToSpan.setSpan(new ForegroundColorSpan(textColor),
                    0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void setFontFamilySpan(Context context, Spannable wordsToSpan,
                                   int length, String fontFamily) {
        Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/" + fontFamily + ".ttf");
        wordsToSpan.setSpan(new TypefaceSpan(typeface), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // 设置字体大小
    private void setSizeSpan(Spannable wordsToSpan, int length, String value) {
        wordsToSpan.setSpan(new AbsoluteSizeSpan(
                        dpToPx(Integer.parseInt(value))), 0, length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // 设置字体样式
    private void setTypefaceStyleSpan(Spannable wordsToSpan, int length,
                                      String typefaceStyle) {
        int style;
        switch (typefaceStyle) {
            case "bold":
                style = Typeface.BOLD;
                break;
            case "italic":
                style = Typeface.ITALIC;
                break;
            case "bold_italic":
                style = Typeface.BOLD_ITALIC;
                break;
            default:
                style = Typeface.NORMAL;
                break;
        }
        wordsToSpan.setSpan(new StyleSpan(style),
                0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private Map<String, String> parserAttribute(String xml)
            throws XmlPullParserException, IOException {
        Map<String, String> map = new ConcurrentHashMap<>();
        //创建XmlPullParser
        XmlPullParser parser = Xml.newPullParser();
        //初始化XmlPull解析器
        parser.setInput(new StringReader(xml));
        //读取标签类型
        int type = parser.getEventType();
        while (type != XmlPullParser.END_DOCUMENT) {
            Log.d(TAG, "type: " + type + "; name: " + parser.getName());
            switch (type) {
                case XmlPullParser.START_TAG:
                    Log.d(TAG, "start tag");
                    if (RICH_TEXT_ELEMENT_NAME.equals(parser.getName())) {
                        int count = parser.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            String attributeName = parser.getAttributeName(i);
                            String attributeValue = parser.getAttributeValue(i);
                            map.put(attributeName, attributeValue);
                            Log.d(TAG, "index: " + i + "; name: " + attributeName
                                    + "; value: " + attributeValue);
                        }
                    }
                    break;
                case XmlPullParser.TEXT:
                    String text = parser.getText();
                    Log.d(TAG, "tag text: " + text);
                    map.put(TAG_ELEMENT_TEXT, text);
                    break;
                case XmlPullParser.END_TAG:
                    Log.d(TAG, "end tag");
                    break;
                default:
                    break;
            }
            type = parser.next();
        }
        return map;
    }

    private static int getPrimaryColor(Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
        return typedValue.data;
    }

    private static int dpToPx(float dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
