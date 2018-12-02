package com.skymonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;

public class SkyAlertPopup extends Activity{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activityskyalert);

	    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
	    alertDialogBuilder.setTitle("SkyMonitor Alert");
		final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

	    String message = getIntent().getStringExtra("Message");
	    String from = getIntent().getStringExtra("from");
	    Log.i("SkyAlertPopup", message);
	    alertDialogBuilder
	            .setMessage(message)
	            .setCancelable(false)
	            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int id) {
	                    stopService(getIntent());
	                    dialog.cancel();
	                    vibrator.cancel();
	                    finish();
	                }
	            });

		long[] pattern = {0, 400, 200, 400, 300, 500},patternInvalidkey = {0, 400, 200, 400, 200, 400,200 ,400};
		if(from.equalsIgnoreCase("invalidkey") || from.equalsIgnoreCase("networkfail"))
			vibrator.vibrate(pattern, -1);
		else
			vibrator.vibrate(patternInvalidkey, -1);

		// create alert dialog
	    AlertDialog alertDialog = alertDialogBuilder.create();

	    // show it
	    alertDialog.show();
	}
}
