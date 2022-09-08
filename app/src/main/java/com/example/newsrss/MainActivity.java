package com.example.newsrss;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.room.Room;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;


public class MainActivity
        extends AppCompatActivity
        implements AddFeedDialog.AddFeedDialogListener,
        RemoveFeedDialog.RemoveFeedDialogListener {
    ArrayList<HashMap<String, String>> articles = new ArrayList<>();

    SwipeRefreshLayout swipeRefreshLayout;
    LinearLayout newsLayout;

    FeedDatabase db;
    FeedDatabase.FeedDatabaseDao feedDao;
    List<FeedDatabase.Feed> feeds;

    String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:221.0) Gecko/20100101 Firefox/31.0";
    int connectionTimeout = 10; // seconds

    // Overrides

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        newsLayout = findViewById(R.id.newsLayout);

        // toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // swipe refresh setup
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        newsLayout.removeAllViews();
                        articles.clear();
                        feeds = feedDao.getAll();
                        new BackgroundFetch().execute();
                    }
                }
        );

        // database
        db = Room.databaseBuilder(getApplicationContext(),
                FeedDatabase.class,
                "newsRSS")
                .allowMainThreadQueries()
                .build();
        feedDao = db.feedDatabaseDao();
        feeds = feedDao.getAll();

        // fetch rss
        new BackgroundFetch().execute();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_add:
                ask_new_feed();
                return true;
            case R.id.action_remove:
                remove_feed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onAddFeedDialogPositiveClick(DialogFragment dialog, String feedName, String feedUrl) {
        // save new feed
        FeedDatabase.Feed new_feed = new FeedDatabase.Feed();
        new_feed.feedName = feedName;
        new_feed.feedUrl = feedUrl;
        feedDao.insert(new_feed);
    }

    @Override
    public void onRemoveFeedDialogSelect(DialogFragment dialog, String selected_feed){
        // remove feed
        feedDao.deleteByUrl(selected_feed);
    }

    // Methods

    public InputStream getInputStream(URL url){
        try{
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(connectionTimeout * 1000);
            urlConnection.setRequestProperty("User-Agent", "irrelevant");
            int responseCode = urlConnection.getResponseCode();
            if (responseCode < HttpsURLConnection.HTTP_BAD_REQUEST){
                if (responseCode > 300){
                    String newUrl = urlConnection.getHeaderField("Location");
                    url = new URL(newUrl);
                    HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
                    httpsURLConnection.setConnectTimeout(connectionTimeout * 1000);
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
                    newsLayout.addView(text);
                }
            });
            return null;

        }
    }

    private void ask_new_feed() {
        DialogFragment addFeedDialog = new AddFeedDialog();
        addFeedDialog.show(getSupportFragmentManager(), "dialog");

    }

    private void remove_feed() {
        ArrayList<String> feedNames = new ArrayList<String>();
        ArrayList<String> feedUrls = new ArrayList<String>();
        List<FeedDatabase.Feed> feeds = feedDao.getAll();

        for (FeedDatabase.Feed feed: feeds){
            feedNames.add(feed.feedName);
            feedUrls.add(feed.feedUrl);
        }

        DialogFragment removeFeedDialog = new RemoveFeedDialog(feedNames, feedUrls);
        removeFeedDialog.show(getSupportFragmentManager(), "dialog");
    }

    // Background tasks

    public class BackgroundFetch extends AsyncTask<Integer, Void, Exception>{

        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        Exception exception = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Loading RSS feed...");
//            progressDialog.show();
            swipeRefreshLayout.setRefreshing(true);

        }

        @Override
        protected Exception doInBackground(Integer... integers) {
            for (FeedDatabase.Feed feed: feeds){
                try{
                    URL url = new URL(feed.feedUrl);

                    // generate xml parser
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    factory.setNamespaceAware(false);
                    XmlPullParser xpp = factory.newPullParser();
                    xpp.setInput(getInputStream(url), "UTF_8");

                    boolean insideItem = false;
                    boolean insideChannel = false;
                    int eventType = xpp.getEventType();

                    String newspaper = "";
                    HashMap<String, String> article = null;
                    while (eventType != XmlPullParser.END_DOCUMENT){
                        if (eventType == XmlPullParser.START_TAG){
                            String name = xpp.getName().toLowerCase();
                            switch(name){
                                case "item":
                                    insideItem = true;
                                    article = new HashMap<>();
                                    article.put("title", "");
                                    article.put("subtitle", "");
                                    article.put("link", "");
                                    article.put("image_link", "");
                                    article.put("newspaper", "");
                                    article.put("date", "");
                                    break;
                                case "channel":
                                    insideChannel = true;
                                    break;
                                case "title":
                                    if (insideItem){
                                        article.put("title", xpp.nextText());
                                        article.put("newspaper", newspaper);
                                    } else if (insideChannel){
                                        newspaper = xpp.nextText();
                                    }
                                    break;
                                case "atom:summary":
                                    if (insideItem){
                                        article.put("subtitle", xpp.nextText());
                                    }
                                    break;
                                case "pubDate":
                                    if (insideItem){
                                        article.put("date", xpp.nextText());
                                    }
                                    break;
                                case "link":
                                    if (insideItem) {
                                        article.put("link", xpp.nextText());
                                    }
                                    break;
                                case "media:content":
                                case "enclosure":
                                    if (insideItem) {
                                        article.put("image_link", xpp.getAttributeValue("", "url"));
                                    }
                                    break;
                            }
                        }
                        else if (eventType == XmlPullParser.END_TAG) {
                            String tag = xpp.getName().toLowerCase();
                            switch (tag) {
                                case "item":
                                    insideItem = false;
                                    articles.add(article);
                                    break;
                                case "channel":
                                    insideChannel = false;
                                    break;
                                default:
                                    break;
                            }
                        }

                        eventType = xpp.next();
                    }
                } catch (XmlPullParserException | IOException | IllegalArgumentException exc){
                    exception = exc;
                }
            }

            return exception;
        }

        @Override
        protected void onPostExecute(Exception s) {
            super.onPostExecute(s);

            // add articles to view

            for (HashMap<String, String> article: articles){
                News news_article = new News(MainActivity.this,
                        article.get("title"),
                        article.get("subtitle"),
                        article.get("link"),
                        article.get("image_link"),
                        article.get("newspaper"));
                newsLayout.addView(news_article);
            }

            swipeRefreshLayout.setRefreshing(false);
        }
    }

//    public class DatabaseAccess extends AsyncTask<Integer, Void, Exception>{
//        @Override
//        protected void onPreExecute(){
//
//        }
//
//        @Override
//        protected Exception doInBackground(Integer... integers){
//
//        }
//
//        @Override
//        protected void onPostExecute(Exception s){
//
//        }
//    }

}
