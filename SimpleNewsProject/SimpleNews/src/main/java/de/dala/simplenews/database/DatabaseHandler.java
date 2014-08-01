package de.dala.simplenews.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import de.dala.simplenews.common.Category;
import de.dala.simplenews.common.Entry;
import de.dala.simplenews.common.Feed;
import de.dala.simplenews.common.News;
import de.dala.simplenews.parser.XmlParser;
import de.dala.simplenews.utilities.PrefUtilities;

/**
 * The DatabaseHandler for the communication between Client and Client-Database
 *
 * @author Daniel Langerenken based on
 *         http://www.androidhive.info/2011/11/android-sqlite-database-tutorial/
 */
public class DatabaseHandler extends SQLiteOpenHelper implements
        IDatabaseHandler {

    /**
     * Database Name and Version
     */
    private static final int DATABASE_VERSION = 43;
    private static final String DATABASE_NAME = "news_database";

    /**
     * Table names
     */
    public static final String TABLE_CATEGORY = "category";
    public static final String TABLE_FEED = "feed";
    public static final String TABLE_ENTRY = "entry";
    public static final String CATEGORY_ID = "_id";
    public static final String CATEGORY_COLOR = "color";
    public static final String CATEGORY_NAME = "name";
    public static final String CATEGORY_VISIBLE = "visible";
    public static final String CATEGORY_LAST_UPDATE = "last_update";
    public static final String CATEGORY_ORDER = "_order";

    public static final String FEED_ID = "_id";
    public static final String FEED_CATEGORY_ID = "category_id";
    public static final String FEED_TITLE = "title";
    public static final String FEED_DESCRIPTION = "description";
    public static final String FEED_URL = "url";
    public static final String FEED_HTML_URL = "html_url";
    public static final String FEED_VISIBLE = "visible";

    public static final String ENTRY_ID = "_id";
    public static final String ENTRY_CATEGORY_ID = "category_id";
    public static final String ENTRY_FEED_ID = "feed_id";
    public static final String ENTRY_TITLE = "title";
    public static final String ENTRY_DESCRIPTION = "description";
    public static final String ENTRY_DATE = "date";
    public static final String ENTRY_SRC_NAME = "src_name";
    public static final String ENTRY_URL = "url";
    public static final String ENTRY_SHORTENED_URL = "shortened_url";
    public static final String ENTRY_IMAGE_URL = "image_url";
    public static final String ENTRY_VISIBLE = "visible";
    public static final String ENTRY_VISITED_DATE = "visited";
    public static final String ENTRY_FAVORITE_DATE = "favorite";
    public static final String ENTRY_IS_EXPANDED = "expanded";

    private static SQLiteDatabase db;
    private static DatabaseHandler instance;
    private Context mContext;

    private DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    /**
     * @return the singleton instance.
     */
    public static synchronized DatabaseHandler getInstance() {
        return instance;
    }
    public static synchronized SQLiteDatabase getDbInstance() {
        return db;
    }


    public static String concatenateQueries(String query, String additionalQuery) {
        if (query == null) {
            return additionalQuery;
        }
        return query + " AND " + additionalQuery;
    }

    /*
    * Retrieves a thread-safe instance of the singleton object {@link DatabaseHandler} and opens the database
    * with writing permissions.
    *
    * @param context the context to set.
    */
    public static void init(Context context) {
        if (instance == null) {
            instance = new DatabaseHandler(context);
            db = instance.getWritableDatabase();
        }
    }

    /*
     * (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#close()
     */
    @Override
    public synchronized void close() {
        if (instance != null) {
            db.close();
        }
    }

    /*
     * Creating Tables(non-Javadoc)
     *
     * @see
     * android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite
     * .SQLiteDatabase)
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createCategoryTable = "CREATE TABLE "
                + TABLE_CATEGORY + "("
                + CATEGORY_ID + " LONG PRIMARY KEY, "
                + CATEGORY_COLOR + " INTEGER,"
                + CATEGORY_NAME + " TEXT,"
                + CATEGORY_LAST_UPDATE + " LONG,"
                + CATEGORY_VISIBLE + " INTEGER,"
                + CATEGORY_ORDER + " INTEGER" + ")";
        String createFeedTable = "CREATE TABLE "
                + TABLE_FEED + "("
                + FEED_ID + " LONG PRIMARY KEY, "
                + FEED_CATEGORY_ID + " LONG,"
                + FEED_TITLE + " TEXT,"
                + FEED_DESCRIPTION + " TEXT,"
                + FEED_URL + " TEXT,"
                + FEED_VISIBLE + " INTEGER,"
                + FEED_HTML_URL + " TEXT"+ ")";
        String createEntryTable = "CREATE TABLE "
                + TABLE_ENTRY + "("
                + ENTRY_ID + " LONG PRIMARY KEY, "
                + ENTRY_CATEGORY_ID + " LONG,"
                + ENTRY_FEED_ID + " LONG,"
                + ENTRY_TITLE + " TEXT,"
                + ENTRY_DESCRIPTION + " TEXT,"
                + ENTRY_DATE + " LONG,"
                + ENTRY_SRC_NAME + " TEXT,"
                + ENTRY_URL + " TEXT,"
                + ENTRY_SHORTENED_URL + " TEXT,"
                + ENTRY_IMAGE_URL + " TEXT,"
                + ENTRY_VISIBLE + " INTEGER,"
                + ENTRY_VISITED_DATE + " LONG,"
                + ENTRY_FAVORITE_DATE + " LONG,"
                + ENTRY_IS_EXPANDED + " INTEGER"+ ")";
        db.execSQL(createCategoryTable);
        db.execSQL(createFeedTable);
        db.execSQL(createEntryTable);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP DATABASE " + DATABASE_NAME);
        onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String upgradeQueryVisited = "ALTER TABLE " + TABLE_ENTRY + " ADD COLUMN " + ENTRY_VISITED_DATE + " LONG";
        String upgradeQueryFavorite = "ALTER TABLE " + TABLE_ENTRY + " ADD COLUMN " + ENTRY_FAVORITE_DATE + " LONG";
        String upgradeQueryEntry = "ALTER TABLE " + TABLE_ENTRY + " ADD COLUMN " + ENTRY_IS_EXPANDED + " INTEGER";
        String upgradeQueryFeed = "ALTER TABLE " + TABLE_FEED + " ADD COLUMN " + FEED_HTML_URL + " TEXT";

        if (oldVersion < 35 && newVersion >= 35) {
            db.execSQL(upgradeQueryVisited);
            db.execSQL(upgradeQueryFavorite);
        }

        if (oldVersion < 42 && newVersion >= 42) {
            db.execSQL(upgradeQueryEntry);
            db.execSQL(upgradeQueryFeed);
        }
    }

    public List<Category> getCategories(Boolean excludeFeeds, Boolean excludeEntries, Boolean visible) {
        IPersistableObject<Category> persistence = new PersistableCategories(null, excludeFeeds, excludeEntries, visible);
        return load(persistence);
    }

    @Override
    public Category getCategory(Long categoryId, Boolean excludeFeeds, Boolean excludeEntries) {
        IPersistableObject<Category> persistence = new PersistableCategories(categoryId,excludeFeeds, excludeEntries, null);
        List<Category> result = load(persistence);
        if (result != null && result.size() == 1){
            return result.get(0);
        }
        return null;
    }

    @Override
    public long addCategory(Category category, Boolean excludeFeeds, Boolean excludeEntries) {
        IPersistableObject<Category> persistence = new PersistableCategories(null, excludeFeeds, excludeEntries, null);
        List<Category> result = new ArrayList<Category>();
        result.add(category);
        persistence.store(result);
        return 0;
    }

    @Override
    public int removeCategory(long categoryId, Boolean excludeFeeds, Boolean excludeEntries) {
        IPersistableObject<Category> persistence = new PersistableCategories(categoryId, excludeFeeds, excludeEntries, null);
        persistence.delete();
        return 0;
    }

    @Override
    public List<Feed> getFeeds(Long categoryId, Boolean excludeEntries) {
        IPersistableObject<Feed> persistence = new PersistableFeeds(categoryId, null, excludeEntries, null);
        return load(persistence);
    }

    @Override
    public Feed getFeed(long feedId, Boolean excludeEntries) {
        IPersistableObject<Feed> persistence = new PersistableFeeds(null , feedId, excludeEntries, null);
        List<Feed> result = load(persistence);
        if (result != null && result.size() == 1){
            return result.get(0);
        }
        return null;
    }

    @Override
    public long addFeed(long categoryId, Feed feed, Boolean excludeEntries) {
        IPersistableObject<Feed> persistence = new PersistableFeeds(categoryId , null, excludeEntries, null);
        List<Feed> result = new ArrayList<Feed>();
        result.add(feed);
        persistence.store(result);
        return 0;
    }

    @Override
    public int removeFeeds(Long categoryId, Long feedId, Boolean excludeEntries) {
        IPersistableObject<Feed> persistence = new PersistableFeeds(categoryId , feedId, excludeEntries, null);
        persistence.delete();
        return 0;
    }

    @Override
    public List<Entry> getEntries(Long categoryId, Long feedId) {
        IPersistableObject<Entry> persistence = new PersistableEntries(categoryId, feedId, null, null);
        return load(persistence);
    }

    @Override
    public Entry getEntry(long entryId) {
        IPersistableObject<Entry> persistence = new PersistableEntries(null, null, entryId, null);
        List<Entry> result = load(persistence);
        if (result != null && result.size() == 1){
            return result.get(0);
        }
        return null;
    }

    @Override
    public long addEntry(long categoryId, long feedId, Entry entry) {
        IPersistableObject<Entry> persistence = new PersistableEntries(categoryId, feedId, null, null);
        List<Entry> result = new ArrayList<Entry>();
        result.add(entry);
        persistence.store(result);
        return 0;
    }

    @Override
    public int updateEntry(Entry entry) {
        IPersistableObject<Entry> persistence = new PersistableEntries(entry.getCategoryId(), entry.getFeedId(), entry.getId(), null);
        update(persistence, entry);
        return 0;
    }

    @Override
    public int removeEntries(Long categoryId, Long feedId, Long entryId) {
        IPersistableObject<Entry> persistence = new PersistableEntries(categoryId, feedId, entryId, null);
        persistence.delete();
        return 0;
    }

    @Override
    public List<Entry> getFavoriteEntries(long categoryId) {
        IPersistableObject<Entry> persistence = new PersistableFavoriteEntries(categoryId, null, null, null);
        return load(persistence);
    }

    @Override
    public List<Entry> getVisitedEntries(long categoryId) {
        IPersistableObject<Entry> persistence = new PersistableVisibleEntries(categoryId, null, null, null);
        return load(persistence);
    }

    @Override
    public Cursor getEntriesCursor(Long categoryId, Long feedId) {
        IPersistableObject<Entry> persistence = new PersistableEntries(categoryId, feedId, null, null);
        return persistence.getCursor();
    }

    @Override
    public Cursor getFavoriteEntriesCursor(Long categoryId, Long feedId) {
        IPersistableObject<Entry> persistence = new PersistableFavoriteEntries(categoryId, feedId, null, null);
        return persistence.getCursor();
    }

    @Override
    public Cursor getRecentEntriesCursor(Long categoryId, Long feedId) {
        IPersistableObject<Entry> persistence = new PersistableVisibleEntries(categoryId, feedId, null, null);
        return persistence.getCursor();
    }

    @Override
    public Cursor getUnreadEntriesCursor(Long categoryId, Long feedId) {
        IPersistableObject<Entry> persistence = new PersistableUnreadEntries(categoryId, feedId, null, null);
        return persistence.getCursor();
    }

    @Override
    public Cursor getEntriesCursor(Long categoryId) {
        return getEntriesCursor(categoryId, null);
    }

    @Override
    public Cursor getFavoriteEntriesCursor(Long categoryId) {
        return getFavoriteEntriesCursor(categoryId, null);
    }

    @Override
    public Cursor getRecentEntriesCursor(Long categoryId) {
        return getRecentEntriesCursor(categoryId, null);
    }

    @Override
    public Cursor getUnreadEntriesCursor(Long categoryId) {
        return getUnreadEntriesCursor(categoryId, null);
    }

    @Override
    public void removeAllCategories() {
        IPersistableObject<Category> persistence = new PersistableCategories(null , null, null, null);
        persistence.delete();
    }

    @Override
    public int updateCategory(Category category) {
        IPersistableObject<Category> persistence = new PersistableCategories(category.getId(), null, null, null);
        update(persistence, category);
        return 0;
    }

    @Override
    public int updateFeed(Feed feed) {
        IPersistableObject<Feed> persistence = new PersistableFeeds(feed.getCategoryId(), feed.getId(), null, null);
        update(persistence, feed);
        return 0;
    }

    private <E> void update(final IPersistableObject<E> persistableResource, E resource){
        List<E> result = new ArrayList<E>();
        result.add(resource);
        persistableResource.store(result);
    }

    private <E> List<E> load(final IPersistableObject<E> persistableResource) {
        Cursor cursor = persistableResource.getCursor();
        List<E> cached = new ArrayList<E>();
        try {
            if (!cursor.moveToFirst()) {
                return cached;
            }
            do {
                cached.add(persistableResource.loadFrom(cursor));
            }
            while (cursor.moveToNext());
            return cached;
        } finally {
            cursor.close();
        }
    }

    public void loadXml() {
        try {
            News news = new XmlParser(mContext).readDefaultNewsFile();
            for (Category category : news.getCategories()) {
                if (category != null) {
                    addCategory(category, false, false);
                }
            }
            PrefUtilities.getInstance().saveLoading(true);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
}
