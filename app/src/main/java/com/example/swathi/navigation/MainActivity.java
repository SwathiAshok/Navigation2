package com.example.swathi.navigation;

import com.example.swathi.navigation.utils.model.APS;
import com.example.swathi.navigation.API.MyAPI;
import com.example.swathi.navigation.utils.model.CAL;

import com.google.gson.Gson;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class MainActivity extends AppCompatActivity {

    String[] BSSID = new String[3];
    int[] RSSI = new int[3];
    Values v= new Values();
    double A=40.23;
    float xi, yi;
    int n=2;
    CAL xy;
    String S1;

    Button setWifi; // WiFi Toggle Button
    TextView T1, T2;
    WifiManager wifiManager;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList; //List of APs scanned
    List<String> listOfProvider;

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://10.132.127.90:3000")
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
                if (wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(false);
                    setWifi.setText("OFF");

                } else if (!wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(true);
                    setWifi.setText("ON");
                    scaning();
                }
            }
        });

        T1 = (TextView) findViewById(R.id.textView4);
        T2 = (TextView) findViewById(R.id.textView5);

        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

        if (wifiManager.isWifiEnabled()) {
            setWifi.setText("ON");
            scaning();

        }

    }

    private void scaning() {
        receiverWifi = new WifiReceiver();
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();



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
            Map<String, List<ScanResult>> sortedMap = new HashMap<String, List<ScanResult>>();
            for(ScanResult scanResult: wifiList) {
                if(sortedMap.get(scanResult.SSID) == null) {
                    sortedMap.put(scanResult.SSID, new ArrayList<ScanResult>());
                }
                sortedMap.get(scanResult.SSID).add(scanResult);
            }
            /* sorting of wifi provider based on level */
            Collections.sort(sortedMap.get("twdata"), new Comparator<ScanResult>() {
                @Override
                public int compare(ScanResult lhs, ScanResult rhs) {
                    return (lhs.level > rhs.level ? -1 : (lhs.level == rhs.level ? 0 : 1));
                    // returns -1 0 1 based on comparision of scan results RSSI level
                }
            });


            listOfProvider.clear();

            for (int i = 0; i < 3; i++) {

                BSSID[i] = sortedMap.get("twdata").get(i).BSSID.toString();
                RSSI[i] =  sortedMap.get("twdata").get(i).level;
                v.d[i]=0.11 * Math.exp( (RSSI[i]-A) / (10*n) );
                xyfrombssid(BSSID[i], i);
            }

             S1 = getJsonString(v);
             getres(S1);
             T1.setText((char) xi);
             T2.setText((char) yi);

			/*setting list of all wifi provider in a List*/
        }
    }

   private void xyfrombssid(String id, final int i) {
       final Call<APS> apsCall = api.getxyByBSSID(id);
       apsCall.enqueue(new Callback<APS>() {
           @Override
           public void onResponse(Response<APS> response, Retrofit retrofit) {
               v.x[i] = Float.parseFloat(new APS().getXco());
               v.y[i] = Float.parseFloat(new APS().getYco());
               Toast.makeText(getApplicationContext(),"Swathi",Toast.LENGTH_LONG).show();
           }

           @Override
           public void onFailure(Throwable t) {
               Toast.makeText(getApplicationContext(), "Failed !", Toast.LENGTH_LONG).show();
               //Message
           }
       });
   }

    private void getres(String S1) {
        Call<CAL> calCall = api.getres(S1);
        calCall.enqueue( new Callback<CAL>() {
            @Override
            public void onResponse(Response<CAL> response, Retrofit retrofit) {

                xi=xy.getX();
                yi=xy.getY();

            }

            @Override
            public void onFailure(Throwable t) {
                //Message
                Toast.makeText(getApplicationContext(), "Failed !", Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getJsonString(Values v) {
        Gson gson = new Gson();
        return gson.toJson(v);
    }
}

