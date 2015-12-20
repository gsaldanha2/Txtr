package txtr.apps.armorg.com.txtr;

import android.graphics.Bitmap;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

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
            cvh.contactNumTv.setText(contacts.get(i).contactName);
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