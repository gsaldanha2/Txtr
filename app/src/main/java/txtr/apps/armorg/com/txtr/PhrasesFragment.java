package txtr.apps.armorg.com.txtr;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by Gregory on 11/24/2015.
 */
public class PhrasesFragment extends Fragment {

    private List<String> phrases;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_phrases, container);

        phrases = SharedPrefsHandler.loadStringArray("phrase_list", getActivity());

        final RecyclerView phrasesRv = (RecyclerView) rootView.findViewById(R.id.phrases_rv);
        phrasesRv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        phrasesRv.setLayoutManager(llm);

        RVPhraseAdapter adapter = new RVPhraseAdapter(phrases);
        phrasesRv.setAdapter(adapter);

        phrasesRv.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                CardCreatorActivity.phrase = phrases.get(position);
                view.setBackgroundColor(Color.LTGRAY);
            }
        }));

        return rootView;
    }
}
