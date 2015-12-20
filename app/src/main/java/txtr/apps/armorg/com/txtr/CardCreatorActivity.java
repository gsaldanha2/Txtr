package txtr.apps.armorg.com.txtr;

import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CardCreatorActivity extends AppCompatActivity {

    public static String phrase, contactName;
    public static FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_creator);

        fm = getFragmentManager();
        fm.beginTransaction().replace(R.id.frame_root, new ContactsFragment()).commit();
    }

    public static void submitContact() {
        fm.beginTransaction().replace(R.id.frame_root, new PhrasesFragment()).commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(CardCreatorActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
