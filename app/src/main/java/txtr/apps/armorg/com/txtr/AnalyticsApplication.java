package txtr.apps.armorg.com.txtr;

import android.app.Application;
import com.flurry.android.FlurryAgent;

/**
 * Created by Gregory on 12/11/2015.
 */
public class AnalyticsApplication extends Application {
    public void onCreate() {
        super.onCreate();
        FlurryAgent.init(this, "PNQ9XZBRTR4M4YDJ22Q6");
        //your code
    }
}
