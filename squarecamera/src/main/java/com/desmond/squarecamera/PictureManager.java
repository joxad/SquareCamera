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
     * @param imageParameters
     */
    public static void save(Context context, byte[] data, int photoRotation, ImageParameters imageParameters) {
        imageParameters.mIsPortrait =
                context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        Bitmap bitmap = ImageUtility.decodeSampledBitmapFromByte(context, data);

        Matrix matrix = new Matrix();
        matrix.postRotate(photoRotation);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        ImageUtility.savePicture(context, bitmap);

    }



}
