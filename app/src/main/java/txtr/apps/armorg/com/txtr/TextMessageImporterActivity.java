package txtr.apps.armorg.com.txtr;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.StaleDataException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TextMessageImporterActivity extends AppCompatActivity {

    private ArrayList<String> cardArray, cardNumArray;
    private ArrayList<Card> cards;
    private RVCardsAdapter adapter;
    private RelativeLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_message_importer);
        Toolbar bar = (Toolbar) findViewById(R.id.toolbar);
        bar.setTitle(getResources().getString(R.string.cardImporter));
        setSupportActionBar(bar);

        layout = (RelativeLayout) findViewById(R.id.root);

        Map<String, Integer> map = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);
        List<Sms> sms = getAllSms();
        for (Sms msgs : sms) {
            String message = msgs.getMsg().replaceAll("[?!.]", "");
            if (message.equals("")) {
                continue;
            }
            String num = msgs.getAddress();
            if(num.equals("") || num.length() < 7)
                continue;
            if(num.length() > 10)
                num = num.substring(num.length() - 10);
            String entry = num + "&" + message;
            try {
                int duplicates = map.get(entry)+1;
                map.put(entry, duplicates);
//                Log.e("txtr-app", "\n\n duplicate: " + entry+"\n\n");
            } catch (Exception e) {
                map.put(entry, 1);
//                Log.e("txtr-app", "single msg :\t" + entry);
            }
        }
//        Log.e("txtr-app", "\n\n\n\n");

        map = new LinkedHashMap<>(sortByComparator(map, false));
//        Log.e("txtr-app", map.toString());
        ArrayList<String> topSms = new ArrayList(map.keySet());

        RecyclerView cardsRv = (RecyclerView) findViewById(R.id.cards_rv);
        cardsRv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        cardsRv.setLayoutManager(llm);

        cards = new ArrayList<>();
        int extractingSMS;
        if(map.size() < 10)
            extractingSMS = map.size();
        else
            extractingSMS = 10;

        for (int i = 0; i < extractingSMS; i++) {
            String entry = topSms.get(i);
            System.out.println(entry);
            String[] str = entry.split("&");
            String contactNum = str[0];
            String message = str[1];
            String contactName = getContactName(this, contactNum);
            cards.add(new Card(message, contactName));
        }

        adapter = new RVCardsAdapter(cards);
        cardsRv.setAdapter(adapter);

        cardArray = SharedPrefsHandler.loadStringArray("card_msg_list", this);
        cardNumArray = SharedPrefsHandler.loadStringArray("card_num_list", this);

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
                                    onDismissedBySwipeRight(recyclerView, reverseSortedPositions);
                                }
                            }

                            @Override
                            public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    cardArray.add(cards.get(position).message);
                                    cardNumArray.add(cards.get(position).contactName);
                                    SharedPrefsHandler.saveStringArray(cardArray, "card_msg_list", TextMessageImporterActivity.this);
                                    SharedPrefsHandler.saveStringArray(cardNumArray, "card_num_list", TextMessageImporterActivity.this);
                                    Snackbar.make(layout, "Card added", Snackbar.LENGTH_SHORT).show();
                                }
                            }
                        });

        cardsRv.addOnItemTouchListener(cardSwipeListener);
    }

    public String getContactName(Context context, String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor == null) {
            return null;
        }
        String contactName = null;
        if(cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }

        if(cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return contactName;
    }

    private static Map<String, Integer> sortByComparator(Map<String, Integer> unsortMap, final boolean order) {

        List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2) {
                if (order) {
                    return o1.getValue().compareTo(o2.getValue());
                } else {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.importer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.done:
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onPause() {
        super.onPause();
        finish();
    }

    public List<Sms> getAllSms() {
        List<Sms> lstSms = new ArrayList<Sms>();
        Sms objSms = new Sms();
        Uri message = Uri.parse("content://sms/sent");
        ContentResolver cr = this.getContentResolver();

        Cursor c = cr.query(message, null, null, null, null);
        startManagingCursor(c);
        int totalSMS = 200;
        if(c.getCount() < totalSMS)
            totalSMS = c.getCount();

        if (c.moveToFirst()) {
            for (int i = 0; i < totalSMS; i++) {

                objSms = new Sms();
                objSms.setId(c.getString(c.getColumnIndexOrThrow("_id")));
                objSms.setAddress(c.getString(c
                        .getColumnIndexOrThrow("address")));
                objSms.setMsg(c.getString(c.getColumnIndexOrThrow("body")));
                objSms.setReadState(c.getString(c.getColumnIndex("read")));
                objSms.setTime(c.getString(c.getColumnIndexOrThrow("date")));
                if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                    objSms.setFolderName("inbox");
                } else {
                    objSms.setFolderName("sent");
                }

                lstSms.add(objSms);
                c.moveToNext();
            }
        }
        // else {
        // throw new RuntimeException("You have no SMS");
        // }
        c.close();

        return lstSms;
    }
}
