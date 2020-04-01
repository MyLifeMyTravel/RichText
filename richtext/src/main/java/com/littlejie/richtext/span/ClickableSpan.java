package com.littlejie.richtext.span;

import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.View;

import androidx.annotation.NonNull;

import com.littlejie.richtext.OnHrefClickListener;

public class ClickableSpan extends URLSpan {

    private int color;
    private OnHrefClickListener listener;

    public ClickableSpan(String url, int color, OnHrefClickListener listener) {
        super(url);
        this.color = color;
        this.listener = listener;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        super.updateDrawState(ds);
        ds.setColor(color);
    }

    @Override
    public void onClick(@NonNull View widget) {
        if (listener != null) {
            listener.onClick(getURL());
        }
    }
}
