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
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends AppCompatActivity {
    ArrayList<String> titles;
    ArrayList<String> subtitles;
    ArrayList<String> links;
    ArrayList<String> image_links;
    ArrayList<String> newspaperNames;
    LinearLayout latestNewsLayout;
    String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:221.0) Gecko/20100101 Firefox/31.0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        titles = new ArrayList<>();
        subtitles = new ArrayList<>();
        links = new ArrayList<>();
        image_links = new ArrayList<>();
        newspaperNames = new ArrayList<>();
        latestNewsLayout = findViewById(R.id.newsLayout);

        // toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // fetch rss
        new BackgroundFetch().execute();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    public InputStream getInputStream(URL url){
        try{
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(100);
            urlConnection.setRequestProperty("User-Agent", "irrelevant");
            int responseCode = urlConnection.getResponseCode();
            if (responseCode < HttpsURLConnection.HTTP_BAD_REQUEST){
                if (responseCode > 300){
                    String newUrl = urlConnection.getHeaderField("Location");
                    url = new URL(newUrl);
                    HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
                    httpsURLConnection.setConnectTimeout(50);
                    httpsURLConnection.setRequestProperty("User-Agent", userAgent);
                    responseCode = httpsURLConnection.getResponseCode();
                    if (responseCode > HttpURLConnection.HTTP_BAD_REQUEST) {
                        return null;
                    }
                    return httpsURLConnection.getInputStream();
                }
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
//                URL url = new URL("https://www.ilfoglio.it/esteri/rss.xml");
//                URL url = new URL("https://weeeopen.polito.it/rss");

                // generate xml parser
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(false);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(getInputStream(url), "UTF_8");

                boolean insideItem = false;
                boolean insideChannel = false;
                boolean foundSubtitle = false;
                int eventType = xpp.getEventType();

                String newspaper = "";
                while (eventType != XmlPullParser.END_DOCUMENT){
                    if (eventType == XmlPullParser.START_TAG){
                        if (xpp.getName().equalsIgnoreCase("item")) {
                            insideItem = true;
                        }
                        if (xpp.getName().equalsIgnoreCase("channel")) {
                            insideChannel = true;
                        }
                        else if (xpp.getName().equalsIgnoreCase("title")){
                            if (insideItem){
                                titles.add(xpp.nextText());
                                newspaperNames.add(newspaper);
                            } else if (insideChannel){
                                newspaper = xpp.nextText();
                            }
                        }
                        else if (xpp.getName().equalsIgnoreCase("atom:summary")){
                            if (insideItem){
                                String asd = xpp.nextText();
                                subtitles.add(asd);
                                foundSubtitle = true;
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
                        else if (xpp.getName().equalsIgnoreCase("enclosure")){
                            if (insideItem) {
                                image_links.add(xpp.getAttributeValue("", "url"));
                            }
                        }
                        if (image_links.size() < titles.size()){
                            image_links.add("");
                        }
                    }
                    else if (eventType == XmlPullParser.END_TAG && xpp.getName().equalsIgnoreCase("item")){
                        insideItem = false;
                        if (!foundSubtitle){
                            subtitles.add("");
                        }
                        foundSubtitle = false;
                    }
                    else if (eventType == XmlPullParser.END_TAG && xpp.getName().equalsIgnoreCase("channel")){
                        insideChannel = false;
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
                News text = new News(MainActivity.this,
                        titles.get(i),
                        subtitles.get(i),
                        links.get(i),
                        image_links.get(i),
                        newspaperNames.get(i));
                latestNewsLayout.addView(text);
            }

            progressDialog.dismiss();
        }
    }
}

class News extends LinearLayout {
    TextView titleView = new TextView(this.getContext());
    TextView newspaperView = new TextView(this.getContext());
    String titleText;
    String subtitleText;
    String bodyText = "No text";
    String articleLink;
    String imageLink;
    String newspaperName;
    Context context;

    public News(Context parent_context, String title, String subtitle, String link, String image_link, String newspaper_name){
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