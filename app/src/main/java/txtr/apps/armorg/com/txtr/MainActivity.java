package txtr.apps.armorg.com.txtr;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
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
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<Card> cards;
    private List<Contact> contacts;
    private RelativeLayout layout;
    private Card undoCard;

    private BroadcastReceiver sendBroadcastReceiver;

    private String SENT = "SMS_SENT", currentAddedName = "", currentAddedNumber = "", currentAddedPhrase = "";
    private Bitmap curredAddedBitmap = null;

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
        registerMessagingReceivers();

        final RVCardsAdapter adapter = new RVCardsAdapter(cards);
        cardsRv.setAdapter(adapter);

        RVContactsAdapter contactsAdapter = new RVContactsAdapter(contacts);
        contactsRv.setAdapter(contactsAdapter);

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
                                    adapter.notifyItemRemoved(position);

                                    final Snackbar snackBar = Snackbar.make(layout, "Card removed", Snackbar.LENGTH_LONG);
                                    snackBar.setAction("UNDO", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            cards.add(undoCard);
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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setTitle("Create Card");
                alert.setMessage("Select Contact and Phrase");
                LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.alert_layout, null);
                alert.setView(dialogView);

                View contactView = dialogView.findViewById(R.id.contact_view);
                TextView selectedContactName = (TextView) contactView.findViewById(R.id.contact_name_tv);
                selectedContactName.setText("Click here to select a contact");
                TextView selectedContactNumber = (TextView) contactView.findViewById(R.id.contact_number_tv);
                selectedContactNumber.setText(currentAddedNumber);
                ImageView selectedContactImage = (ImageView) contactView.findViewById(R.id.contact_iv);
                selectedContactImage.setImageResource(R.drawable.profile);

                View phraseView = dialogView.findViewById(R.id.phrase_view);
                TextView selectedPhrase = (TextView) phraseView.findViewById(R.id.phrase_tv);
                selectedPhrase.setText("Click here to select a phrase");
                alert.create();
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
        cards = new ArrayList<>();
        cards.add(new Card("Testing app...", "510-367-2406"));

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
        unregisterReceiver(sendBroadcastReceiver);
        super.onStop();
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

        public class CardViewHolder extends RecyclerView.ViewHolder {
            CardView cv;
            TextView messageTv, contactTv;

            public CardViewHolder(View itemView) {
                super(itemView);
                cv = (CardView) itemView.findViewById(R.id.cv);
                messageTv = (TextView) itemView.findViewById(R.id.phrase_tv);
                contactTv = (TextView) itemView.findViewById(R.id.contact_tv);
            }
        }
    }

    public class RVContactsAdapter extends RecyclerView.Adapter<RVContactsAdapter.CardViewHolder> {
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
            Bitmap bitmap = contacts.get(i).image;
            if (bitmap != null)
                cvh.contactIv.setImageBitmap(contacts.get(i).image);
            else
                cvh.contactIv.setImageResource(R.drawable.profile);
        }

        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }

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
            }
        }
    }
}
