package com.example.swathi.navigation;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
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
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private Values v = new Values();
    double[] vx=new double[31];
    double[] vy=new double[31];
    private float xid = 0, yid=0;
    double zid= 2.2;
    final find_path f = new find_path();
    String url="http://10.132.124.50:3000/", S;
    int scan_count=0, room_count=-1;

    Timer T=new Timer();
    WifiManager wifiManager;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList;
    List<String> listOfProvider;
    TileView t;
    FloatingActionButton fab;
    RadioGroup radioGroup;
    ImageView marker;
    View V,W;
    CompositePathView.DrawablePath drawablePath = new CompositePathView.DrawablePath();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        t=(TileView)findViewById(R.id.tileView);
        t.setSize(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
        t.addDetailLevel(1f, "b2f2.png", getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);

        ImageView marker = new ImageView(this);
        marker.setImageResource(R.drawable.push_pin);

        fab= (FloatingActionButton) findViewById(R.id.fb);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radioGroup = (RadioGroup) findViewById(R.id.myRadioGroup);
                radioGroup.setVisibility(View.VISIBLE);
                fab.setVisibility(View.GONE);
                radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        if (checkedId == R.id.radioButton) S = "Maj";
                        else if (checkedId == R.id.radioButton2) S = "Hal";
                        else if (checkedId == R.id.radioButton3) S = "Fuj";
                        else if (checkedId == R.id.radioButton4) S = "Ova";
                        else if (checkedId == R.id.radioButton5) S = "Cub";
                        else if (checkedId == R.id.radioButton6) S = "Flo";
                        else S = "Gla";

                    }
                });

                Button B1 = (Button) findViewById(R.id.B1);
                B1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        room_count++;
                        radioGroup.setVisibility(View.GONE);
                        findloc(S);
                        fab.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        listOfProvider=new ArrayList<>();
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        receiverWifi = new WifiReceiver();

        scanning();

        T.schedule(new TimerTask() {
            @Override
            public void run() {
                scan_count++;
                scanning();
            }
        }, 5000,5000); //ReScan every 5 secs

        f.floor = "b2f2";

        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
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
        @TargetApi(Build.VERSION_CODES.KITKAT)
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
            if(sortedMap.get("twdata").size() < 2) {
                Toast.makeText(getApplicationContext(), "Too few APs available", Toast.LENGTH_LONG).show();
            }
