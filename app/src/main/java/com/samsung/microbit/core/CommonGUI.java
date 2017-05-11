package com.samsung.microbit.core;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;

/**
 * Contains some common user interface elements such as an alert dialog window.
 */
public class CommonGUI {

    //TODO: consider to use somewhere or remove
    public static void commonAlertDialog(Context parent, String title, String message) {

        AlertDialog alertDialog = new AlertDialog.Builder(parent, AlertDialog.THEME_HOLO_DARK).create();
        alertDialog.setTitle(title);

        alertDialog.setMessage(Html.fromHtml(message));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }
        );

        alertDialog.show();
    }

}
