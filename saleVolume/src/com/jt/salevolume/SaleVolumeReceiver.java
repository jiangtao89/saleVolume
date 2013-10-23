package com.jt.salevolume;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class SaleVolumeReceiver extends BroadcastReceiver {

	public static final String SERVICE_ACTION = "com.jt.salevolume.SALEVOLUMESERVICE";

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();

		if (action != null && Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			SharedPreferences sp = context.getSharedPreferences(
					SaleVolumeService.SALEVOLUME, Context.MODE_PRIVATE);
			boolean timeout = sp.getBoolean(SaleVolumeService.TIMEOUT, false);
			// Log.e("JT", "startService");
			if (!timeout) {
				
				Intent intentService = new Intent(SERVICE_ACTION);
				context.startService(intentService);
			}
		}
	}

}
