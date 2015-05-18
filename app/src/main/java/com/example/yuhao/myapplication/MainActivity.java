package com.example.yuhao.myapplication;

import java.io.*;
import java.net.Socket;
import java.net.URLEncoder;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.*;
import android.app.Activity;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

public class MainActivity extends Activity{
    // widgets
    private TextView show;
    private CheckBox check_start;
    private CheckBox msm_check;
    private CheckBox phone_check;
    private CheckBox battery_check;

    private Intent intent;
    private MyService msgService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // register widgets
        show = (TextView) findViewById(R.id.show);
        msm_check = (CheckBox) findViewById(R.id.MSM);
        phone_check = (CheckBox) findViewById(R.id.phone_call);
        battery_check = (CheckBox) findViewById(R.id.battery_usage);
        check_start = (CheckBox) findViewById(R.id.start_service);

        msm_check.setOnCheckedChangeListener(m_checkboxListener);
        phone_check.setOnCheckedChangeListener(m_checkboxListener);
        battery_check.setOnCheckedChangeListener(m_checkboxListener);
        check_start.setOnCheckedChangeListener(m_checkboxListener);
        // bind service
        intent = new Intent(this, MyService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
        // start the service in the beginning
        startService(intent);
    }


    ServiceConnection conn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            // return the instance of MyService
            msgService = ((MyService.MsgBinder) service).getService();

            msgService.setOnProgressListener(new MyService.OnProgressListener() {
                @Override
                public void onProgress(String progress) {
                    show.setText(progress);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private CheckBox.OnCheckedChangeListener m_checkboxListener = new CheckBox.OnCheckedChangeListener(){
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (buttonView.getId())
            {
                case R.id.start_service:
                    if(isChecked)
                    {
                        bindService(intent, conn, Context.BIND_AUTO_CREATE);
                        startService(intent);
                    }else{
                        unbindService(conn);
                        stopService(intent);
                    }
                    break;

                case R.id.MSM:
                    if(isChecked)
                    {
                        msgService.monitor_MSM = true;
                    }else{
                        msgService.monitor_MSM = false;
                    }
                    break;

                case R.id.phone_call:
                    if(isChecked)
                    {
                        msgService.monitor_phoneCall = true;
                    }else{
                        msgService.monitor_phoneCall = false;
                    }
                    break;

                case R.id.battery_usage:
                    if(isChecked)
                    {
                        msgService.monitor_battery = true;
                    }else{
                        msgService.monitor_battery = false;
                    }
                    break;
            }
        }
    };

}

