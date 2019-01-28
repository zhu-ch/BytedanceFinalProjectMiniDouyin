package myself.zch.minidouyin;

import android.content.ContentValues;
import android.content.Entity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import myself.zch.minidouyin.Database.FavoriteContract;
import myself.zch.minidouyin.Database.FavoriteDbHelper;
import myself.zch.minidouyin.JavaBeans.Entry;

public class ShowFavoriteActivity extends AppCompatActivity {
    private static final String TAG = "DEBUG_FAVORITE_ACTIVITY";

    private FavoriteDbHelper mDbHelper;
    private SQLiteDatabase db;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter entryAdapter;
    private List<Entry> mEntry;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_favorite);
        mDbHelper = new FavoriteDbHelper(this);
        db = mDbHelper.getWritableDatabase();
        initRecyclerView();
    }

    private void initRecyclerView() {
        entryAdapter = new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                ImageView imageView = new ImageView(viewGroup.getContext());
                imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                imageView.setAdjustViewBounds(true);
                return new ShowFavoriteActivity.MyViewHolder(imageView);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                ImageView iv = (ImageView) viewHolder.itemView;

                //Glide加载图片
                String url = mEntry.get(i).getImageUrl();
                Glide.with(iv.getContext()).load(url).into(iv);

                //单击进入详情页面播放
                viewHolder.itemView.setOnClickListener((v -> {
                    Intent intent = new Intent(ShowFavoriteActivity.this, PlayActivity.class);
                    intent.putExtra("VIDEO_URL", mEntry.get(i).getVideoUrl());
                    intent.putExtra("IMAGE_URL", mEntry.get(i).getImageUrl());
                    intent.putExtra("STUDENT_ID", mEntry.get(i).getStudentId());
                    intent.putExtra("USER_NAME", mEntry.get(i).getUserName());

                    startActivity(intent);
                }));

                //长按收藏
                viewHolder.itemView.setOnLongClickListener(v -> {
                    if (db == null) {
                        Log.d(TAG, "onBindViewHolder: db=null");
                        return false;
                    }

                    int rows = db.delete(FavoriteContract.FavEntry.TABLE_NAME,
                            FavoriteContract.FavEntry._ID + " =?",
                            new String[]{String.valueOf(mEntry.get(i).id)});
                    if (rows > 0) {
                        entryAdapter.notifyDataSetChanged();
                        return true;
                    }
                    return false;
                });
            }

            @Override
            public int getItemCount() {
                return mEntry.size();
            }
        };
        mEntry = loadEntiresFromDatabase();
        recyclerView = findViewById(R.id.rv_favorite);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(entryAdapter);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    //从数据库中查询数据，并转换成 JavaBeans
    private List<Entry> loadEntiresFromDatabase() {
        if (db == null) {
            return Collections.emptyList();
        }

        List<Entry> result = new LinkedList<>();
        Cursor cursor = null;
        try {
            cursor = db.query(FavoriteContract.FavEntry.TABLE_NAME,
                    new String[]{FavoriteContract.FavEntry._ID,
                            FavoriteContract.FavEntry.COLUMN_DATE,
                            FavoriteContract.FavEntry.COLUMN_STUDENT_ID,
                            FavoriteContract.FavEntry.COLUMN_USER_NAME,
                            FavoriteContract.FavEntry.COLUMN_IMAGE_URL,
                            FavoriteContract.FavEntry.COLUMN__VIDEO_URL},
                    null, null, null, null,
                    FavoriteContract.FavEntry.COLUMN_DATE + " ASC");

            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(FavoriteContract.FavEntry._ID));
                long dateInMs = cursor.getLong(cursor.getColumnIndex(FavoriteContract.FavEntry.COLUMN_DATE));
                String stuId = cursor.getString(cursor.getColumnIndex(FavoriteContract.FavEntry.COLUMN_STUDENT_ID));
                String userName = cursor.getString(cursor.getColumnIndex(FavoriteContract.FavEntry.COLUMN_USER_NAME));
                String vidUrl = cursor.getString(cursor.getColumnIndex(FavoriteContract.FavEntry.COLUMN__VIDEO_URL));
                String imgUrl = cursor.getString(cursor.getColumnIndex(FavoriteContract.FavEntry.COLUMN_IMAGE_URL));

                Entry entry = new Entry(id);
                entry.setDate(new Date(dateInMs));
                entry.setImageUrl(imgUrl);
                entry.setStudentId(stuId);
                entry.setUserName(userName);
                entry.setVideoUrl(vidUrl);

                result.add(entry);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }
}
