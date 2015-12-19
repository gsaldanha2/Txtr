package txtr.apps.armorg.com.txtr;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<Card> cards;
    private List<Contact> contacts;
    private List<String> phrases;
    private ArrayList<String> cardArray, cardNumArray;
    private RelativeLayout layout;
    private Card undoCard;
    private String undoPhrase;
    private HashMap<String, Integer> spamMap; //stores recent numbers that were sent to, to prevent spam
    private TextView noCardsTv, noPhrasesTv;
    private BroadcastReceiver sendBroadcastReceiver;
    private final String TAPPX_KEY = "/120940746/Pub-7193-Android-5856";
    private com.google.android.gms.ads.doubleclick.PublisherInterstitialAd adInterstitial = null;
    private String SENT = "SMS_SENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar bar = (Toolbar) findViewById(R.id.toolbar);
        bar.setTitle("Txtr");
        setSupportActionBar(bar);

        if(SharedPrefsHandler.loadBoolean("firstTime", this)) {
            Intent intent = new Intent(MainActivity.this, WelcomeScreenActivity.class);
            startActivity(intent);
        }

        adInterstitial = com.tappx.TAPPXAdInterstitial.Configure(this, TAPPX_KEY,
                new AdListener() {
                    @Override public void onAdLoaded() {
                        com.tappx.TAPPXAdInterstitial.Show(adInterstitial);
                    }
                });

        final RecyclerView cardsRv = (RecyclerView) findViewById(R.id.cards_rv);
        final RecyclerView phrasesRv = (RecyclerView) findViewById(R.id.phrases_rv);

        cardsRv.setHasFixedSize(true);
        phrasesRv.setHasFixedSize(true);

        layout = (RelativeLayout) findViewById(R.id.root);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        cardsRv.setLayoutManager(llm);
        LinearLayoutManager llm3 = new LinearLayoutManager(this);
        phrasesRv.setLayoutManager(llm3);

        initializeCards();

        noCardsTv = (TextView) findViewById(R.id.no_cards_tv);
        noPhrasesTv = (TextView) findViewById(R.id.no_phrases_tv);

        refreshEmptyRvMsg();
        registerMessagingReceivers();

        final RVCardsAdapter adapter = new RVCardsAdapter(cards);
        cardsRv.setAdapter(adapter);

        final RVPhraseAdapter phraseAdapter = new RVPhraseAdapter(phrases);
        phrasesRv.setAdapter(phraseAdapter);

        SwipeableRecyclerViewTouchListener phraseSwipeListener =
                new SwipeableRecyclerViewTouchListener(phrasesRv,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {
                            @Override
                            public boolean canSwipe(int position) {
                                return true;
                            }

                            @Override
                            public void onDismissedBySwipeLeft(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (final int position : reverseSortedPositions) {
                                    undoPhrase = phrases.get(position);
                                    phrases.remove(position);
                                    SharedPrefsHandler.saveStringArray(phrases, "phrase_list", MainActivity.this);
                                    phraseAdapter.notifyItemRemoved(position);

                                    final Snackbar snackBar = Snackbar.make(layout, getResources().getString(R.string.phraseRemoved), Snackbar.LENGTH_LONG);
                                    snackBar.setAction(getResources().getString(R.string.undo), new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            phrases.add(undoPhrase);
                                            SharedPrefsHandler.saveStringArray(phrases, "phrase_list", MainActivity.this);
                                            phraseAdapter.notifyItemInserted(phrases.indexOf(undoPhrase));
                                            snackBar.dismiss();
                                            Snackbar.make(layout, getResources().getString(R.string.actionUndone), Snackbar.LENGTH_SHORT).show();
                                            refreshEmptyRvMsg();
                                        }
                                    });
                                    snackBar.setActionTextColor(Color.RED);
                                    snackBar.show();
                                    refreshEmptyRvMsg();
                                }
                                phraseAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                onDismissedBySwipeLeft(recyclerView, reverseSortedPositions);
                            }
                        });

        SwipeableRecyclerViewTouchListener cardSwipeListener =
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

                                    final Snackbar snackBar = Snackbar.make(layout, getResources().getString(R.string.cardRemoved), Snackbar.LENGTH_LONG);
                                    snackBar.setAction(getResources().getString(R.string.undo), new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            cards.add(undoCard);
                                            cardArray.add(undoCard.message);
                                            cardNumArray.add(undoCard.contactNum);
                                            SharedPrefsHandler.saveStringArray(cardArray, "card_msg_list", MainActivity.this);
                                            SharedPrefsHandler.saveStringArray(cardNumArray, "card_num_list", MainActivity.this);
                                            adapter.notifyItemInserted(cards.indexOf(undoCard));
                                            snackBar.dismiss();
                                            Snackbar.make(layout, getResources().getString(R.string.actionUndone), Snackbar.LENGTH_SHORT).show();
                                            refreshEmptyRvMsg();
                                        }
                                    });
                                    snackBar.setActionTextColor(Color.RED);
                                    snackBar.show();
                                    refreshEmptyRvMsg();
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

        cardsRv.addOnItemTouchListener(cardSwipeListener);
        phrasesRv.addOnItemTouchListener(phraseSwipeListener);

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
                alert.setTitle(getResources().getString(R.string.addPhrase));
                alert.setMessage(getResources().getString(R.string.enterPhrase));
                final EditText phraseEt = new EditText(MainActivity.this);
                alert.setView(phraseEt);
                alert.setPositiveButton(getResources().getString(R.string.add), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newPhrase = phraseEt.getText().toString();
                        phrases.add(newPhrase);
                        SharedPrefsHandler.saveStringArray(phrases, "phrase_list", MainActivity.this);
                        phraseAdapter.notifyItemInserted(phrases.indexOf(newPhrase));
                        phraseAdapter.notifyDataSetChanged();
                        refreshEmptyRvMsg();
                    }
                });
                alert.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
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
        tabSpec.setIndicator(getResources().getString(R.string.cards));
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("phrases");
        tabSpec.setContent(R.id.phrases_tab);
        tabSpec.setIndicator(getResources().getString(R.string.phrases));
        tabHost.addTab(tabSpec);

        TabWidget widget = tabHost.getTabWidget();
        for(int i = 0; i < widget.getChildCount(); i++) {
            View v = widget.getChildAt(i);

            TextView tv = (TextView)v.findViewById(android.R.id.title);
            if(tv == null) {
                continue;
            }
            v.setBackgroundResource(R.drawable.custom_tab_selector);
        }

        for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
            TextView tv = (TextView) tabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
            tv.setTextColor(Color.parseColor("#ffffff"));
        }
        //</editor-fold>
    }

    public void refreshEmptyRvMsg() {
        if(cards.size()==0)
            noCardsTv.setVisibility(View.VISIBLE);
        else
            noCardsTv.setVisibility(View.GONE);

        if(phrases.size() == 0)
            noPhrasesTv.setVisibility(View.VISIBLE);
        else
            noPhrasesTv.setVisibility(View.GONE);
    }

    public void initializeCards() {
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

        spamMap = new HashMap<String, Integer>();
        for(String num : cardNumArray) {
            spamMap.put(num, 0);
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


    private void registerMessagingReceivers() {
        //---when the SMS has been sent---
        sendBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Snackbar.make(layout, getResources().getString(R.string.messageSent), Snackbar.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Snackbar.make(layout, getResources().getString(R.string.failedSend), Snackbar.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Snackbar.make(layout, getResources().getString(R.string.noService), Snackbar.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Snackbar.make(layout, getResources().getString(R.string.nullErrorSend), Snackbar.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Snackbar.make(layout, getResources().getString(R.string.radioOff), Snackbar.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        registerReceiver(sendBroadcastReceiver, new IntentFilter(SENT));
    }

    public void sendMessage(String msg, String contactNum) {
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);
        if(spamMap.get(contactNum) < 9) {
            int i = contactNum.indexOf('[');
            String contactNumC = contactNum.substring(0, i);
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(contactNumC, null, msg, sentPI, null);
            spamMap.put(contactNum, spamMap.get(contactNum) + 1);

            adInterstitial.show();

            int sessions = SharedPrefsHandler.loadInt("sessions", this);
            boolean notRated = SharedPrefsHandler.loadBoolean("notRated", this);
            sessions++;
            SharedPrefsHandler.saveInt("sessions", sessions, this);
            if(sessions % 5 == 0 && notRated == true && sessions != 0) {
                AlertDialog.Builder balert = new AlertDialog.Builder(this);
                balert.setTitle("Rate Txtr");
                balert.setMessage("Tell us how you like Txtr!");
                balert.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = Uri.parse("market://details?id=" + MainActivity.this.getPackageName());
                        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        SharedPrefsHandler.saveBoolean("notRated", false, MainActivity.this);
                        try {
                            startActivity(goToMarket);
                        } catch (ActivityNotFoundException e) {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("http://play.google.com/store/apps/details?id=" + MainActivity.this.getPackageName())));
                        }
                    }
                });
                balert.setNegativeButton("Later", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                balert.show();
            }
        }else {
            Snackbar.make(layout, getResources().getString(R.string.spam), Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        try {
            unregisterReceiver(sendBroadcastReceiver);
        }catch(Exception e) {}
        super.onStop();
    }
}
