package txtr.apps.armorg.com.txtr;

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

    private List<Contact> contacts;
    public static String phrase, contactNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_creator);

        initializeCards();

        final RecyclerView contactsRv = (RecyclerView) findViewById(R.id.contacts_rv);
        contactsRv.setHasFixedSize(true);
        LinearLayoutManager llm2= new LinearLayoutManager(this);
        contactsRv.setLayoutManager(llm2);



        RVContactsAdapter adapter1 = new RVContactsAdapter(contacts);
        contactsRv.setAdapter(adapter1);


        contactsRv.addOnItemTouchListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                contactNum = contacts.get(position).contactNum;
            }
        }));

        Button addBtn = (Button) findViewById(R.id.create_btn);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(phrase != null && contactNum != null) {
                    ArrayList<String> cardMsgs = SharedPrefsHandler.loadStringArray("card_msg_list", CardCreatorActivity.this);
                    ArrayList<String> cardNums = SharedPrefsHandler.loadStringArray("card_num_list", CardCreatorActivity.this);
                    cardMsgs.add(phrase);
                    cardNums.add(contactNum);
                    SharedPrefsHandler.saveStringArray(cardMsgs, "card_msg_list", CardCreatorActivity.this);
                    SharedPrefsHandler.saveStringArray(cardNums, "card_num_list", CardCreatorActivity.this);
                    Intent intent = new Intent(CardCreatorActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(CardCreatorActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void initializeCards() {


        contacts = new ArrayList<>();
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (Integer.parseInt(cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        Bitmap bit = openPhoto(pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)));
                        contacts.add(new Contact(name, phoneNo, bit));
                    }
                    pCur.close();
                }
            }
        }
        cur.close();
    }

    public Bitmap openPhoto(String contactId) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(contactId));
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = getContentResolver().query(photoUri,
                new String[]{ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
        if (cursor == null) {
            return null;
        }
        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(0);
                if (data != null) {
                    return new BitmapFactory().decodeStream(new ByteArrayInputStream(data));
                }
            }
        } finally {
            cursor.close();
        }
        return null;
    }
}
