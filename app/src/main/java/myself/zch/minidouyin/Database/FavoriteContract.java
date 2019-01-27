package myself.zch.minidouyin.Database;

import android.provider.BaseColumns;

public class FavoriteContract {
    private FavoriteContract() {
    }

    public static class FavEntry implements BaseColumns {
        public static final String TABLE_NAME = "fav";
        public static final String COLUMN_STUDENT_ID = "stu_id";
        public static final String COLUMN_USER_NAME = "user_name";
        public static final String COLUMN_IMAGE_URL = "img_url";
        public static final String COLUMN__VIDEO_URL = "vid_url";
        public static final String COLUMN_DATE = "date";
    }

    //建表
    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + FavEntry.TABLE_NAME +
                    "(" +
                    FavEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    FavEntry.COLUMN_STUDENT_ID + " TEXT," +
                    FavEntry.COLUMN_USER_NAME + " TEXT," +
                    FavEntry.COLUMN_IMAGE_URL + " TEXT," +
                    FavEntry.COLUMN__VIDEO_URL + " TEXT," +
                    FavEntry.COLUMN_DATE + " TEXT" +
                    ")";

    //删表
    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + FavEntry.TABLE_NAME;
}
