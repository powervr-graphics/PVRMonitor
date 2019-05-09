/******************************************************************************

 @File         AboutDialog.java

 @Title        AboutDialog

 @Version

 @Copyright    Copyright (c) Imagination Technologies Limited.

 @Platform     ANSI compatible

 @Description  The GUI's About dialog.

 ******************************************************************************/
package com.powervr.PVRMonitor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class AboutDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View main = inflater.inflate(R.layout.dialog_about, null);
        TextView tv =  (TextView) main.findViewById(R.id.about_us_support);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv =  (TextView) main.findViewById(R.id.about_us_forum);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv =  (TextView) main.findViewById(R.id.about_counters);
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        builder.setView(main);
        //builder.setTitle("PowerVR Insider");
        //builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
        //    public void onClick(DialogInterface dialog, int id) {}
        //});
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
