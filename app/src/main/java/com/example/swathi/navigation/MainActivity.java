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
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.qozix.tileview.TileView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.view.View.INVISIBLE;
import static android.view.View.OnClickListener;
import static android.view.View.VISIBLE;
import static com.example.swathi.navigation.R.id;

public class MainActivity extends AppCompatActivity {

    private Values v = new Values();
    private final double A = -32;
    private double n;
    private float xi = 0, yi = 0;

    WifiManager wifiManager;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList; //List of APs scanned
    List<String> listOfProvider;
    Button circleButton, B1, setWifi;
    TileView tileview;
    ImageView marker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tileview=(TileView)findViewById(id.tileView);
        tileview.setSize(3349, 6000);
        tileview.addDetailLevel(1f, "b2f2.png", 3349, 6000);
        marker= new ImageView(this);
        marker.setImageResource(R.drawable.images);

        listOfProvider = new ArrayList<>();

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        receiverWifi = new WifiReceiver();

        setWifi = (Button) findViewById(id.btn_wifi);
        setWifi.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(false);
                    setWifi.setText("OFF");
                }

                else {
                    wifiManager.setWifiEnabled(true);
                    setWifi.setText("ON");
                    scanning();
                }
            }
        });

        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

        if (wifiManager.isWifiEnabled()) {
            setWifi.setText("ON");
            scanning();
        }



        circleButton= (Button) findViewById(id.cb);
        B1= (Button) findViewById(id.button);

        circleButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                circleButton.setVisibility(INVISIBLE);
                B1.setVisibility(VISIBLE);
            }
        });

        B1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                B1.setVisibility(INVISIBLE);
                circleButton.setVisibility(VISIBLE);
            }
        });
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
                for (int i = 0; i < 3; i++) {

                    v.BSSID[i] = sortedMap.get("twdata").get(i).BSSID;
                    v.RSSI[i] = sortedMap.get("twdata").get(i).level;
                    if(v.RSSI[i]>-50) n=2;
                    else n=2.5;
                    v.d[i] = 0.14 * Math.pow(10, ((A - v.RSSI[i]) / (10 * n)));
                    xyfrombssid(v.BSSID[i],i);
                }
            }
        }
    }

    private void xyfrombssid(String id, final int i) {
        String URL = "http://10.132.126.70:3000/AP/" + id;
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
    } //GET calls

    private void getres() {
        Ion.with(this)
                .load("http://10.132.126.70:3000/CAl")
                .setJsonPojoBody(v)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (e != null) {
                            Toast.makeText(getApplicationContext(), "Error in POST", Toast.LENGTH_LONG).show();
                            System.out.println("Error in POST");
                            return;
                        }

                        if (!Double.isNaN(xi)) {
                            if (!Double.isNaN(yi)) {
                                xi = result.get("xi").getAsFloat();
                                yi = result.get("yi").getAsFloat();
                                tileview.addMarker(marker, xi, yi, tileview.getPivotX(), tileview.getPivotY());
                            } else xi = (float) 0.0;
                        } else {
                            yi = (float) 0.0;
                        }

                        System.out.println("marker");
                    }
                });
    } //triangulation

//    private void findloc(String s) {
//
//    String URL = "http://10.132.126.70:3000/LOC/" + s;
//    Ion.with(this)
//            .load(URL)
//            .asJsonObject()
//            .setCallback(new FutureCallback<JsonObject>() {
//                @Override
//                public void onCompleted(Exception e, JsonObject result) {
//                    if (e != null) {
//                        Toast.makeText(getApplicationContext(), "Error in GET", Toast.LENGTH_LONG).show();
//                        System.out.println("Error in GET");
//                        return;
//                    }
//
//                    if (result.has("error")) {
//                        return;
//                    }
//
//                    marker.setImageResource(R.drawable.images);
//                    tileview.addMarker(marker, result.get("xco").getAsFloat(), result.get("yco").getAsFloat(), tileview.getPivotX(), tileview.getPivotY());
//                }
//            });
//}
//
//    private void placeMarker(float x, float y) {
}
