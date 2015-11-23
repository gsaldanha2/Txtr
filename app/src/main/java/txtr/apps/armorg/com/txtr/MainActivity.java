package txtr.apps.armorg.com.txtr;

import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<Card> cards;
    private RelativeLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView rv = (RecyclerView)findViewById(R.id.rv);
        rv.setHasFixedSize(true);

        layout = (RelativeLayout) findViewById(R.id.root);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);

        initializeCards();

        RVAdapter adapter = new RVAdapter(cards);
        rv.setAdapter(adapter);;
    }

    public void initializeCards() {
        cards = new ArrayList<>();
        cards.add(new Card("When are you picking me up?", "510-367-2406"));
        cards.add(new Card("I'm done!", "602-563-9240"));
    }

    public void sendMessage(String msg, String contactNum) {
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

    public class RVAdapter extends RecyclerView.Adapter<RVAdapter.CardViewHolder> {
        public class CardViewHolder extends RecyclerView.ViewHolder {
            CardView cv;
            TextView messageTv, contactTv;

            CardViewHolder(View itemView) {
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
        RVAdapter(List<Card> cards) {
            this.cards = cards;
        }

        public int getItemCount() {
            return cards.size();
        }

        public CardViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item, viewGroup, false);
            CardViewHolder cvh = new CardViewHolder(v);
            return cvh;
        }

        public void onBindViewHolder(CardViewHolder cvh, int i) {
            cvh.messageTv.setText(cards.get(i).message);
            cvh.contactTv.setText(cards.get(i).contactNum);
        }

        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }
    }
}
