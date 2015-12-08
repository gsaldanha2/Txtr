package txtr.apps.armorg.com.txtr;

import android.app.Application;

import com.flurry.android.FlurryAgent;

/**
 * Created by Gregory on 12/7/2015.
 */
public class FlurryApplication extends Application {
    @Override
    public void onCreate(){
        super.onCreate();
        FlurryAgent.init(this, "SGMR3BDY3YJWSQ2N4MPK");
    }
}
