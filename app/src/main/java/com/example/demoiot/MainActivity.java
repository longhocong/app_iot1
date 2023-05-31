package com.example.demoiot;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.angads25.toggle.interfaces.OnToggledListener;
import com.github.angads25.toggle.model.ToggleableView;
import com.github.angads25.toggle.widget.LabeledSwitch;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private CountDownLatch ackReceived;
    private int ackTimeout = 4000;
    MQTTHelper mqttHelper;
    TextView txtTemp,txtHumi;
    LabeledSwitch btLED, btPUMP;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        txtTemp = findViewById(R.id.txttemp);
        txtHumi = findViewById(R.id.txtHumidity);
        btLED = findViewById(R.id.btLED);
        btPUMP= findViewById(R.id.btPUMP);

        btLED.setOnToggledListener(new OnToggledListener() {
                                       @Override
                                       public void onSwitched(ToggleableView toggleableView, boolean isOn) {
                                           if (isOn == true) {
                                               sendDataMQTT("holong/feeds/nutnhan1","1","holong/feeds/ack", "11");


                                           } else { sendDataMQTT("holong/feeds/nutnhan1","0","holong/feeds/ack","10");
                                           }
                                       }

                                   });
        btPUMP.setOnToggledListener(new OnToggledListener() {
            @Override
            public void onSwitched(ToggleableView toggleableView, boolean isOn) {
                if (isOn == true) {
                    sendDataMQTT("holong/feeds/nutnhan2","1","holong/feeds/ack2","21");


                } else { sendDataMQTT("holong/feeds/nutnhan2","0","holong/feeds/ack2","20");
                }
            }
        });
        starMQTT();
    }
//    public void sendDataMQTT(String topic, String value){
//        MqttMessage msg = new MqttMessage();
//        msg.setId(1234);
//        msg.setQos(0);
//        msg.setRetained(false);
//
//        byte[] b = value.getBytes(Charset.forName("UTF-8"));
//        msg.setPayload(b);
//
//        try {
//            mqttHelper.mqttAndroidClient.publish(topic, msg);
//        }catch (MqttException e){
//        }
//
//           }

    public void sendDataMQTT(String topic, String data, String topic_ack, String type) {
        MqttMessage message = new MqttMessage();
        message.setPayload(data.getBytes());
        try {
            mqttHelper.mqttAndroidClient.publish(topic, message);
            Log.d("TEST", "Send success " + topic + ": " + data);

            // Tạo một thread mới để chờ đợi ack
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // Khởi tạo biến đếm thời gian
                    CountDownLatch ackReceived = new CountDownLatch(1);

                    // Đăng ký bộ lắng nghe cho topic ack
                    try {
                        mqttHelper.mqttAndroidClient.subscribe(topic_ack, 2, new IMqttMessageListener() {
                            @Override
                            public void messageArrived(String topic, MqttMessage message) throws Exception {
                                // Nhận được ack, giải phóng biến đếm thời gian
                                if (message.toString().equals(type))
                                    ackReceived.countDown();
                                Log.d("TEST", "Ack received: " + message.toString());
                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

                    // Chờ ack trong khoảng thời gian được cho phép
                    try {
                        if (!ackReceived.await(ackTimeout, TimeUnit.MILLISECONDS)) {
                            // Quá thời gian chờ, tắt công tắc và in ra thông báo
                            if (type=="11")
                                btLED.setOn(false);
                            else if (type=="10")
                                btLED.setOn(true);
                            else if (type=="21")
                                btPUMP.setOn(false);
                            else
                                btLED.setOn(true);

                            Log.d("TEST", "Timeout waiting for ack");
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (MqttException e) {
            Log.d("TEST", "Send failed: " + e.toString());
        }
    }


    public void starMQTT(){
        mqttHelper = new MQTTHelper(this);
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.d("TEST", topic + "***" + mqttMessage.toString());
                if(topic.contains("cambien1")){
                    txtTemp.setText(mqttMessage.toString()+"°C");
                } else if (topic.contains("cambien2")) {
                    txtHumi.setText(mqttMessage.toString()+" lux");

                } else if (topic.contains("nutnhan1")) { if (mqttMessage.toString().equals("1")) {
                    btLED.setOn(true);
                } else {
                    btLED.setOn(false);
                }
                    
                }else if (topic.contains("nutnhan2")) { if (mqttMessage.toString().equals("1")) {
                    btPUMP.setOn(true);
                } else {
                    btPUMP.setOn(false);
                }

                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }


}