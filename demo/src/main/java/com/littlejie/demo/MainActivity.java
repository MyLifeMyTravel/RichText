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

    public static final String RICH_TEXT = "测试<rich-text font-weight='bold' font-size='20'>加粗</rich-text><rich-text href='https://www.baidu.com' text-color='#000000' text-decoration-color='#00FF88'>链接1>?</rich-text> <rich-text href='https://www.w3school.com.cn/index.html' text-decoration-style='dashed'>链接2链接2链接2链接2链接2链接2链接2链接2链接2链接2链接2链接2链接2链接2链接2链接2链接2链接2链接2链接2链接2</rich-text>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = findViewById(R.id.tv_text);
        RichTextManager.getInstance().setSpanText(tv, RICH_TEXT, false, new OnHrefClickListener() {
            @Override
            public void onClick(String href) {
                Toast.makeText(MainActivity.this, href, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
