package cody.bleremote;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {
    static volatile BluetoothAdapter btAdapter;
    static volatile BluetoothGattCharacteristic control;
    static volatile BluetoothGattCharacteristic battery;
    static volatile BluetoothGatt btGatt;

    SensorManager mSensorManager;
    Sensor mSensor;

    static volatile float angle = 0;

    static volatile boolean left = false;
    static volatile boolean right = false;
    static volatile boolean forward = false;
    static volatile boolean backward = false;
    static volatile boolean top_light = false;
    static volatile byte front_light = 0;
    static volatile boolean blink_left = false;
    static volatile boolean blink_right = false;
    static volatile boolean beep = false;


    MainActivity x = this;
    static BluetoothDevice remoteDevice;

    public static boolean setBluetooth(BluetoothAdapter bluetoothAdapter, boolean enable) {
        boolean isEnabled = bluetoothAdapter.isEnabled();
        if (enable && !isEnabled) {
            return bluetoothAdapter.enable();
        }
        else if(!enable && isEnabled) {
            return bluetoothAdapter.disable();
        }
        // No need to change bluetooth state
        return true;
    }

    int sendtick = 0;

    void Send()
    {
        sendtick++;
        if(control != null)
        {
            byte[] msg = new byte[7];

            if(forward == backward) {
                msg[1] = 0;
            }
            else if(forward) {
                msg[1] = 50;
            }
            else if(backward) {
                msg[1] = -40;
            }

            if(angle==0) {
                if (right == left) {
                    msg[0] = -20;
                } else if (right) {
                    msg[0] = 70;
                } else if (left) {
                    msg[0] = -100;
                }
            }
            else {

                float max = 70;
                float min = -100;
                float mid = (max + min) / 2;
                float range = (max - min) / 2;

                msg[0] = (byte) ((angle / 90) * range + mid);
            }

            msg[6] = (byte) (beep ? 1 : 0);
            msg[3] = (byte) (top_light ? 1 : 0);
            msg[2] = front_light;
            msg[4] = (byte)(blink_left ? 1 : 0);
            msg[5] = (byte)(blink_right ? 1 : 0);

            control.setValue(msg);
            control.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            btGatt.writeCharacteristic(control);

        }

        if((battery != null) && (sendtick % 5 == 0)){
            btGatt.readCharacteristic(battery);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        View v = findViewById(android.R.id.content);
        v.setOnTouchListener(new View.OnTouchListener() {
                                 @Override
                                 public boolean onTouch(View v, MotionEvent event) {
                                     TextView tv1 = (TextView)findViewById(R.id.textView2);
                                     tv1.setText(Float.toString(event.getX()));

                                     if(event.getAction() == MotionEvent.ACTION_UP){

                                         // Do what you want
                                         return true;
                                     }
                                     return true;
                                 }
                             });
                             */
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                switch(event.sensor.getType()) {
                    //case Sensor.TYPE_ACCELEROMETER:
                    //    float[] acc = event.values;
                    //    Log.d("bleremote", "acc:" + Float.toString(acc[0]) + " " + Float.toString(acc[1]) + " " + Float.toString(acc[2]));
                    //    break;
                    case Sensor.TYPE_GYROSCOPE:
                        float[] gyro = event.values;
                        //Log.d("bleremote", "gyro:" + Float.toString(gyro[0]) + " " + Float.toString(gyro[1]) + " " + Float.toString(gyro[2]));
                        Log.d("bleremote", "gyro:" + Float.toString(gyro[2]));
                        break;
                }
                /*float[] rotationMatrix=new float[16];
                mSensorManager.getRotationMatrixFromVector(rotationMatrix,event.values);

                float[] orientationValues = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientationValues);
                double azimuth = Math.toDegrees(orientationValues[0]);
                double pitch = Math.toDegrees(orientationValues[1]);
                float roll = (float)Math.toDegrees(orientationValues[2]);*/
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        },mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);

        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                switch(event.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        float[] acc = event.values;
                        float a = (float)Math.toDegrees(Math.atan2(acc[0],acc[1]));
                        if(a <= -90)
                            a = 180;
                        else if(a < 0)
                            a = 0;

                        if(acc[2] > 5)
                            angle = 0;
                        else
                            angle = -(a - 90);
                        //Log.d("bleremote", "acc:" + Float.toString(acc[0]) + " " + Float.toString(acc[1]) + " " + Float.toString(acc[2]));
                        //Log.d("bleremote", "acc:" + Float.toString(acc[0]) + " " + Float.toString(acc[1]));
                        Log.d("bleremote", "angle: " + Float.toString(angle));
                        break;
                    //case Sensor.TYPE_GYROSCOPE:
                    //    float[] gyro = event.values;
                    //    Log.d("bleremote", "gyro:" + Float.toString(gyro[0]) + " " + Float.toString(gyro[1]) + " " + Float.toString(gyro[2]));
                    //    break;
                }
                /*float[] rotationMatrix=new float[16];
                mSensorManager.getRotationMatrixFromVector(rotationMatrix,event.values);

                float[] orientationValues = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientationValues);
                double azimuth = Math.toDegrees(orientationValues[0]);
                double pitch = Math.toDegrees(orientationValues[1]);
                float roll = (float)Math.toDegrees(orientationValues[2]);*/
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        },mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

        BluetoothManager btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);

        btAdapter = btManager.getAdapter();

        if(!btAdapter.isEnabled()) {
            if(!btAdapter.enable())
                throw new RuntimeException("bluetooth problem");
            //while(!btAdapter.isEnabled()) {
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            //}
        }

        remoteDevice = btAdapter.getRemoteDevice("F2:24:4D:E3:42:0F");

        if(remoteDevice == null) {
            if (!btAdapter.startLeScan(leScanCallback))
                throw new RuntimeException("lescan");
            setState("Scanning");
        }
        else
        {
            setState("Connecting");
            btGatt = remoteDevice.connectGatt(x, false, btleGattCallback);
        }

        TimerTask timer3= new TimerTask(){
            @Override
            public void run() {
                Send();
            }

        };

        Timer t = new Timer();
        t.scheduleAtFixedRate(timer3, 0, 200);

        Button b = (Button)findViewById(R.id.button_left);
        b.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    left = false;
                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    left = true;
                }
                //Send();
                return true;
            }
        });
        b = (Button)findViewById(R.id.button_right);
        b.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction() == MotionEvent.ACTION_UP){
                    right = false;
                }
                else if(event.getAction() == MotionEvent.ACTION_DOWN){
                    right = true;
                }
                //Send();
                return true;
            }
        });

        b = (Button)findViewById(R.id.button_forward);
        b.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction() == MotionEvent.ACTION_UP){
                    forward = false;
                }
                else if(event.getAction() == MotionEvent.ACTION_DOWN){
                    forward = true;
                }
                //Send();
                return true;
            }
        });

        b = (Button)findViewById(R.id.button_backward);
        b.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction() == MotionEvent.ACTION_UP){
                    backward = false;
                }
                else if(event.getAction() == MotionEvent.ACTION_DOWN){
                    backward = true;
                }
                //Send();
                return true;
            }
        });

        b = (Button)findViewById(R.id.beep);
        b.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    beep = false;
                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    beep = true;
                }
                //Send();
                return true;
            }
        });

        ToggleButton tb = (ToggleButton)findViewById(R.id.blink_left);
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                blink_left = isChecked;
                //Send();
            }
        });
        tb = (ToggleButton)findViewById(R.id.blink_right);
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                blink_right = isChecked;
                //Send();
            }
        });

        final Switch sw = (Switch)findViewById(R.id.blue);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                top_light = isChecked;
                //Send();
            }
        });
        final Switch sw2 = (Switch)findViewById(R.id.light);
        sw2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                front_light = (byte) (isChecked ? 255 : 0);
                //Send();
            }
        });
        //btAdapter.stopLeScan(leScanCallback);
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            // your implementation here
            String mac = device.getAddress();
            if(mac.compareTo("D1:E4:D4:1E:12:F2") == 0 ||
                    mac.compareTo("F2:24:4D:E3:42:0F") == 0) {
                remoteDevice = device;
                btAdapter.stopLeScan(leScanCallback);
                setState("Connecting");
                btGatt = device.connectGatt(x, false, btleGattCallback);
            }
        }
    };

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //if(status != BluetoothGatt.GATT_SUCCESS)
            //    logMachineResponse(characteristic, status);

            //mWaitingCommandResponse = false;
            //dequeCommand();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
        }

        @Override
        public void onCharacteristicRead (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            byte[] value = battery.getValue();
            if(value.length == 2) {
                setBattery(((float)((int)value[0] + (int)value[1] * 256)) / 1000);
            }

        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            if(remoteDevice != null && gatt.getDevice().getAddress() == remoteDevice.getAddress()) {
                // this will get called when a device connects or disconnects
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    setState("Discover Services");
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    setState("Scanning");
                    btAdapter.startLeScan(leScanCallback);
                    control = null;
                }
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            if(!gatt.equals(btGatt))
                return;
            // this will get called after the client initiates a BluetoothGatt.discoverServices() call
            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService service : services) {
                if(service.getUuid().compareTo(UUID.fromString("0000a000-0000-1000-8000-00805F9B34FB")) == 0) {
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic c : characteristics) {
                        if(c.getUuid().compareTo(UUID.fromString("0000a010-0000-1000-8000-00805F9B34FB"))==0){
                            setState("Connected");
                            control = c;
                            //return;
                        }
                        else if(c.getUuid().compareTo(UUID.fromString("0000a001-0000-1000-8000-00805F9B34FB"))==0) {
                            battery = c;
                            //return;
                        }
                    }
                }
            }

            if(control == null)
            {
                throw new RuntimeException("control characteristic missing");
            }
        }
    };

    public void setState(final String state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView)findViewById(R.id.textView2)).setText(state);
            }
        });

    }

    public void setBattery(final float volt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView)findViewById(R.id.textView)).setText(Float.toString(volt) + "mV");
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        else if(id == R.id.action_quit) {
            if(btGatt!=null) {
                btGatt.disconnect();
                btGatt.close();
            }
            finish();
            System.exit(0);
        }

        return super.onOptionsItemSelected(item);
    }
}
