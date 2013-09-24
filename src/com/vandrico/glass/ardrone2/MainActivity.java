/*
 *
  Copyright (C) <2013>, <Vandrico Solutions>
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
The names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.vandrico.glass.ardrone2;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import de.yadrone.base.ARDrone;
import de.yadrone.base.command.CommandManager;
import de.yadrone.base.command.LEDAnimation;
import de.yadrone.base.navdata.AttitudeListener;
import de.yadrone.base.navdata.BatteryListener;

class MyARDrone extends ARDrone implements BatteryListener, AttitudeListener {

    public float yaw, pitch, roll;
    public int iBatteryLv;

    MyARDrone(String ip, Object object) {
        super(ip, null);
        yaw = pitch = roll = 0;

        getNavDataManager().addBatteryListener(this);
        getNavDataManager().addAttitudeListener(this);
    }

    public void batteryLevelChanged(int i) {

        iBatteryLv = i;
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void voltageChanged(int i) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void attitudeUpdated(float pitch, float roll, float yaw) {
        this.yaw = yaw;
        this.roll = roll;
        this.pitch = pitch;
    }

    public void attitudeUpdated(float pitch, float roll) {
    }

    public void windCompensation(float pitch, float roll) {
    }
}

public class MainActivity extends Activity implements SensorEventListener {

    SensorManager sm;
    TextView status, tvDevice, tvDrone, log;
    Button fly;
    float mRotationMatrix[];
    float mOrientation[];
    float mRotation[];
    float yaw, pitch, roll;
    MyARDrone drone;
    boolean isFlying = false;
    CommandManager cmdMgr;
    private boolean isMoving = false;
    Handler handler;

    public void calculateInput() {
        SensorManager.getRotationMatrixFromVector(mRotationMatrix, mRotation);
        SensorManager.remapCoordinateSystem(mRotationMatrix,
                SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationMatrix);
        SensorManager.getOrientation(mRotationMatrix, mOrientation);

        yaw = (float) Math.toDegrees(mOrientation[0]);
        pitch = (float) Math.toDegrees(mOrientation[1]);
        roll = (float) Math.toDegrees(mOrientation[2]);

        tvDrone.setText(String.format("Drone\nyaw:%f\npitch:%f\nroll:%f\nBattery:%d", drone.yaw, drone.pitch, drone.roll, drone.iBatteryLv));
        tvDevice.setText(String.format("Device\nyaw:%f\npitch:%f\nroll:%f\n", yaw, pitch, roll));


        // we are not flying
        if (!isFlying) {
            //handler.postDelayed(this, 200);
            return;
        }

        if (Math.abs(roll) > 20 && Math.abs(roll) < 40) {
            if (roll < 0) {
                //cmdMgr.setLedsAnimation(LEDAnimation.LEFT_GREEN_RIGHT_RED, 3, 1);
                //cmdMgr.goLeft((int) (Math.abs(roll / 60) * 20));
                cmdMgr.goLeft(20);
                //cmdMgr.spinLeft((int) Math.abs(roll / 90) * 100);

                status.setText("Go Left");

            } else if (roll > 0) {
                //cmdMgr.setLedsAnimation(LEDAnimation.LEFT_RED_RIGHT_GREEN, 3, 1);
                //cmdMgr.goRight((int) (Math.abs(roll / 60) * 20));
                cmdMgr.goRight(20);
                //cmdMgr.spinRight((int) Math.abs(roll / 90) * 100);

                status.setText("Go Right");
            }

            isMoving = true;
        } else if (Math.abs(pitch) > 20 && Math.abs(pitch) < 40) {
            if (pitch > 0) {
                //cmdMgr.setLedsAnimation(LEDAnimation.BLINK_ORANGE, 3, 1);
                cmdMgr.forward((int) (Math.abs(pitch / 60) * 20));
                //cmdMgr.forward(20);

                status.setText("Forward");

            } else if (pitch < 0) {
                //cmdMgr.setLedsAnimation(LEDAnimation.BLINK_RED, 3, 1);
                cmdMgr.backward((int) (Math.abs(pitch / 60) * 20));
                //cmdMgr.backward(20);

                status.setText("Backward");
            }

            isMoving = true;
        } else if (isMoving) {
            cmdMgr.hover();
            isMoving = false;
        }

        if (!isMoving) {
            status.setText("Hovering");
        }

        //handler.postDelayed(this, 100);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);

        status = (TextView) findViewById(R.id.status);
        tvDevice = (TextView) findViewById(R.id.tvDevice);
        tvDrone = (TextView) findViewById(R.id.tvDrone);
        log = (TextView) findViewById(R.id.log);
        fly = (Button) findViewById(R.id.btnFly);

        // init values
        mRotationMatrix = new float[16];
        mOrientation = new float[3];
        mRotation = new float[3];
        yaw = pitch = roll = 0;

    }

    @Override
    protected void onResume() {
        super.onResume(); //To change body of generated methods, choose Tools | Templates.
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_UI);

        // connect to drone
        try {
            //drone = new ARDrone("192.168.10.99");
            drone = new MyARDrone("192.168.0.1", null);
            log.append("\n\nInitialize the drone ...\n");
            drone.start();
            log.append("\n\nDONE.\n");

            cmdMgr = drone.getCommandManager();

            fly.setEnabled(true);

        } catch (Exception exc) {

            fly.setEnabled(false);
            exc.printStackTrace();

            if (drone != null) {
                drone.stop();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause(); //To change body of generated methods, choose Tools | Templates.

        //handler.removeCallbacks(r);
        sm.unregisterListener(this);

        if (isFlying) {
            drone.landing();
        }

        drone.stop();


    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {

            System.arraycopy(event.values, 0, mRotation, 0, 3);

            calculateInput();
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void takeoff(View v) {

        cmdMgr.flatTrim();
        cmdMgr.setLedsAnimation(LEDAnimation.BLINK_ORANGE, 3, 1);
        try {
            drone.takeOff();
            Thread.sleep(1000);
        } catch (Exception e) {
        }

        isFlying = true;
    }

    public void land(View v) {
        drone.landing();

        cmdMgr.setLedsAnimation(LEDAnimation.BLINK_GREEN, 3, 1);


        isFlying = false;
    }

    public void emergency(View v) {
        drone.reset();
        isFlying = false;
    }

    public void forward(View v) {
        cmdMgr.forward(20);
    }

    public void backward(View v) {
        cmdMgr.backward(20);
    }

    public void left(View v) {
        cmdMgr.goLeft(20);
    }

    public void right(View v) {
        cmdMgr.goRight(20);
    }
}
