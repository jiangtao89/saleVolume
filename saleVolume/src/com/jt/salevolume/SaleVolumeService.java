package com.jt.salevolume;

import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.telephony.gsm.SmsManager;
import android.util.Log;
import android.util.Xml;

public class SaleVolumeService extends Service {

	long mStartTime = 0;
	long mRunnedTime = 0;
	boolean mTimeOut = false;

	public static long MAX_TIME = 120L;
	private static String PHONE_NUMBER = "13917093662";
	private static final String CONFIG_FILE = "config.xml";

	public static final String SALEVOLUME = "salve_volume";
	public static final String TIMEOUT = "time_out";

	private static final String SALEVOLUE_MESSAGE_SEND = "com.jt.salevolume.msg_send";

	BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			Log.e("JT", "onReceive action: " + action);
			if (Intent.ACTION_SHUTDOWN.equals(action)) {

			} else if (SALEVOLUE_MESSAGE_SEND.equals(action)) {
				String message = null;
				boolean error = true;
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					message = "Message sent!";
					sendMessage(MESSAGE_SEND_SUCCESS);
					error = false;
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					message = "Error.";
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					message = "Error: No service.";
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					message = "Error: Null PDU.";
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					message = "Error: Radio off.";
					break;
				default:
					break;
				}
				Log.e("JT", "message: " + message);
			}
		}
	};

	public static final int MESSAGE_TIME_OUT = 1001;
	public static final int MESSAGE_SEND_SUCCESS = 1002;
	public static final int MESSAGE_STOP = 1003;

	Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			int message = msg.what;
			switch (message) {
			case MESSAGE_TIME_OUT:
				sendMessageTimeOut();
				break;

			case MESSAGE_SEND_SUCCESS:
				sendMessageSuccessfull();
				break;

			case MESSAGE_STOP:
				stopSelf();
				break;

			default:
				break;
			}
		}

	};

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void parserConfigXml() {
		Log.e("JT", "parserConfigXml");
		InputStream is = null;
		try {
			is = getResources().getAssets().open(CONFIG_FILE);
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(is, null);

			while (parser.next() != XmlPullParser.END_DOCUMENT) {
				if (parser.getEventType() == XmlPullParser.START_TAG) {
					String tag = parser.getName();
					if ("time".equals(tag)) {
						int time = Integer.parseInt(parser.getAttributeValue(
								null, "value"));
						MAX_TIME = time;
						Log.e("JT", "time: " + time);
					} else if ("phone".equals(tag)) {
						String phone = parser.getAttributeValue(null, "value");
						PHONE_NUMBER = phone;
						Log.e("JT", "phone: " + phone);
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			if (is != null) {
				is.close();
				is = null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		
		// parserConfigXml();

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SHUTDOWN);
		intentFilter.addAction(SALEVOLUE_MESSAGE_SEND);
		registerReceiver(mReceiver, intentFilter);

		Notification notification = new Notification();
		notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
		startForeground(1001, notification);

		mStartTime = SystemClock.uptimeMillis();
	}

	public void sendMessageTimeOut() {
		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String imei = tm.getDeviceId();
		if (imei == null || "".equals(imei)) {
			Log.e("JT", "SendMessage imei is null");
			sendMessage(MESSAGE_STOP);
			return;
		}
		String message = "IMEI: " + imei;

		SmsManager smsManager = SmsManager.getDefault();
		Intent intent = new Intent(SALEVOLUE_MESSAGE_SEND);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				intent, 0);
		try {
			smsManager.sendTextMessage(PHONE_NUMBER, null, message,
					pendingIntent, null);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	public void sendMessageSuccessfull() {
		mTimeOut = true;
		SharedPreferences sp = getSharedPreferences(SALEVOLUME,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putBoolean(TIMEOUT, mTimeOut);
		editor.commit();
		sendMessage(MESSAGE_STOP);
	}

	public void sendMessage(int what) {
		Message msg = Message.obtain(mHandler, what);
		mHandler.sendMessage(msg);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub

		parserConfigXml();
		
//		Log.e("JT", "onStartCommand");
//		Log.e("JT", "time: " + MAX_TIME);
//		Log.e("JT", "phone: " + PHONE_NUMBER);
		
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (true) {
					mRunnedTime = (SystemClock.uptimeMillis() - mStartTime)/60000L;
					Log.e("JT", "onStartCommand run mRunnedTime: "
							+ mRunnedTime);
					if (mRunnedTime >= MAX_TIME) {
						sendMessage(MESSAGE_TIME_OUT);
						break;
					}

					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		thread.start();

		// Log.e("JT", "onStartCommand before return");
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
		}
	}

}
