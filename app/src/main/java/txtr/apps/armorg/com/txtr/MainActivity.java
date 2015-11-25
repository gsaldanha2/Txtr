package txtr.apps.armorg.com.txtr;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private List<Card> cards;
    private List<Contact> contacts;
    private List<String> phrases;
    private ArrayList<String> cardArray, cardNumArray;
    private RelativeLayout layout;
    private Card undoCard;

    private BroadcastReceiver sendBroadcastReceiver;

    private String SENT = "SMS_SENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar bar = (Toolbar) findViewById(R.id.toolbar);
        bar.setTitle("Txtr");
        setSupportActionBar(bar);

        final RecyclerView cardsRv = (RecyclerView) findViewById(R.id.cards_rv);
        final RecyclerView contactsRv = (RecyclerView) findViewById(R.id.contacts_rv);
        final RecyclerView phrasesRv = (RecyclerView) findViewById(R.id.phrases_rv);

        cardsRv.setHasFixedSize(true);
        contactsRv.setHasFixedSize(true);
        phrasesRv.setHasFixedSize(true);

        layout = (RelativeLayout) findViewById(R.id.root);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        cardsRv.setLayoutManager(llm);
        LinearLayoutManager llm2 = new LinearLayoutManager(this);
        contactsRv.setLayoutManager(llm2);
        LinearLayoutManager llm3 = new LinearLayoutManager(this);
        phrasesRv.setLayoutManager(llm3);

        initializeCards();
        registerMessagingReceivers();

        final RVCardsAdapter adapter = new RVCardsAdapter(cards);
        cardsRv.setAdapter(adapter);

        final RVContactsAdapter contactsAdapter = new RVContactsAdapter(contacts);
        contactsRv.setAdapter(contactsAdapter);

        final RVPhraseAdapter phraseAdapter = new RVPhraseAdapter(phrases);
        phrasesRv.setAdapter(phraseAdapter);

        SwipeableRecyclerViewTouchListener swipeTouchListener =
                new SwipeableRecyclerViewTouchListener(cardsRv,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {
                            @Override
                            public boolean canSwipe(int position) {
                                return true;
                            }

                            @Override
                            public void onDismissedBySwipeLeft(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (final int position : reverseSortedPositions) {
                                    undoCard = cards.get(position);
                                    cards.remove(position);
                                    cardArray.remove(position);
                                    cardNumArray.remove(position);
                                    SharedPrefsHandler.saveStringArray(cardArray, "card_msg_list", MainActivity.this);
                                    SharedPrefsHandler.saveStringArray(cardNumArray, "card_num_list", MainActivity.this);
                                    adapter.notifyItemRemoved(position);

                                    final Snackbar snackBar = Snackbar.make(layout, "Card removed", Snackbar.LENGTH_LONG);
                                    snackBar.setAction("UNDO", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            cards.add(undoCard);
                                            cardArray.add(undoCard.message);
                                            cardNumArray.add(undoCard.contactNum);
                                            SharedPrefsHandler.saveStringArray(cardArray, "card_msg_list", MainActivity.this);
                                            SharedPrefsHandler.saveStringArray(cardNumArray, "card_num_list", MainActivity.this);
                                            adapter.notifyItemInserted(cards.indexOf(undoCard));
                                            snackBar.dismiss();
                                            Snackbar.make(layout, "Action undone", Snackbar.LENGTH_SHORT).show();
                                        }
                                    });
                                    snackBar.setActionTextColor(Color.RED);
                                    snackBar.show();
                                }
                                adapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    sendMessage(cards.get(position).message, cards.get(position).contactNum);
                                }
                            }
                        });

        cardsRv.addOnItemTouchListener(swipeTouchListener);

        FloatingActionButton cardFab = (FloatingActionButton) findViewById(R.id.cards_fab);
        cardFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CardCreatorActivity.class);
                startActivity(intent);
                finish();
            }
        });

        FloatingActionButton phraseFab = (FloatingActionButton) findViewById(R.id.phrases_fab);
        phraseFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setTitle("Add Phrase");
                alert.setMessage("Enter phrase:");
                final EditText phraseEt = new EditText(MainActivity.this);
                alert.setView(phraseEt);
                alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newPhrase = phraseEt.getText().toString();
                        phrases.add(newPhrase);
                        SharedPrefsHandler.saveStringArray(phrases, "phrase_list", MainActivity.this);
                        phraseAdapter.notifyItemInserted(phrases.indexOf(newPhrase));
                        phraseAdapter.notifyDataSetChanged();
                    }
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                alert.show();
            }
        });
        //<editor-fold desc="Tab Setup">
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

        for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
            TextView tv = (TextView) tabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
            tv.setTextColor(Color.parseColor("#ffffff"));
        }
        //</editor-fold>
    }

    public void initializeCards() {
        phrases = new ArrayList<String>();
        phrases.add("I'm done");
        SharedPrefsHandler.saveStringArray(phrases, "phrase_list", this);

        cards = new ArrayList<>();
        cardArray = SharedPrefsHandler.loadStringArray("card_msg_list", this);
        cardNumArray = SharedPrefsHandler.loadStringArray("card_num_list", this);
        for(int i = 0; i < cardArray.size(); i ++) {
            cards.add(new Card(cardArray.get(i), cardNumArray.get(i)));
        }

        phrases = SharedPrefsHandler.loadStringArray("phrase_list", this);


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


    private void registerMessagingReceivers() {
        //---when the SMS has been sent---
        sendBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Snackbar.make(layout, "Message Sent", Snackbar.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Snackbar.make(layout, "Failed to send", Snackbar.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Snackbar.make(layout, "No service - can't send", Snackbar.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Snackbar.make(layout, "Null error - can't send", Snackbar.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Snackbar.make(layout, "Radio is off - can't send", Snackbar.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        registerReceiver(sendBroadcastReceiver, new IntentFilter(SENT));
    }

    public void sendMessage(String msg, String contactNum) {
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(contactNum, null, msg, sentPI, null);
    }

    @Override
    protected void onStop() {
        try {
            unregisterReceiver(sendBroadcastReceiver);
        }catch(Exception e) {}
        super.onStop();
    }
}
