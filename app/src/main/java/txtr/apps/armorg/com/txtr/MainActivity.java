package txtr.apps.armorg.com.txtr;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<Card> cards;
    private List<Contact> contacts;
    private RelativeLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar bar = (Toolbar) findViewById(R.id.toolbar);
        bar.setTitle("Txtr");
        setSupportActionBar(bar);

        RecyclerView cardsRv = (RecyclerView) findViewById(R.id.cards_rv);
        RecyclerView contactsRv = (RecyclerView) findViewById(R.id.contacts_rv);

        cardsRv.setHasFixedSize(true);
        contactsRv.setHasFixedSize(true);

        layout = (RelativeLayout) findViewById(R.id.root);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        cardsRv.setLayoutManager(llm);
        LinearLayoutManager llm2 = new LinearLayoutManager(this);
        contactsRv.setLayoutManager(llm2);

        initializeCards();

        RVCardsAdapter adapter = new RVCardsAdapter(cards);
        cardsRv.setAdapter(adapter);

        RVContactsAdapter contactsAdapter = new RVContactsAdapter(contacts);
        contactsRv.setAdapter(contactsAdapter);

        TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec("cards");
        tabSpec.setContent(R.id.cards_tab);
        tabSpec.setIndicator("Cards");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("contacts");
        tabSpec.setContent(R.id.contacts_tab);
        tabSpec.setIndicator("Contacts");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("phrases");
        tabSpec.setContent(R.id.phrases_tab);
        tabSpec.setIndicator("Phrases");
        tabHost.addTab(tabSpec);
    }

    public void initializeCards() {
        cards = new ArrayList<>();
        cards.add(new Card("When are you picking me up?", "510-367-2406"));
        cards.add(new Card("I'm done!", "602-563-9240"));

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
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
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

    public void sendMessage(String msg, String contactNum) {

        //send message here

        final Snackbar snackBar = Snackbar.make(layout, "Message sent", Snackbar.LENGTH_LONG);
        snackBar.setAction("DISMISS", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackBar.dismiss();
            }
        });
        snackBar.setActionTextColor(Color.RED);
        snackBar.show();
    }

    class Card {
        String message;
        String contactNum;

        Card(String message, String contactNum) {
            this.message = message;
            this.contactNum = contactNum;
        }
    }

    class Contact {
        String contactName, contactNum;
        Bitmap image;

        Contact(String name, String num, Bitmap image) {
            contactName = name;
            contactNum = num;
            this.image = image;
        }
    }

    public class RVCardsAdapter extends RecyclerView.Adapter<RVCardsAdapter.CardViewHolder> {
        public class CardViewHolder extends RecyclerView.ViewHolder {
            CardView cv;
            TextView messageTv, contactTv;

            public CardViewHolder(View itemView) {
                super(itemView);
                cv = (CardView) itemView.findViewById(R.id.cv);
                messageTv = (TextView) itemView.findViewById(R.id.message_tv);
                contactTv = (TextView) itemView.findViewById(R.id.contact_tv);
                cv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendMessage(messageTv.getText().toString(), contactTv.getText().toString());
                    }
                });
            }
        }

        List<Card> cards;
        public RVCardsAdapter(List<Card> cards) {
            this.cards = cards;
        }

        public int getItemCount() {
            return cards.size();
        }

        public CardViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item, viewGroup, false);
            return new CardViewHolder(v);
        }

        public void onBindViewHolder(CardViewHolder cvh, int i) {
            cvh.messageTv.setText(cards.get(i).message);
            cvh.contactTv.setText(cards.get(i).contactNum);
        }

        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }
    }

    public class RVContactsAdapter extends RecyclerView.Adapter<RVContactsAdapter.CardViewHolder> {
        public class CardViewHolder extends RecyclerView.ViewHolder {
            CardView cv;
            TextView contactNameTv, contactNumTv;
            ImageView contactIv;

            public CardViewHolder(View itemView) {
                super(itemView);
                cv = (CardView) itemView.findViewById(R.id.cv);
                contactNameTv = (TextView) itemView.findViewById(R.id.contact_name_tv);
                contactNumTv = (TextView) itemView.findViewById(R.id.contact_number_tv);
                contactIv = (ImageView) itemView.findViewById(R.id.contact_iv);
                cv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });
            }
        }

        List<Contact> contacts;
        public RVContactsAdapter(List<Contact> contacts) {
            this.contacts = contacts;
        }

        public int getItemCount() {
            return contacts.size();
        }

        public CardViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.contact_item, viewGroup, false);
            return new CardViewHolder(v);
        }

        public void onBindViewHolder(CardViewHolder cvh, int i) {
            cvh.contactNameTv.setText(contacts.get(i).contactName);
            cvh.contactNumTv.setText(contacts.get(i).contactNum);
            cvh.contactIv.setImageBitmap(contacts.get(i).image);
        }

        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }
    }
}
