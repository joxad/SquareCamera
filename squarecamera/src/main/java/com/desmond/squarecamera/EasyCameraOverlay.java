package com.desmond.squarecamera;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * Created by josh on 10/08/16.
 */
public abstract class EasyCameraOverlay extends RelativeLayout {

    public EasyCameraOverlay(Context context) {
        super(context);
        init();
    }

    public EasyCameraOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EasyCameraOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init() {
        inflate(getContext(), layout(), this);
    }

    private int layout() {
        return 0;
    }


}
