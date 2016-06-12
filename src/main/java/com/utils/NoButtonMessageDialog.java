package com.utils;

import android.app.AlertDialog;
import android.content.Context;

/**
 * Created by Lehyu on 2016/5/22.
 */
public class NoButtonMessageDialog extends AlertDialog {
    public NoButtonMessageDialog(Context context) {
        super(context);
        init();
    }

    protected void init(){
        setCanceledOnTouchOutside(false);
        setCancelable(true);
    }
}
