package txtr.apps.armorg.com.txtr;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class RVPhraseAdapter extends RecyclerView.Adapter<RVPhraseAdapter.PhraseViewHolder> {
        List<String> phrases;

        public RVPhraseAdapter(List<String> phrases) {
            this.phrases = phrases;
        }

        public int getItemCount() {
            return phrases.size();
        }

        public PhraseViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.phrase_item, viewGroup, false);
            return new PhraseViewHolder(v);
        }

        public void onBindViewHolder(PhraseViewHolder cvh, int i) {
            cvh.phraseTv.setText(phrases.get(i));
        }

        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }

        public class PhraseViewHolder extends RecyclerView.ViewHolder {
            CardView cv;
            TextView phraseTv;

            public PhraseViewHolder(View itemView) {
                super(itemView);
                cv = (CardView) itemView.findViewById(R.id.cv);
                phraseTv = (TextView) itemView.findViewById(R.id.phrase_tv);
            }
        }
    }
