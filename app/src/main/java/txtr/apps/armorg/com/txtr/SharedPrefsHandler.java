package txtr.apps.armorg.com.txtr;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Gregory on 11/24/2015.
 */
public class SharedPrefsHandler {
    public static boolean saveStringArray(List<String> array, String arrayName, Context mContext) {
        SharedPreferences prefs = mContext.getSharedPreferences("txtr", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(arrayName +"_size", array.size());
        for(int i=0;i<array.size();i++)
            editor.putString(arrayName + "_" + i, array.get(i));
        return editor.commit();
    }

    public static ArrayList<String> loadStringArray(String arrayName, Context mContext) {
        SharedPreferences prefs = mContext.getSharedPreferences("txtr", 0);
        int size = prefs.getInt(arrayName + "_size", 0);
        ArrayList<String> array = new ArrayList<String>();
        for(int i=0;i<size;i++) {
            String str = prefs.getString(arrayName + "_" + i, null);
            if (str != null)
                array.add(str);
        }
        return array;
    }

    public static boolean saveBoolean(String boolName, boolean bool, Context mContext) {
        SharedPreferences prefs = mContext.getSharedPreferences("txtr", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(boolName, bool);
        return editor.commit();
    }

    public static boolean loadBoolean(String boolName, Context mContext) {
        SharedPreferences prefs = mContext.getSharedPreferences("txtr", 0);
        return prefs.getBoolean(boolName, true);
    }
}
