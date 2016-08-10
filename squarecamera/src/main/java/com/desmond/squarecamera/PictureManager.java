package com.desmond.squarecamera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * Created by josh on 09/08/16.
 */
public class PictureManager {


    /**
     *
     * @param context
     * @param data
     * @param photoRotation
     */
    public static void save(Context context, byte[] data, int photoRotation) {
        Bitmap bitmap = ImageUtility.decodeSampledBitmapFromByte(context, data);

        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
        ImageUtility.savePicture(context, rotatedBitmap);

    }



}
