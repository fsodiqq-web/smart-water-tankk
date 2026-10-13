package id.smknis.smartwatertank;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int REQ_BT = 10;
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter adapter;
    private BluetoothSocket socket;
    private Thread readerThread;
    private volatile boolean reading = false;
    private Spinner deviceSpinner;
    private TextView connection, percentText, litersText, distanceText, heightText, statusText;
    private ProgressBar tankProgress;
    private Button connectButton;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ArrayList<BluetoothDevice> devices = new ArrayList<>();

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        adapter = BluetoothAdapter.getDefaultAdapter();
        buildUi();
        if (adapter == null) {
            connection.setText("Perangkat ini tidak memiliki Bluetooth");
            connectButton.setEnabled(false);
        } else if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQ_BT);
        } else refreshDevices();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20), dp(18), dp(20), dp(24));
        root.setBackgroundColor(Color.rgb(242,247,248)); scroll.addView(root); setContentView(scroll);
        TextView title = new TextView(this); title.setText("SMART GELAS"); title.setTextSize(24); title.setTypeface(null, Typeface.BOLD); title.setTextColor(Color.rgb(7,91,102)); root.addView(title);
        TextView subtitle = new TextView(this); subtitle.setText("Pemantauan isi gelas melalui Bluetooth HC-05"); subtitle.setTextSize(14); subtitle.setTextColor(Color.DKGRAY); subtitle.setPadding(0,dp(4),0,dp(18)); root.addView(subtitle);
        deviceSpinner = new Spinner(this); root.addView(deviceSpinner, lp(-1, dp(52)));
        connectButton = new Button(this); connectButton.setText("SAMBUNGKAN BLUETOOTH"); root.addView(connectButton, lp(-1,dp(52)));
        connection = new TextView(this); connection.setText("Pilih perangkat HC-05 yang sudah dipasangkan"); connection.setTextSize(13); connection.setPadding(0,dp(10),0,dp(12)); root.addView(connection);
        LinearLayout tankCard = card(); root.addView(tankCard, lp(-1,-2));
        TextView tankLabel = new TextView(this); tankLabel.setText("ISI GELAS"); tankLabel.setTextSize(14); tankLabel.setTypeface(null,Typeface.BOLD); tankLabel.setTextColor(Color.DKGRAY); tankCard.addView(tankLabel);
        percentText = new TextView(this); percentText.setText("--%"); percentText.setTextSize(44); percentText.setTypeface(null,Typeface.BOLD); percentText.setTextColor(Color.rgb(7,127,140)); percentText.setGravity(Gravity.CENTER); percentText.setPadding(0,dp(8),0,dp(8)); tankCard.addView(percentText,lp(-1,-2));
        tankProgress = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); tankProgress.setMax(100); tankProgress.setProgress(0); tankProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(8,127,140))); tankCard.addView(tankProgress,lp(-1,dp(18)));
        statusText = new TextView(this); statusText.setText("Status: menunggu data"); statusText.setTextSize(16); statusText.setTypeface(null,Typeface.BOLD); statusText.setGravity(Gravity.CENTER); statusText.setPadding(0,dp(12),0,0); tankCard.addView(statusText,lp(-1,-2));
        LinearLayout metrics = new LinearLayout(this); metrics.setOrientation(LinearLayout.HORIZONTAL); root.addView(metrics,lp(-1,-2));
        LinearLayout c1=card(); LinearLayout c2=card(); metrics.addView(c1,new LinearLayout.LayoutParams(0,-2,1)); LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(0,-2,1);p2.leftMargin=dp(8);metrics.addView(c2,p2);
        addMetric(c1,"VOLUME AIR","-- L",true); litersText=lastText;
        addMetric(c2,"JARAK SENSOR","-- cm",true); distanceText=lastText;
        LinearLayout heightCard=card(); root.addView(heightCard,lp(-1,-2)); addMetric(heightCard,"TINGGI AIR","-- cm",false); heightText=lastText;
        TextView note = new TextView(this); note.setText("Catatan: pasangkan HC-05 dari pengaturan Bluetooth Android terlebih dahulu. PIN umum: 1234 atau 0000."); note.setTextSize(12); note.setTextColor(Color.GRAY); note.setPadding(0,dp(14),0,0); root.addView(note);
        connectButton.setOnClickListener(v -> connect());
    }
    private TextView lastText;
    private void addMetric(LinearLayout card,String label,String value,boolean compact){TextView l=new TextView(this);l.setText(label);l.setTextSize(12);l.setTextColor(Color.GRAY);card.addView(l);lastText=new TextView(this);lastText.setText(value);lastText.setTextSize(compact?22:24);lastText.setTypeface(null,Typeface.BOLD);lastText.setTextColor(Color.rgb(7,91,102));lastText.setPadding(0,dp(6),0,0);card.addView(lastText);}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(16),dp(16),dp(16));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(18));c.setBackground(bg);c.setElevation(dp(2));LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=dp(12);c.setLayoutParams(p);return c;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}

    private void refreshDevices(){
        try { if(adapter==null)return; if(!adapter.isEnabled()){connection.setText("Aktifkan Bluetooth Android terlebih dahulu");return;}
            Set<BluetoothDevice> paired=adapter.getBondedDevices(); devices.clear(); ArrayList<String> names=new ArrayList<>();
            for(BluetoothDevice d:paired){devices.add(d);String n=d.getName();names.add((n==null?"Bluetooth":n)+"  •  "+d.getAddress());}
            if(names.isEmpty())names.add("Belum ada perangkat yang dipasangkan");
            deviceSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
        } catch(SecurityException e){connection.setText("Izin Bluetooth belum diberikan");}
    }
    private void connect(){
        if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},REQ_BT);return;}
        if(devices.isEmpty()){refreshDevices();Toast.makeText(this,"Pasangkan HC-05 di pengaturan Bluetooth dulu",Toast.LENGTH_LONG).show();return;}
        int idx=deviceSpinner.getSelectedItemPosition();if(idx<0||idx>=devices.size())return;
        connectButton.setEnabled(false);connection.setText("Sedang menyambungkan...");
        new Thread(()->{try{BluetoothDevice d=devices.get(idx);BluetoothSocket s=d.createRfcommSocketToServiceRecord(SPP_UUID);try{adapter.cancelDiscovery();}catch(SecurityException ignored){}s.connect();socket=s;reading=true;main.post(()->{connection.setText("Terhubung ke "+d.getName());connectButton.setText("TERHUBUNG");connectButton.setEnabled(true);});readLoop(s);}catch(Exception e){main.post(()->{connection.setText("Gagal terhubung. Pastikan HC-05 menyala dan sudah dipasangkan.");connectButton.setEnabled(true);});closeSocket();}}).start();
    }
    private void readLoop(BluetoothSocket s){readerThread=Thread.currentThread();try{BufferedReader r=new BufferedReader(new InputStreamReader(s.getInputStream()));String line;while(reading&&(line=r.readLine())!=null){parseLine(line.trim());}}catch(Exception e){main.post(()->connection.setText("Koneksi terputus"));}finally{closeSocket();main.post(()->{connectButton.setText("SAMBUNGKAN BLUETOOTH");connectButton.setEnabled(true);});}}
    private void parseLine(String line){String[] a=line.split(",");if(a.length<5||line.startsWith("ERROR"))return;try{final float dist=Float.parseFloat(a[0]),height=Float.parseFloat(a[1]),pct=Float.parseFloat(a[2]),liters=Float.parseFloat(a[3]);final String status=a[4].replace('_',' ');main.post(()->{int p=Math.max(0,Math.min(100,Math.round(pct)));percentText.setText(p+"%");tankProgress.setProgress(p);litersText.setText(String.format(java.util.Locale.US,"%.2f L",liters));distanceText.setText(String.format(java.util.Locale.US,"%.1f cm",dist));heightText.setText(String.format(java.util.Locale.US,"%.1f cm",height));statusText.setText("Status: "+status);statusText.setTextColor(p<=20?Color.rgb(190,55,45):p>=90?Color.rgb(20,130,75):Color.rgb(7,91,102));});}catch(Exception ignored){}}
    private synchronized void closeSocket(){reading=false;try{if(socket!=null)socket.close();}catch(Exception ignored){}socket=null;}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){super.onRequestPermissionsResult(requestCode,permissions,results);if(requestCode==REQ_BT){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)refreshDevices();else connection.setText("Izin Bluetooth diperlukan untuk menghubungkan HC-05");}}
    @Override protected void onDestroy(){closeSocket();super.onDestroy();}
}
