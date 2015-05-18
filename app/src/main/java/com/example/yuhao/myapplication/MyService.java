package com.example.yuhao.myapplication;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.telephony.PhoneStateListener;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;


/**
 * Created by YUHAO on 2015/01/03.
 */
public class MyService extends Service implements Runnable{

    private final String DEBUG_TAG = "Activity01";
    private static String IpAddress = "192.168.0.103";
    private static int Port = 5554;
    private IBroadcastReceiver broadcastReceiver;
    private static final String SMS_ACTION = "android.provider.Telephony.SMS_RECEIVED";

    private int intLevel;
    private int intScale;
    private int status;

    public boolean monitor_MSM;
    public boolean monitor_phoneCall;
    public boolean monitor_battery;

    private OnProgressListener onProgressListener;

    Socket s;
    BufferedReader in;
    PrintWriter out;

    public void setOnProgressListener(OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
    }


    @Override
    public void onCreate() {
        // initial value of some param
        monitor_MSM = true;
        monitor_battery = true;
        monitor_phoneCall = true;

        exPhoneCallListener myPhoneCallListener = new exPhoneCallListener();
        // get telephony service
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        // register phone listener
        tm.listen(myPhoneCallListener, PhoneStateListener.LISTEN_CALL_STATE);

        // register broadcastReceiver
        broadcastReceiver = new IBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SMS_ACTION);
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(broadcastReceiver, intentFilter);

        new Thread(this).start();  // use socket to receive and send msg.
    }

    Handler h=new Handler(){
        public void handleMessage(Message msg) {
            String message = (String) msg.obj;  //接收线程的消息
            System.out.println("Handler:"+message);
            onProgressListener.onProgress(message);
//            show.append("\n"+message);  //在界面显示
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return new MsgBinder();
    }

    public class MsgBinder extends Binder {
        // get the instance of this service
        public MyService getService(){
            return MyService.this;
        }
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        try {
            s = new Socket(IpAddress,Port);
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            out = new PrintWriter(s.getOutputStream());

            while(true){
                String str=in.readLine();
                if (str != null) {
                    Message m=new Message();  //Message对象
                    m.obj=str;
                    h.sendMessage(m); //向Handler发送消息
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service stop", Toast.LENGTH_SHORT).show();
        try {
            s.close();
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Phone calling state.  Head: 0002
    public class exPhoneCallListener extends PhoneStateListener
    {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if(monitor_phoneCall)
            {
                switch (state)
                {
                    case TelephonyManager.CALL_STATE_IDLE:
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        String name = getContactName(incomingNumber);
                        out.print("0002" + name);
                        out.flush();
                    default:
                        break;
                }
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    }

    // this class will active when receive SMS, phone call etc...
    // then send the info to the PC server through socket.
    public class IBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Battery usage, Header:0001
            if(monitor_battery)
            {
                if(action.equals(Intent.ACTION_BATTERY_CHANGED))
                {
                    intLevel = intent.getIntExtra("level", 0);
                    intScale = intent.getIntExtra("scale", 100);
                    status = intent.getIntExtra("status", 0);

                    if(intLevel%intScale <= 20 && (status != BatteryManager.BATTERY_STATUS_CHARGING))
                    {
                        if(intLevel%intScale == 20)
                        {
                            out.print("0001" + intLevel%intScale);
                            out.flush();
                        }
                        if(intLevel%intScale <= 5)
                        {
                            out.print("0001" + intLevel%intScale);
                            out.flush();
                        }
                    }
                }
            }

            //SMS receiver, Header:0000
            if(monitor_MSM)
            {
                if(action.equals(SMS_ACTION))
                {
                    Bundle bundle = intent.getExtras();
                    if(bundle != null)
                    {
                        Object[] myOBJpdus = (Object[]) bundle.get("pdus");
                        android.telephony.SmsMessage[] messages = new android.telephony.SmsMessage[myOBJpdus.length];
                        for(int i = 0; i<myOBJpdus.length; i++)
                        {
                            messages[i] = android.telephony.SmsMessage.createFromPdu((byte[]) myOBJpdus[i]);
                        }

                        for(android.telephony.SmsMessage currentMessage : messages)
                        {
                            // get the body of MSM
                            String SMS_body = currentMessage.getDisplayMessageBody();
                            // get the sender name
                            String SMS_name = getContactName(currentMessage.getDisplayOriginatingAddress());
                            // use socket to send to PC
                            out.print("0000" + SMS_name + "$$$$" + SMS_body);
                            out.flush();
                        }
                    }
                }
            }
        }
    }

    public interface OnProgressListener
    {
        void onProgress(String progress);
    }

    public String getContactName(String phoneNum)
    {
        // transferm to right format for finding the name
        char[] numtochar = phoneNum.toCharArray();
        if(numtochar.length == 10){
            StringBuffer sb = new StringBuffer();

            for(int i = 0; i < numtochar.length; i++){
                if(i == 0){
                    sb.append("(" + String.valueOf(numtochar[i]));
                }else if(i == 2){
                    sb.append(String.valueOf(numtochar[i]) + ") ");
                }else if(i == 5){
                    sb.append(String.valueOf(numtochar[i]) + "-");
                }else{
                    sb.append(String.valueOf(numtochar[i]));
                }
            }

            phoneNum = sb.toString();
        }

        String[] projection = new String[]
                {
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                };

        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, // Which columns to return.
                ContactsContract.CommonDataKinds.Phone.NUMBER + "=?", new String[] {phoneNum},
                null);

        if (cursor == null) {
            return null;
        }

        if(cursor.getCount() == 0)  // no recorder in the contact
        {
            return phoneNum;
        }else if(cursor.getCount() > 0){
            cursor.moveToFirst();
            return cursor.getString(1);
        }

        return null;
    }
}
