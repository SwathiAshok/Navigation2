package com.example.swathi.navigation;

import com.example.swathi.navigation.utils.model.APS;
import com.example.swathi.navigation.API.MyAPI;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class MainActivity extends AppCompatActivity {

    String[] BSSID;
    int[] RSSI;
    double[] d;
    double A=40.23;
    float[] x, y;
    float xi, yi;
    int n=2;

    Button setWifi; // WiFi Toggle Button
    TextView T1, T2;
    WifiManager wifiManager;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList; //List of APs scanned
    List<String> listOfProvider;

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://10.4.21.217:3000")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    MyAPI api = retrofit.create(MyAPI.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listOfProvider = new ArrayList<>();

        wifiManager=(WifiManager) getSystemService(Context.WIFI_SERVICE);
        receiverWifi= new WifiReceiver();

        setWifi = (Button) findViewById(R.id.btn_wifi);
        setWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (wifiManager.isWifiEnabled() == true) {
                    wifiManager.setWifiEnabled(false);
                    setWifi.setText("OFF");

                } else if (wifiManager.isWifiEnabled() == false) {
                    wifiManager.setWifiEnabled(true);
                    setWifi.setText("ON");
                    scaning();
                }
            }
        });
        T1 = (TextView) findViewById(R.id.textView4);
        T2 = (TextView) findViewById(R.id.textView5);
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

        if (wifiManager.isWifiEnabled() == true) {
            setWifi.setText("ON");
            scaning();
        }
    }

    private void scaning() {
        receiverWifi = new WifiReceiver();
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        triangle();
    }


    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiverWifi);
    }

    protected void onResume() {
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    class WifiReceiver extends BroadcastReceiver {

        // This method is called when number of wifi connections is changed
        public void onReceive(Context c, Intent intent) {
            wifiList = wifiManager.getScanResults();

			/* sorting of wifi provider based on level */
            Collections.sort(wifiList, new Comparator<ScanResult>() {
                @Override
                public int compare(ScanResult lhs, ScanResult rhs) {
                    return (lhs.level > rhs.level ? -1 : (lhs.level == rhs.level ? 0 : 1));
                    // returns -1 0 1 based on comparision of scan results RSSI level
                }
            });

            listOfProvider.clear();

            for (int i = 0; i < 3; i++) {

                BSSID[i] = wifiList.get(i).BSSID;
                RSSI[i] =  wifiList.get(i).level;
                d[i]=0.11*Math.exp( (RSSI[i]-A) / (10*n) );
                xyfrombssid(BSSID[i], i);
            }
			/*setting list of all wifi provider in a List*/
        }
    }

    public void triangle() {
        float u = (float)(((d[2]*d[2]- d[3]*d[3]) - (x[2]*x[2] - x[3]*x[3]) - (y[2]*y[2] - y[3]*y[3])) / 2);
        float v = (float)(((d[2]*d[2] - d[1]*d[1]) - (x[2]*x[2] - x[1] *x[1] ) - (y[2] *y[2] - y[1] *y[1] )) / 2);
        yi = (v * (x[3] - x[2]) - u * (x[1] - x[2])) / ((y[1] - y[2]) * (x[1] - x[2]) - (y[1] - y[2]) * (x[1] - x[2]));
        xi = (u - yi * (y[3] - y[2])) / (x[3] - x[2]);

        T1.setText((char)xi);
        T2.setText((char)yi);
    }

   private void xyfrombssid(String id, final int i) {
        api.getxyByBSSID(id, new Callback<APS>() {
            @Override
            public void onResponse(Response<APS> response, Retrofit retrofit) {
                x[i] = Float.parseFloat(new APS().getXco());
                y[i] = Float.parseFloat(new APS().getYco());
            }

            @Override
            public void onFailure(Throwable t) {
             //Message
            }
        });
    }

}

