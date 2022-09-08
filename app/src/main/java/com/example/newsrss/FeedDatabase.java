package com.example.newsrss;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.RoomDatabase;

import java.util.List;


@Database(entities = {FeedDatabase.Feed.class}, version = 1)
public abstract class FeedDatabase extends RoomDatabase {
    public abstract FeedDatabaseDao feedDatabaseDao();

    @Entity(tableName = "feed")
    public static class Feed {
        @PrimaryKey
        @NonNull
        public String feedUrl;

        @ColumnInfo(name = "feed_name")
        public String feedName;
    }

    @Dao
    public interface FeedDatabaseDao {
        @Query("SELECT * FROM feed")
        List<Feed> getAll();

        @Query("SELECT feedUrl FROM feed WHERE feed_name LIKE :feedName")
        Feed findByName(String feedName);

        @Insert
        void insert(Feed feed);

        @Query("DELETE FROM feed WHERE feedUrl = :feed_url")
        void deleteByUrl(String feed_url);

    }
}





