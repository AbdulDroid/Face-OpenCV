package com.example.android.face;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface.OnClickListener;

/**
 * Created by Isa Abuljalil with Matriculation number: 13/SCI01/010
 * on 04/19/2017.
 */

public class MessageAlert {
    //This method will be invoked to display alert dialog
    public static void showAlert(String message, Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.setPositiveButton("OK", new OnClickListener(){
            public void onClick(DialogInterface dialog, int which){
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
