package txtr.apps.armorg.com.txtr;

import android.graphics.Bitmap;

class Contact {
        String contactNum, contactName;
        Bitmap image;

        Contact(String name, String num, Bitmap image) {
            contactName = name;
            contactNum = num;
            this.image = image;
        }
    }