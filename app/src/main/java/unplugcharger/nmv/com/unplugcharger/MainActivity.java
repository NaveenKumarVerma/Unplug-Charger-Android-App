package unplugcharger.nmv.com.unplugcharger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public int level, status, temperature, voltage, charge_current, battery_capacity,
            remaining_time = -1, hr, min, plugged, charging_rate = 0, attempts = 0;
    public int flag = 1, charging_time_flag = 1;
    public String technology, rem_time;
    public boolean isCharging;
    public MediaPlayer mediaPlayer = null;
    public static final String TAG ="com.example.application.unplugcharger";
    public ProgressBar progress;
    public TextView BatteryLevel, BatteryStatus, ChargeSource, TextSource, BatteryHealth,
            BatteryVoltage, BatteryTemperature, TextHealth, TextVoltage, TextTemperature,
            Temp, TextCapacity, BatteryCapacity, BatteryTechnology, TextTechnology, TextCurrent,
            BatteryCurrent, Developers;
    public Button serButton;
    public Date dt;
    public Intent gintent;
    public Thread battery_charging_thread, music_loop_thread;

    private BroadcastReceiver BatteryReceiver = new BroadcastReceiver() {
        @Override
        //When Event is published, onReceive method is called
        public void onReceive(Context c, Intent intent) {
            gintent = intent;
            initComponents(intent);
            setColor();
            setIcon();

            dt = new Date();

            battery_capacity = Integer.parseInt(getBatCapacity());
            charge_current = BatteryManager.BATTERY_PROPERTY_CURRENT_NOW;
            BatteryLevel.setText(" "+level + "%");
            progress.setProgress(level);
            BatteryCapacity.setText(battery_capacity+"mAh");
            BatteryTechnology.setText(technology);
            BatteryCurrent.setText(charge_current+"μA");
            Developers.setText("Developers\n(Naveen, Mohit) Verma");

            music_loop_thread = new Thread(new MusicLoop());
            battery_charging_thread = new Thread(new BatteryCharging());
//set progress bar color
            setProgressColor();
//calculating hr and min
            checkTime();
// battery health
            checkBatteryHealth();
//if charging or not
            if(plugged != 0){
                if((remaining_time > 3) && flag == 1)
                    BatteryStatus.setText(rem_time+" to charge the battery");
                else if((remaining_time <= 3 && remaining_time >= 0) && flag == 1)
                    BatteryStatus.setText("Battery almost charged");
                else if(flag == 1)
                    BatteryStatus.setText("Calculating time to charge the device...");
                if(plugged == 1)
                    ChargeSource.setText("AC");
                else if(plugged == 2)
                    ChargeSource.setText("USB");
                isCharging = true;
                TextSource.setTextColor(Color.BLACK);
                if(charging_time_flag == 1 && level%2 == 0 && level != 100){
                    charging_time_flag = 0;
                    Temp.setText("\nInitiating battery charging thread. count ");
                    System.out.println("Initiating battery charging thread. count ");
                    battery_charging_thread.start();
                }
                else{
                    Temp.setText("\nAttempted to initiate battery charging thread.");
                    System.out.println("Attempted to initiate battery charging thread.");
                }
            }
            else{
                charging_rate = attempts = 0;
                isCharging = false;
                ChargeSource.setText("-----");
                ChargeSource.setTextColor(Color.RED);
                BatteryStatus.setText("Discharging");
                BatteryCurrent.setText("-----");
                BatteryCurrent.setTextColor(Color.RED);
                TextCurrent.setTextColor(Color.RED);
                TextSource.setTextColor(Color.RED);
                BatteryStatus.setTextColor(Color.RED);
            }

            BatteryTemperature.setText(""+temperature+" °C");
            BatteryVoltage.setText(""+voltage+" V");
// to check if battery is charged or not
            if (flag == 1 && isCharging && level == 100){
                BatteryStatus.setText("Unplug your charger");
                BatteryStatus.setTextColor(Color.RED);
                flag = 0;
                playMusic();
                music_loop_thread.start();
            }
            else if((!isCharging) && flag == 0){
                flag = 1;
                BatteryStatus.setTextColor(Color.BLUE);
                stopMusic();
            }
        }
    };

    class MusicLoop implements Runnable {
        @Override
        public void run() {
            for (int i = flag; i < 6; i++) {
                try {
                    Thread.sleep(10000);
                    if(i == 5){
                        flag = 1;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }
    class BatteryCharging implements Runnable{
        @Override
        public void run() {
            int old_level = level;
            int time = 0;
            int old_time = dt.getHours()*60 + dt.getMinutes();
            while(level - old_level != 1){}
            time = dt.getHours()*60 + dt.getMinutes();
            charging_rate = time - old_time;
            charging_time_flag = 1;
            attempts++;
        }
    }

    //method to calculate the remaining time
    public void checkTime(){
        if(charging_rate != 0 && attempts > 1 && level != 100){
            Temp.setText("Attempts exceeded more than 1");
            Temp.setText("in CheckTime()");
            remaining_time = (100 - level)*charging_rate;
            hr = remaining_time/60;
            min = remaining_time - hr*60;
            System.out.println("Calculated remaining time "+remaining_time);
            System.out.println("\nHour = "+hr);
            System.out.println("\nMin = "+min);
            if(hr > 0){
                rem_time = hr+" hrs "+min+" mins remaining";
            }
            else{
                rem_time = min+" mins remaining";
            }
            Temp.setText("\nremaining time = "+remaining_time);
            Temp.setText("\nHour = "+hr);
            Temp.setText("\nMin = "+min);
        }
        else if(level != 100)
            remaining_time = -1;
    }
    //method to get the total capacity of the battery
    public String getBatCapacity() {
        Object mPowerProfile_ = null;
        double batteryCapacity = 0;

        final String POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile";

        try {
            mPowerProfile_ = Class.forName(POWER_PROFILE_CLASS)
                    .getConstructor(Context.class).newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            batteryCapacity = (Double) Class
                    .forName(POWER_PROFILE_CLASS)
                    .getMethod("getAveragePower", java.lang.String.class)
                    .invoke(mPowerProfile_, "battery.capacity");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            String value = Double.toString(batteryCapacity);
            String[] values = value.split("\\.");
            return values[0];
        }
    }
    //method to play the music in background
    public void playMusic(){
        mediaPlayer = MediaPlayer.create(this, R.raw.tone);
        mediaPlayer.start();
    }
    //method to stop the music in background
    public void stopMusic(){
        mediaPlayer.stop();
    }
    //method for initialising the base components of the app
    public void initComponents(Intent intent){
        level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
        technology = intent.getExtras().getString(BatteryManager.EXTRA_TECHNOLOGY);
        temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10;
        voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0)/1000;
        level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,0);

        BatteryLevel = (TextView) findViewById(R.id.Battery_Level);
        BatteryStatus = (TextView) findViewById(R.id.Battery_Status);
        ChargeSource = (TextView) findViewById(R.id.Charge_Source);
        TextSource = (TextView) findViewById(R.id.Text_Source);
        BatteryHealth = (TextView) findViewById(R.id.Battery_Health);
        BatteryVoltage = (TextView) findViewById(R.id.Battery_Voltage);
        BatteryTemperature = (TextView) findViewById(R.id.Battery_Temperature);
        BatteryCapacity = (TextView) findViewById(R.id.Battery_Capacity);
        BatteryTechnology = (TextView) findViewById(R.id.Battery_Technology);
        BatteryCurrent = (TextView) findViewById(R.id.Battery_Current);
        TextHealth = (TextView) findViewById(R.id.Text_Health);
        TextVoltage = (TextView) findViewById(R.id.Text_Voltage);
        TextTemperature = (TextView) findViewById(R.id.Text_Temperature);
        TextCapacity = (TextView) findViewById(R.id.Text_Capacity);
        TextTechnology = (TextView) findViewById(R.id.Text_Technology);
        TextCurrent = (TextView) findViewById(R.id.Text_Current);
        progress = (ProgressBar) findViewById(R.id.Progress_Bar);
        Developers = (TextView)findViewById(R.id.Developers);
        serButton = (Button) findViewById(R.id.Servie_Toggle);
        Temp = (TextView) findViewById(R.id.temp);
    }
    //method for setting the icon to the component
    public void setIcon(){
        if(Build.VERSION.SDK_INT > 16) {
            BatteryLevel.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.levelicon, 0, 0, 0);
            TextSource.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.sourceicon, 0, 0, 0);
            TextHealth.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.healthicon, 0, 0, 0);
            TextVoltage.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.voltageicon, 0, 0, 0);
            TextCurrent.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.currenticon, 0, 0, 0);
            TextTemperature.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.temperatureicon, 0, 0, 0);
            TextCapacity.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.capacityicon, 0, 0, 0);
            TextTechnology.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.technologyicon, 0, 0, 0);
        }
    }
    //method for allocating the color to the compponent
    public void setColor(){

        //getActionBar().setIcon(R.drawable.app_icon);

        BatteryLevel.setTextColor(Color.BLACK);
        BatteryStatus.setTextColor(Color.BLACK);
        ChargeSource.setTextColor(Color.BLACK);
        TextSource.setTextColor(Color.BLACK);
        BatteryHealth.setTextColor(Color.BLACK);
        BatteryVoltage.setTextColor(Color.BLACK);
        TextHealth.setTextColor(Color.BLACK);
        TextVoltage.setTextColor(Color.BLACK);
        TextTemperature.setTextColor(Color.BLACK);
        TextCapacity.setTextColor(Color.BLACK);
        BatteryCapacity.setTextColor(Color.BLACK);
        BatteryTechnology.setTextColor(Color.BLACK);
        TextTechnology.setTextColor(Color.BLACK);
        TextCurrent.setTextColor(Color.BLACK);
        BatteryCurrent.setTextColor(Color.BLACK);
        Developers.setTextColor(Color.GRAY);

        if(temperature > 40)
            BatteryTemperature.setTextColor(Color.RED);
        else
            BatteryTemperature.setTextColor(Color.BLACK);
    }
    //method to change the color of status and progress bar
    public void setProgressColor(){
        if(level <= 15){
            if(Build.VERSION.SDK_INT > 21)
                progress.setProgressTintList(ColorStateList.valueOf(Color.RED));
            BatteryStatus.setTextColor(Color.RED);
        }
        else if(level > 15 && level <= 50){
            if(Build.VERSION.SDK_INT > 21)
                progress.setProgressTintList(ColorStateList.valueOf(Color.MAGENTA));
            BatteryStatus.setTextColor(Color.MAGENTA);
        }
        else if(level > 50 && level <= 90){
            if(Build.VERSION.SDK_INT > 21)
                progress.setProgressTintList(ColorStateList.valueOf(Color.BLUE));
            BatteryStatus.setTextColor(Color.BLUE);
        }
        else{
            if(Build.VERSION.SDK_INT > 21)
                progress.setProgressTintList(ColorStateList.valueOf(Color.BLACK));
            BatteryStatus.setTextColor(Color.BLACK);
        }
    }
    //method to check the health of the battery
    public void checkBatteryHealth(){
        if(status == BatteryManager.BATTERY_HEALTH_GOOD){
            BatteryHealth.setText("Good");
            BatteryHealth.setTextColor(Color.BLUE);
        }
        else if(status == BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE){
            BatteryHealth.setText("Over Voltage");
            BatteryHealth.setTextColor(Color.RED);
        }
        else{
            BatteryHealth.setText("Ok");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Developers\n(Naveen, Mohit) Verma", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        registerReceiver(BatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
