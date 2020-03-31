package com.littlejie.richtext;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ReplacementSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

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
        setSpanText(textView, text, false, null);
    }

    public void setSpanText(TextView textView, String text, OnHrefClickListener listener) {
        setSpanText(textView, text, false, listener);
    }

    public void setSpanText(TextView textView, String text,
                            boolean withLineSpacingExtra) {
        setSpanText(textView, text, withLineSpacingExtra, null);
    }

    public void setSpanText(TextView textView, String text,
                            boolean withLineSpacingExtra,
                            OnHrefClickListener listener) {
        textView.setText(getSpannableString(textView.getContext(),
                text, listener, withLineSpacingExtra));
        if (text.contains("href")) {
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }
        //去掉按压高亮
        textView.setHighlightColor(Color.parseColor("#00000000"));
    }

    public Spannable getSpannableString(Context context, String text) {
        return getSpannableString(context, text, null, false);
    }

    public Spannable getSpannableString(Context context, String text,
                                        OnHrefClickListener listener,
                                        boolean withLineSpacingExtra) {
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
                            listener,
                            withLineSpacingExtra));
                }
                builder.append(getSpanText(context,
                        text.substring(attrStart, attrEnd + END_TAG.length()),
                        listener,
                        withLineSpacingExtra));
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
                                     OnHrefClickListener listener,
                                     boolean withLineSpacingExtra)
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
                case TEXT_DECORATION_STYLE:
                case TEXT_DECORATION_COLOR:
                    setTextSpan(context, wordsToSpan, length, listener,
                            withLineSpacingExtra, spanMap);
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
                             boolean withLineSpacingExtra,
                             Map<String, String> spanMap) {
        final TextSpanAttr attr = TextSpanAttr.getDefault(context);
        for (String key : spanMap.keySet()) {
            final String value = spanMap.get(key);
            if (TextUtils.isEmpty(value)) {
                continue;
            }
            RichTextType type = RichTextType.fromName(key);
            switch (type) {
                case TEXT_COLOR:
                    attr.textColor = Color.parseColor(value);
                    spanMap.remove(key);
                    break;
                case TEXT_DECORATION_LINE:
                    attr.hasUnderline = "underline".equals(value);
                    spanMap.remove(key);
                    break;
                case TEXT_DECORATION_STYLE:
                    attr.isDashUnderline = "dashed".equals(value);
                    spanMap.remove(key);
                    break;
                case TEXT_DECORATION_COLOR:
                    attr.underlineColor = Color.parseColor(value);
                    spanMap.remove(key);
                    break;
                default:
                    break;
            }
        }

        //有href标签，即代表有下划线。
        //若underlineColor=0，则默认取字体颜色
        for (String key : spanMap.keySet()) {
            final String value = spanMap.get(key);
            if (TextUtils.isEmpty(value)) {
                continue;
            }
            if (RichTextType.HREF.getName().equals(key)) { //超链接
                attr.value = value;
                attr.hasUnderline = true;
                if (attr.underlineColor == 0) {
                    attr.underlineColor = attr.textColor;
                }
                spanMap.remove(key);
            }
        }

        if (!attr.hasUnderline) { //没有下划线
            wordsToSpan.setSpan(new ForegroundColorSpan(attr.textColor),
                    0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (!attr.isDashUnderline
                && attr.textColor == attr.underlineColor) { //下划线颜色==字体颜色
            wordsToSpan.setSpan(new ForegroundColorSpan(attr.textColor),
                    0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            wordsToSpan.setSpan(new CustomClickableSpan(context, attr, listener),
                    0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else { //下划线颜色!=字体颜色
            wordsToSpan.setSpan(new CustomClickableSpan(context, attr, listener),
                    0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            wordsToSpan.setSpan(new CustomUnderlineSpan(attr.textColor,
                            attr.underlineColor, attr.isDashUnderline, withLineSpacingExtra),
                    0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void setFontFamilySpan(Context context, Spannable wordsToSpan,
                                   int length, String fontFamily) {
        //iOS字体加粗DINCondensed-Bold
        int index = fontFamily.indexOf("-");
        String name = index == -1 ? fontFamily : fontFamily.substring(0, index);
        //todo 优化使用注释
        Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/" + name + ".ttf");
        if (typeface != null) {
            wordsToSpan.setSpan(Typeface.DEFAULT,
                    0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
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

    private static class CustomClickableSpan extends ClickableSpan {

        private Context context;
        private TextSpanAttr attr;
        private OnHrefClickListener listener;

        CustomClickableSpan(Context context, TextSpanAttr attr, OnHrefClickListener listener) {
            this.context = context;
            this.attr = attr;
            this.listener = listener;
        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            super.updateDrawState(ds);
            ds.setColor(attr.underlineColor);
        }

        @Override
        public void onClick(@NonNull View widget) {
            if (listener != null) {
                listener.onClick(attr.value);
            }
        }
    }

    /**
     * ReplacementSpan 用于整体替换，当文本长度过长的时候不会自动换行.
     * 故，使用时该Span的step=1.
     * 对于下划线为虚线的，目前没有很好的方法解决虚线点间的重叠，故step=length.
     * 后期有合适方法需要优化.
     */
    private static class CustomUnderlineSpan extends ReplacementSpan {
        private Paint paint;
        private int textColor;
        private int textWidth;
        private float dashInterval = 4;
        private boolean withLineSpacingExtra;

        CustomUnderlineSpan(int textColor, int color,
                            boolean isDash, boolean withLineSpacingExtra) {
            this.textColor = textColor;
            this.withLineSpacingExtra = withLineSpacingExtra;
            paint = new Paint();
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setPathEffect(isDash
                    ? new DashPathEffect(new float[]{dashInterval, dashInterval}, 0)
                    : null);
            paint.setStrokeWidth(dpToPx(1f));
        }

        @Override
        public int getSize(@NonNull Paint paint, CharSequence text,
                           int start, int end, Paint.FontMetricsInt fm) {
            textWidth = (int) paint.measureText(text, start, end);
            return textWidth;
        }

        @Override
        public void draw(@NonNull Canvas canvas, CharSequence text,
                         int start, int end, float x, int top, int y, int bottom,
                         @NonNull Paint paint) {
            if (textColor != 0) {
                paint.setColor(textColor);
            }
            canvas.drawText(text, start, end, x, y, paint);
            float realBottom = bottom;
            if (withLineSpacingExtra) { //有行距，下划线位置可能不对，特殊处理
                Paint.FontMetrics fontMetrics = paint.getFontMetrics();
                float height = fontMetrics.descent - fontMetrics.ascent;
                realBottom = top + height;
            }
            Path path = new Path();
            path.moveTo(x, realBottom);
            path.lineTo(x + textWidth, realBottom);
            canvas.drawPath(path, this.paint);
        }
    }

    private static class TextSpanAttr {
        private int underlineColor = 0;
        private int textColor = 0;
        private boolean isDashUnderline = false;
        private boolean hasUnderline = false;
        private String value;

        static TextSpanAttr getDefault(Context context) {
            TextSpanAttr attr = new TextSpanAttr();
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
            attr.textColor = typedValue.data;
            return attr;
        }
    }

    private static int dpToPx(float dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
