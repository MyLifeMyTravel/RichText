package com.littlejie.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.StaticLayout;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.littlejie.richtext.OnHrefClickListener;
import com.littlejie.richtext.RichTextManager;

public class MainActivity extends AppCompatActivity {

    public static final String RICH_TEXT = "普通文本\n"
            + "<rich-text>带标签的普通文本</rich-text>\n"
            + "<rich-text text-color='#FF0000'>带颜色的富文本</rich-text>\n"
            + "<rich-text text-color='#00FF00' href='https://www.baidu.com'>带超链接的富文本</rich-text>\n"
            + "<rich-text text-color='#0000FF' font-size='30'>30sp的文字</rich-text>\n"
            + "<rich-text text-color='#FFC125' font-weight='bold'>加粗的字体</rich-text>\n"
            + "<rich-text text-color='#97FFFF' font-family='font-test' font-weight='bold' font-size='25'>25sp、加粗自定义字体</rich-text>\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = findViewById(R.id.tv_text);
        RichTextManager.getInstance().setSpanText(tv, RICH_TEXT, new OnHrefClickListener() {
            @Override
            public void onClick(String href) {
                Toast.makeText(MainActivity.this, "URL：" + href, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
