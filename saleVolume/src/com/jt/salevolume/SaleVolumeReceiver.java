package com.jt.salevolume;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

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
//			SharedPreferences sp = context.getSharedPreferences(
//					SaleVolumeService.SALEVOLUME, Context.MODE_PRIVATE);
//			boolean timeout = sp.getBoolean(SaleVolumeService.TIMEOUT, false);
//			// Log.e("JT", "startService");
			if (readFlag() == 0) {
				Intent intentService = new Intent(SERVICE_ACTION);
				context.startService(intentService);
			}
		}
	}
	
	private int readFlag() {
		File file = new File(SaleVolumeService.SALE_APP_FILE);
		int flag = 0;
		if (file.exists()) {
			try {
				DataInputStream is = new DataInputStream(
						new BufferedInputStream(new FileInputStream(SaleVolumeService.SALE_APP_FILE)));
				flag = is.readInt();
				is.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 
		Log.e("JT", "readFlag flag: " + flag);
		return flag;
	}

}
