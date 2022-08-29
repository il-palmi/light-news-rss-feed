package com.example.newsrss;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class ActivityArticle extends AppCompatActivity {
    Bundle extras;
    String titleText;
    String subtitleText;
    String bodyUrl;
    String bodyText;
    String imageUrl;
    TextView bodyView;
    ImageView imageView;
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        // initialize values from parent
        extras = getIntent().getExtras();
        titleText = extras.getString("titleText");
        subtitleText = extras.getString("subtitleText");
        bodyUrl = extras.getString("articleLink");
        imageUrl = extras.getString("imageLink");
        bodyText = "";
        imageView = findViewById(R.id.imageView);

        // set body text
        TextView titleView = findViewById(R.id.titleText);
        titleView.setText(titleText);
        TextView subtitleView = findViewById(R.id.subtitleText);
        subtitleView.setText(subtitleText);
        bodyView = findViewById(R.id.bodyText);
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
                    Toast errorToast = Toast.makeText(ActivityArticle.this, exc.getMessage(), Toast.LENGTH_LONG);
                    errorToast.show();
                }
            });
            return null;

        }
    }


    public class BackgroundFetch extends AsyncTask<Integer, Void, Exception> {
        ProgressDialog progressDialog = new ProgressDialog(ActivityArticle.this);
        Exception exception = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Loading article...");
            progressDialog.show();
        }

        @Override
        protected Exception doInBackground(Integer... integers) {
            try{
                // html parser
                Document document = Jsoup.connect(bodyUrl).get();
                Elements text_elements = document.getElementsByTag("p");
                Elements article_image = document.getElementsByTag("img");
                bodyText = text_elements.text();
                URL url = new URL(imageUrl);
                InputStream inputStream = getInputStream(url);
                bitmap = BitmapFactory.decodeStream(inputStream);
            }
            catch (Exception exc){
                Toast toast = Toast.makeText(ActivityArticle.this, exc.getMessage(), Toast.LENGTH_LONG);
                toast.show();
            }

            return exception;
        }

        @Override
        protected void onPostExecute(Exception s) {
            super.onPostExecute(s);

            // set text to textView
            bodyView.setText(bodyText);
            if (bitmap.getHeight() > 200) {
                imageView.setImageBitmap(bitmap);
            }
            else{
                imageView.setImageBitmap(null);
            }

            progressDialog.dismiss();
        }
    }


}