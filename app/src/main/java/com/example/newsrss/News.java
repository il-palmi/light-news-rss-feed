package com.example.newsrss;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


@SuppressLint("ViewConstructor")
public class News extends LinearLayout {
    TextView titleView = new TextView(this.getContext());
    TextView newspaperView = new TextView(this.getContext());
    String titleText;
    String subtitleText;
    String bodyText = "No text";
    String articleLink;
    String imageLink;
    String newspaperName;
    Context context;

    public News(Context parent_context,
                String title,
                String subtitle,
                String link,
                String image_link,
                String newspaper_name){
        super(parent_context);
        context = parent_context;
        titleText = title;
        subtitleText = subtitle;
        articleLink = link;
        imageLink = image_link;
        newspaperName = newspaper_name;
        setOnClickListener(onClick());

        // graphics parameters
        setOrientation(LinearLayout.VERTICAL);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(30,10,30,10);
        setLayoutParams(params);

        // Newspaper name label
        newspaperView.setText(newspaperName);
        newspaperView.setTypeface(newspaperView.getTypeface(), Typeface.BOLD_ITALIC);
        addView(newspaperView);

        // Title
        titleView.setText(title);
        titleView.setTextSize(24);
        titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD_ITALIC);
        addView(titleView);

        // Subtitle
        if (!subtitleText.equals("")){
            TextView subtitleView = new TextView(context);
            subtitleView.setText(subtitle.replace("\n", ""));
            addView(subtitleView);
        }

        // Spacer
        TextView lineSpacer = new TextView(context);
        lineSpacer.setWidth(this.getWidth());
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, 15, 0, 0);
        lineSpacer.setLayoutParams(lp);
        lineSpacer.setBackgroundColor(Color.BLACK);

        addView(lineSpacer);
    }

    OnClickListener onClick(){
        return new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ActivityArticle.class);
                intent.putExtra("titleText", titleText);
                intent.putExtra("subtitleText", subtitleText);
                intent.putExtra("bodyText", bodyText);
                intent.putExtra("articleLink", articleLink);
                intent.putExtra("imageLink", imageLink);
                context.startActivity(intent);
            }
        };
    }

}