package com.example.newsrss;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    ArrayList<String> titles;
    ArrayList<String> subtitles;
    ArrayList<String> links;
    ArrayList<String> image_links;
    LinearLayout latestNewsLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        titles = new ArrayList<>();
        subtitles = new ArrayList<>();
        links = new ArrayList<>();
        image_links = new ArrayList<>();
        latestNewsLayout = findViewById(R.id.newsLayout);

        // toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // fetch rss
        new BackgroundFetch().execute();

    }

    public InputStream getInputStream(URL url){
        try{
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            int responseCode = urlConnection.getResponseCode();
            if (responseCode < HttpURLConnection.HTTP_BAD_REQUEST){
                return urlConnection.getInputStream();
            }
            else{
                return null;
            }
        }
        catch (Exception exc){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView text = new TextView(MainActivity.this);
                    text.setText(exc.getMessage());
                    latestNewsLayout.addView(text);
                }
            });
            return null;

        }
    }

    public class BackgroundFetch extends AsyncTask<Integer, Void, Exception>{

        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        Exception exception = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Loading RSS feed...");
            progressDialog.show();
        }

        @Override
        protected Exception doInBackground(Integer... integers) {
            try{
//                URL url = new URL("http://www.repubblica.it/rss/esteri/rss2.0.xml");
                URL url = new URL("http://feed.lastampa.it/esteri.rss");

                // generate xml parser
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(false);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(getInputStream(url), "UTF_8");

                boolean insideItem = false;  //
                int eventType = xpp.getEventType();

                while (eventType != XmlPullParser.END_DOCUMENT){
                    if (eventType == XmlPullParser.START_TAG){
                        if (xpp.getName().equalsIgnoreCase("item")) {
                            insideItem = true;
                        }
                        else if (xpp.getName().equalsIgnoreCase("title")){
                            if (insideItem){
                                titles.add(xpp.nextText());
                            }
                        }
                        else if (xpp.getName().equalsIgnoreCase("atom:summary")){
                            if (insideItem){
                                subtitles.add(xpp.nextText());

                            }
                        }
                        else if (xpp.getName().equalsIgnoreCase("link")){
                            if (insideItem) {
                                links.add(xpp.nextText());
                            }
                        }
                        else if (xpp.getName().equalsIgnoreCase("media:content")){
                            if (insideItem) {
                                image_links.add(xpp.getAttributeValue("", "url"));
                            }
                        }

                    }
                    else if (eventType == XmlPullParser.END_TAG && xpp.getName().equalsIgnoreCase("item")){
                        insideItem = false;
                    }

                    eventType = xpp.next();
                }
            } catch (XmlPullParserException | IOException | IllegalArgumentException exc){
                exception = exc;
            }

            return exception;
        }

        @Override
        protected void onPostExecute(Exception s) {
            super.onPostExecute(s);

            // add articles to view

            for (int i=0; i<titles.size(); i++){

                News text = new News(MainActivity.this, titles.get(i), subtitles.get(i), links.get(i), image_links.get(i));
                latestNewsLayout.addView(text);
            }

            progressDialog.dismiss();
        }
    }
}

class News extends LinearLayout {
    TextView titleView = new TextView(this.getContext());
    String titleText;
    String subtitleText;
    String bodyText = "No text";
    String articleLink;
    String imageLink;
    Context context;

    public News(Context parent_context, String title, String subtitle, String link, String image_link){
        super(parent_context);
        context = parent_context;
        titleText = title;
        subtitleText = subtitle;
        articleLink = link;
        imageLink = image_link;
        setOnClickListener(onClick());

        // graphics parameters
        setOrientation(LinearLayout.VERTICAL);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(30,10,30,10);
        setLayoutParams(params);

        // Title
        titleView.setText(title);
        titleView.setTextSize(24);
        titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD_ITALIC);
        addView(titleView);

        // Subtitle
        TextView subtitleView = new TextView(context);
        subtitleView.setText(subtitle);
        addView(subtitleView);

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
        OnClickListener listener = new OnClickListener() {
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
        return listener;
    }

}