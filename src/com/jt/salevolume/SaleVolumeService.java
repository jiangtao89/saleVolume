package com.jt.salevolume;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.telephony.gsm.SmsManager;
import android.util.Log;
import android.util.Xml;
import android.view.WindowManager;

import com.mediatek.telephony.SmsManagerEx;

public class SaleVolumeService extends Service {

	long mStartTime = 0;
	long mRunnedTime = 0;
	boolean mTimeOut = false;

	public static long MAX_TIME = 1L;
	private static String PHONE_NUMBER = "10086";
	private static final String CONFIG_FILE = "config.xml";

	public static final String SALEVOLUME = "salve_volume";
	public static final String TIMEOUT = "time_out";

	private static final String SALEVOLUE_MESSAGE_SEND = "com.jt.salevolume.msg_send";

	public static final String SALE_APP_FILE = "/protect_f/sale.data";

	int mCurrentAvalidSim = 1;
	int mSimId = 0;
	String mSendMessage = "";

	public boolean mThreadRun = true;

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
				int resultCode = getResultCode();
				switch (resultCode) {
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

				if (resultCode != Activity.RESULT_OK && mCurrentAvalidSim == 2
						&& mSimId == 0) {
					sendMediatekMessage(1); // sim2
				}
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
			Log.e("JT", "message: " + message);
			switch (message) {
			case MESSAGE_TIME_OUT:
				// sendMessageMediatekTimeOut();
				showAlertWindow();
				break;

			case MESSAGE_SEND_SUCCESS:
				sendMessageSuccessfull();
				break;

			case MESSAGE_STOP:
				stopForeground(true);
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

		mStartTime = SystemClock.elapsedRealtime();
	}

	public void showAlertWindow() {

		// Intent intent = new Intent("com.jt.salevolume.ALTER");
		// startActivity(intent);

		AlertDialog.Builder builder = new Builder(this);
		builder.setIcon(android.R.drawable.ic_dialog_info)
				.setMessage(R.string.tip_message)
				/*
				 * .setNegativeButton(android.R.string.cancel, new
				 * OnClickListener() {
				 * 
				 * @Override public void onClick(DialogInterface dialog, int
				 * which) { // TODO Auto-generated method stub
				 * 
				 * } })
				 */
				.setPositiveButton(android.R.string.ok, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						// sendMessageMediatekTimeOut();
						// sendMessageSuccessfull();
						getCurrentSimInfo();
						sendMediatekMessage(mSimId);
					}
				});
		AlertDialog dialog = builder.create();
		dialog.getWindow()
				.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		dialog.show();
	}

	public void getCurrentSimInfo() {
		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

		// String message = "";
		String imei1 = "";
		String imei2 = "";
		String serialNumber1 = "";
		String serialNumber2 = "";

		mSimId = 0;

		try {
			Method getSimSerialNumberGemini = TelephonyManager.class
					.getDeclaredMethod("getSimSerialNumberGemini",
							new Class[] { int.class });

			Method getDeviceIdGemini = TelephonyManager.class
					.getDeclaredMethod("getDeviceIdGemini",
							new Class[] { int.class });

			imei1 = (String) getDeviceIdGemini.invoke(tm, 0);
			imei2 = (String) getDeviceIdGemini.invoke(tm, 1);

			int SIMID = 0;
			serialNumber1 = getSimSerialNumberGemini.invoke(tm, SIMID)
					.toString();
			Log.e("JT", "SendMessage serialNumber1: " + serialNumber1);

			SIMID = 1;
			serialNumber2 = getSimSerialNumberGemini.invoke(tm, SIMID)
					.toString();
			Log.e("JT", "SendMessage serialNumber2: " + serialNumber2);

			Log.e("JT", "SendMessage imei1: " + imei1);
			Log.e("JT", "SendMessage imei2: " + imei2);

			if (serialNumber1 == null || ("".equals(serialNumber1))) {
				if (serialNumber2 == null || ("".equals(serialNumber2))) {
					Log.e("JT", "SendMessage is null");
					sendMessage(MESSAGE_STOP);
					return;
				} else {
					mSimId = 1;
				}
			}

			mSendMessage += "IMEI1: " + imei1 + ", IMEI2: " + imei2;

		} catch (NoSuchMethodException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void sendMediatekMessage(int simId) {

		Log.e("JT", "SendMessage message: " + mSendMessage);
		Log.e("JT", "SendMessage id: " + simId);

		mSimId = simId;

		SmsManagerEx smsManagerEx = SmsManagerEx.getDefault();
		Intent intent = new Intent(SALEVOLUE_MESSAGE_SEND);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				intent, 0);
		try {
			smsManagerEx.sendTextMessage(PHONE_NUMBER, null, mSendMessage,
					pendingIntent, null, simId);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
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
		// mTimeOut = true;
		// SharedPreferences sp = getSharedPreferences(SALEVOLUME,
		// Context.MODE_PRIVATE);
		// SharedPreferences.Editor editor = sp.edit();
		// editor.putBoolean(TIMEOUT, mTimeOut);
		// editor.commit();
		File file = new File(SALE_APP_FILE);

		Log.e("JT", "file.getAbsolutePath(): " + file.getAbsolutePath());

		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			DataOutputStream Out = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(
							SALE_APP_FILE, false)));
			Out.writeInt(1);
			Out.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		sendMessage(MESSAGE_STOP);
	}

	public void sendMessage(int what) {
		Message msg = Message.obtain(mHandler, what);
		mHandler.sendMessage(msg);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub

		mThreadRun = true;
		parserConfigXml();

		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (mThreadRun) {
					Log.e("JT",
							"onStartCommand run (SystemClock.elapsedRealtime() - mStartTime)/1000: "
									+ (SystemClock.elapsedRealtime() - mStartTime)
									/ 1000L);

					mRunnedTime = (SystemClock.elapsedRealtime() - mStartTime) / 60000L;
					Log.e("JT", "onStartCommand run mStartTime: " + mRunnedTime);
					if (mRunnedTime >= MAX_TIME) {
						sendMessage(MESSAGE_TIME_OUT);
						mThreadRun = false;
						break;
					}

					try {
						Thread.sleep(5000);
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
		mThreadRun = false;
		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
		}
	}

}
