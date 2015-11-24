package txtr.apps.armorg.com.txtr;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

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
