package com.example.swathi.navigation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    String[] BSSID = new String[3];
    int[] RSSI = new int[3];
    Values v = new Values();
    double A = -32;
    float xi = 0, yi = 0;
    int i,cend=3;

    Button setWifi; // WiFi Toggle Button
    TextView T1, T2;
    WifiManager wifiManager;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList; //List of APs scanned
    List<String> listOfProvider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listOfProvider = new ArrayList<>();

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        receiverWifi = new WifiReceiver();

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
                    scanning();
                }
            }
        });

        T1 = (TextView) findViewById(R.id.textView4);
        T2 = (TextView) findViewById(R.id.textView5);

        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

        if (wifiManager.isWifiEnabled()) {
            setWifi.setText("ON");
            scanning();
        }
    }

    private void scanning() {
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
            Map<String, List<ScanResult>> sortedMap = new HashMap<>();

            for (ScanResult scanResult : wifiList) {
                if (sortedMap.get(scanResult.SSID) == null) {
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

            if(sortedMap.get("twdata").size()<3) {
                Toast.makeText(getApplicationContext(), "Too few APs available", Toast.LENGTH_LONG).show();
            }
            else {
                for (i = 0; i < cend; i++) {

                    BSSID[i] = sortedMap.get("twdata").get(i).BSSID.toString();
                    RSSI[i] = sortedMap.get("twdata").get(i).level;

                    // Correct RSSI for LoS APs
//                if(RSSI[i]>-40)
//                    RSSI[i]=-40;

//                v.d[i]=0.139*calcD(RSSI[i], sortedMap.get("twdata").get(i).frequency);
                    v.d[i] = 0.14 * Math.pow(10, ((A - RSSI[i]) / (10 * 2)));

//                v.d[i]=calculateDistance(RSSI[i],sortedMap.get("twdata").get(i).frequency);
//                v.d[i] = 0.139 * Math.exp( 2.34*(A-RSSI[i]) / (10 * 2.4));
//                Toast.makeText(getApplicationContext(), String.valueOf(v.d[i]),Toast.LENGTH_LONG).show();

                    xyfrombssid(BSSID[i]);
                }
            }
//            float u = (float) ((Math.pow(v.d[2], 2) - Math.pow(v.d[3], 2) - Math.pow(v.x[2], 2) + Math.pow(v.x[3], 2) - Math.pow(v.y[2], 2) + Math.pow(v.y[3], 2)) / 2);
//            float v1 = (float) ((Math.pow(v.d[2], 2) - Math.pow(v.d[1], 2) - Math.pow(v.x[2], 2) + Math.pow(v.x[1], 2) - Math.pow(v.y[2], 2) + Math.pow(v.y[1], 2)) / 2);
//
//            yi = (u * (v.x[3] - v.x[2]) - v1 * (v.x[1] - v.x[2])) / (((v.y[1] - v.y[2]) * (v.x[3] - v.x[2])) - ((v.y[3] - v.y[2]) * (v.x[1] - v.x[2])));
//            xi = (u - yi * (v.y[3] - v.y[2])) / (v.x[3] - v.x[2]);


        }
    }

    private void xyfrombssid(String id) {
        String URL = "http://10.132.125.49:3000/AP/" + id;
        Ion.with(this)
                .load(URL)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (e != null) {
                            Toast.makeText(getApplicationContext(), "Error in GET", Toast.LENGTH_LONG).show();
                            System.out.println("Error in GET");
                            return;
                        }

                        if (result.has("error")) {
                            cend++;
                            return;
                        }

                        v.x[i] = result.get("xco").getAsFloat();
                        v.y[i] = result.get("yco").getAsFloat();

                        if (i == 2) {
                            getres();
                            /* Calculate co-ordinates based on d1,d2,d3; x1,x2,x3; y1,y2,y3 */
                        }
                    }
                });
    }

    private void getres() {

        Ion.with(this)
                .load("http://10.132.125.49:3000/CAl")
                .setJsonPojoBody(v)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
//                        Toast.makeText(getApplicationContext(), "Calling", Toast.LENGTH_LONG).show();
                        // do stuff with the result or error
                        if (e != null) {
                            Toast.makeText(getApplicationContext(), "Error in POST", Toast.LENGTH_LONG).show();
                            System.out.println("Error in POST");
                            return;
                        }

                        if(!Double.isNaN(xi)) {
                            if(!Double.isNaN(yi)) {
                                xi = result.get("xi").getAsFloat();
                                yi = result.get("yi").getAsFloat();
                            }
                            else xi= (float) 0.0;
                        }
                        else {
                            yi= (float) 0.0;
                        }

                        T1.setText(String.valueOf(xi));
                        T2.setText(String.valueOf(yi));
                    }
                });
    }

//    public double calculateDistance(double levelInDb, double freqInMHz) {
//        double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(levelInDb)) / 20.0;
//        return 0.14*Math.pow(10.0, exp);
//    }
//
//    public double calcD(int lvl, int freq) {
//        double n = (lvl-11+20*Math.log10(freq))/31;
//        return Math.pow(10, n);
//    }

}