//            else if (sortedMap.get("twdata").size() == 2) {
//                for (int i = 0; i < 2; i++) {
//
//                    v.BSSID[i] = sortedMap.get("twdata").get(i).BSSID;
//                    v.RSSI[i] = sortedMap.get("twdata").get(i).level;
//
//                    double n, a = -32;
//                    if (v.RSSI[i] > -50) n = 2;
//                    else n = 2.5;
//                    v.d[i] = 0.14 * Math.pow(10, ((a - v.RSSI[i]) / (10 * n)));
//                    xyfrombssid(v.BSSID[i], i);
//                }
//                v.x[2]=v.y[2]=0;
//                if(v.RSSI[0]>v.RSSI[1]) {
//                    if(finddistance(0,0,v.x[0],v.y[0])>finddistance(0,0,v.x[1],v.y[1]))
//                        v.d[2]=finddistance(0,0,v.x[0],v.y[0])+v.d[0];
//                    else
//                        v.d[2]=finddistance(0,0,v.x[0],v.y[0])-v.d[0];
//                }
//                else {
//                    if(finddistance(0,0,v.x[1],v.y[1])>finddistance(0,0,v.x[0],v.y[0]))
//                        v.d[2]=finddistance(0,0,v.x[1],v.y[1])+v.d[1];
//                    else
//                        v.d[2]=finddistance(0,0,v.x[1],v.y[1])-v.d[1];
//                }
//
//                getres();
//            }
            else {
                for (int i = 0; i < 3; i++) {

                    v.BSSID[i] = sortedMap.get("twdata").get(i).BSSID;
                    v.RSSI[i] = sortedMap.get("twdata").get(i).level;


                    double n, a = -32;
                    if (v.RSSI[i] > -50) n = 2;
                    else n = 2.5;
                    v.d[i] = 0.14 * Math.pow(10, ((a - v.RSSI[i]) / (10 * n)));
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

                        if (result.has("error")) return;

                        v.x[i] = result.get("xco").getAsFloat();
                        v.y[i] = result.get("yco").getAsFloat();
                        v.z[i] = result.get("zco").getAsDouble();

                        if (i == 2) getres(); // Calculate co-ordinates based on d1,d2,d3; x1,x2,x3; y1,y2,y3
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

                        if (!Double.isNaN(result.get("xi").getAsDouble())) {
                            if (!Double.isNaN(result.get("yi").getAsDouble())) {

                                if(v.z[0]==v.z[1]) zid=v.z[0];
                                else if(v.z[0]==v.z[2]) zid=v.z[0];
                                else if(v.z[1]==v.z[2]) zid=v.z[1];
                                else Toast.makeText(getApplicationContext(),"Error",Toast.LENGTH_LONG);

                                yid = 246 * Math.abs(result.get("xi").getAsFloat());
                                xid = 122 * Math.abs(result.get("yi").getAsFloat());

                                mark((double)xid, (double)yid, "S");
                                findloc(S);
                            }
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

                        if (result.has("error")) return;

                        if (zid == result.get("zco").getAsDouble()) {
                            mark(result.get("xco").getAsDouble(), result.get("yco").getAsDouble(), "D");
                            pathfinder(xid, yid, result.get("xco").getAsFloat(), result.get("yco").getAsFloat());
                        }

                        else {
                            mark(result.get("stairx").getAsDouble(), result.get("stairy").getAsDouble(), "D");
                            pathfinder(xid, yid, result.get("stairx").getAsFloat(), result.get("stairy").getAsFloat());
                        }
                    }
                });
    }

    public void pathfinder(final float xs, final float ys, final float xd, final float yd) {
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
                            vx[i]= result.get("xco").getAsJsonArray().get(i).getAsDouble();
                            vy[i]= result.get("yco").getAsJsonArray().get(i).getAsDouble();
                        }

                        double nns, nnd;
                        int s = 0, d = 0;

                        nns = finddistance(xs, ys, vx[0], vy[0]);
                        for (int i = 1; i < 31; i++) {
                            if (nns > finddistance(xs, ys, vx[i], vy[i])) {
                                nns = finddistance(xs, ys, vx[i], vy[i]);
                                s = i;
                            }
                        }

                        f.vertex = s;

                        nnd = finddistance(xd, yd, vx[0], vy[0]);
                        for (int i = 1; i < 31; i++) {
                            if (nnd > finddistance(xd, yd, vx[i], vy[i])) {
                                nnd = finddistance(xd, yd, vx[i], vy[i]);
                                d = i;
                            }
                        }

                        path(s, d, xd, yd);
                    }
                });
    }

    void  path(final int s, final int d, final float xd, final float yd) {
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

                        int l = 31, a = s, b = d, i;

                        int[] edge = new int[l];
                        for (i = 0; i < l; i++)
                            edge[i]=result.get("array").getAsJsonArray().get(i).getAsInt();

                        int[] path = new int[10];
                        path[0] = b;
                        i=1;

                        while (edge[b] != -1) {
                            b = edge[b];
                            path[i++] = b;
                        }

                        path[i] = a;

                        final Path Path = new Path();
                        Path.moveTo(xd, yd);
                        for (int j = 0; j < i+1; j++) {
                            Path.lineTo( (float) vx[path[j]] , (float) vy[path[j]]);
                        }
                        Path.lineTo(xid,yid);

                        if(scan_count!=0)
                            t.removePath(drawablePath);

                        drawablePath.path = Path;

                        DisplayMetrics metrics = getResources().getDisplayMetrics();

                        Paint p = new Paint();
                        p.setColor(Color.BLUE);
                        p.setStyle(Paint.Style.STROKE);
                        drawablePath.paint = p;
                        p.setShadowLayer(
                                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, metrics),
                                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, metrics),
                                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, metrics),
                                0x66000000
                        );
                        p.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, metrics));
                        p.setPathEffect(
                                new CornerPathEffect(
                                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, metrics)
                                )
                        );

                        t.drawPath(drawablePath);
                    }
                });
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    void mark(double x, double y, String s) {
        marker=new ImageView(this);
        marker.setImageResource(R.drawable.push_pin);

        if(Objects.equals(s, "S")) {
            t.removeMarker(V);
            V = t.addMarker(marker, x, y, -0.5f, -0.5f);
        }

        else {
            t.removeMarker(W);
            W = t.addMarker(marker, x, y, -0.5f, -0.5f);
        }
    }

    static double finddistance(float a, float b, double c, double d) {
        return Math.abs(Math.sqrt(Math.pow(a - c, 2) + Math.pow(b - d, 2)));
    }
}
