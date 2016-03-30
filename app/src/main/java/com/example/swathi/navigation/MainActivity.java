package com.example.swathi.navigation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.qozix.tileview.TileView;
import com.qozix.tileview.paths.CompositePathView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.view.View.VISIBLE;
import static com.example.swathi.navigation.R.id;

public class MainActivity extends AppCompatActivity {

    private Values v = new Values();
    vertex[] V = new vertex[31];

    private final double A = -32;
    private double n;
    private float xi = 0, yi = 0;
    final findpath f = new findpath();


    String url="http://10.132.125.91:3000/";

    WifiManager wifiManager;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList; //List of APs scanned
    List<String> listOfProvider;
    Button room, B1, setWifi;
    EditText e1;
    TileView t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        t=(TileView)findViewById(id.tileView);
        t.setSize(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
        t.addDetailLevel(1f, "b2f2.png", getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);

        listOfProvider=new ArrayList<>();

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        receiverWifi = new WifiReceiver();

        setWifi = (Button) findViewById(id.btn_wifi);
        setWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(false);
                    setWifi.setText("OFF");
                } else {
                    wifiManager.setWifiEnabled(true);
                    setWifi.setText("ON");
                    scanning();
                }
            }
        });
        f.floor = "b2f2";

        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

        if (wifiManager.isWifiEnabled()) {
            setWifi.setText("ON");
            scanning();
        }

        room = (Button) findViewById(id.rb);
        B1 = (Button) findViewById(id.button);
        e1 = (EditText) findViewById(id.editText);

        room.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                room.setVisibility(View.GONE);
//                B1.setVisibility(View.VISIBLE);
//                e1.setVisibility(View.VISIBLE);
                findloc("Oval");
            }
        });

        B1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                B1.setVisibility(View.GONE);
                findloc(e1.getText().toString());
                e1.setVisibility(View.GONE);
                room.setText(VISIBLE);
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

            if (sortedMap.get("twdata").size() < 3) {
                Toast.makeText(getApplicationContext(), "Too few APs available", Toast.LENGTH_LONG).show();
            } else {
                for (int i = 0; i < 3; i++) {


                    if(sortedMap.get("twdata").get(i).BSSID=="d8:b1:90:b2:ba:20") {
                        v.BSSID[i] = sortedMap.get("twdata").get(4).BSSID;
                        v.RSSI[i] = sortedMap.get("twdata").get(4).level;
                    }

                    else {
                        v.BSSID[i] = sortedMap.get("twdata").get(i).BSSID;
                        v.RSSI[i] = sortedMap.get("twdata").get(i).level;
                    }

                    if (v.RSSI[i] > -50) n = 2;
                    else n = 2.5;
                    v.d[i] = 0.14 * Math.pow(10, ((A - v.RSSI[i]) / (10 * n)));
                    xyfrombssid(v.BSSID[i], i);
                }
            }
        }
    }

    private void xyfrombssid(String id, final int i) {
        String URL = url+"AP/"+id;
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
                .load(url + "CAl")
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
                                mark(Math.abs(result.get("xi").getAsDouble()), Math.abs(result.get("yi").getAsDouble()));
//                                myHandler.publish(null);
                                findloc("Oval");
                            } else xi = (float) 0.0;
                        } else {
                            yi = (float) 0.0;
                        }
                    }
                });
    } //triangulation

    private void findloc(String s) {

        String URL = url+"LOC/" + s;
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

                        mark(result.get("xco").getAsDouble(), result.get("yco").getAsDouble());
                        path(xi, yi, result.get("xco").getAsFloat(), result.get("yco").getAsFloat());

                    }
                });
    }

    private void mark(double y,double x) {
        ImageView marker=new ImageView(this);
        marker.setImageResource(R.drawable.push_pin);
        t.addMarker(marker, 257 * x, 257 * y, -0.5f, -0.5f);
    }

    public void path(final float ys, final float xs, final float yd, final float xd) {
        Ion.with(this)
                .load(url + "VCO")
                .setJsonPojoBody(f)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (e != null) {
                            Toast.makeText(getApplicationContext(), "Error in POST", Toast.LENGTH_LONG).show();
                            System.out.println("Error in POST");
                            return;
                        }

                        for(int i=0;i<31;i++) {
                            V[i].x=  (result.get("xco").getAsJsonArray().get(i).getAsInt()/3.2);
                            V[i].y=result.get("yco").getAsJsonArray().get(i).getAsInt()/3.2;
                        }

                        double nns, nnd;
                        int s = 0, d = 0;

                        nns = finddistance(xs, ys, V[0].x, V[0].y);
                        for (int i = 1; i < 31; i++) {
                            if (nns > finddistance(xs, ys, V[i].x, V[i].y)) {
                                nns = finddistance(xs, ys, V[i].x, V[i].y);
                                s = i;
                            }
                        }
                        f.vertex = s;

                        nnd = finddistance(xd, yd, V[0].x, V[0].y);
                        d = 0;
                        for (int i = 1; i < 31; i++) {
                            if (nnd > finddistance(xd, yd, V[i].x, V[i].y)) {
                                nnd = finddistance(xd, yd, V[i].x, V[i].y);
                                d = i;
                            }
                        }

                        path2(s,d);
                    }
                });
    }

    static double finddistance(float a, float b, double c, double d) {
        return Math.abs(Math.sqrt(Math.pow(a - c, 2) + Math.pow(b - d, 2)));
    }

    void  path2(final int s, final int d) {
        Ion.with(this)
                .load(url + "Vertex")
                .setJsonPojoBody(f)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (e != null) {
                            Toast.makeText(getApplicationContext(), "Error in POST", Toast.LENGTH_LONG).show();
                            System.out.println("Error in POST");
                            return;
                        }

                        int l = result.get("length").getAsInt();
                        int[] edge = new int[l];
                        for (int i = 0; i < l; i++)
                            edge[i]=result.get("array").getAsJsonArray().get(i).getAsInt();


                        int a = s, b = d, i = 1;
                        int[] path = new int[10];

                        path[0] = b;

                        while (edge[b] != -1) {
                            b = edge[b];
                            path[i++] = b;
                        }

                        path[i] = a;

                        //draw path from source to V[i] to V[0] to destination i.e source to destination
                        final Path Path = new Path();

                        Path.moveTo(xi*257, yi*257);

                        for (int j = 0; j <= i; j++) {
                            Path.lineTo((float) V[path[i - j]].x*257, (float) V[path[i - j]].y*257);
                        }

                        Path.close();

                        CompositePathView.DrawablePath drawablePath = new CompositePathView.DrawablePath();
                        drawablePath.path = Path;

                        Paint p = new Paint();
                        p.setStyle(Paint.Style.STROKE);
                        drawablePath.paint = p;
                        t.drawPath(drawablePath);
                    }
                });
    }

}
