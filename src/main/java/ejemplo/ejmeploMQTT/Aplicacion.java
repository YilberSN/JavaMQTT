package ejemplo.ejmeploMQTT;

import org.eclipse.paho.client.mqttv3.*;
import java.util.concurrent.*;

public class Aplicacion {

    public static void main(String[] args) throws Exception {

        //Configuracion de detalles
        final String serverUrl   = "tcp://192.168.0.10:1883";     /* ssl://mqtt.cumulocity.com:8883 for a secure connection */
        final String clientId    = "my_mqtt_java_client";
        final String device_name = "My Java MQTT device"; //NOMBRE DEL DISPOSITIVO
        final String tenant      = "Hummm"; //ID DEL DISPOSITIVO
        final String username    = ""; //USUARIO
        final String password    = ""; //CONTRASEÑA

        // MQTT opciones de conexion
        final MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(tenant + "/" + username);
        options.setPassword(password.toCharArray());

        //Conectar con el cliente en broker definido anteriormente
        final MqttClient client = new MqttClient(serverUrl, clientId, null);
        client.connect(options);

        //Registrar un nuevo dispositivo
        client.publish("s/us", ("100," + device_name + ",c8y_MQTTDevice").getBytes(), 2, false);

        //Colocar la información referente al hardware
        client.publish("s/us", "110,S123456789,MQTT test model,Rev0.1".getBytes(), 2, false);

        //Agregar un reinicio de la operación
        client.publish("s/us", "114,c8y_Restart".getBytes(), 2, false);

        System.out.println("The device \"" + device_name + "\" has been registered successfully!");

        // Esperar operaciones o instrucciones 
        client.subscribe("s/ds", new IMqttMessageListener() {
            public void messageArrived (final String topic, final MqttMessage message) throws Exception {
                final String payload = new String(message.getPayload());

                System.out.println("Received operation " + payload);
                if (payload.startsWith("510")) {
                    // ejecuta la operacion en otro hilo (thread) para termine de 
                	//procesar este mensaje y acuse de recibo al servidor
                    Executors.newSingleThreadScheduledExecutor().execute(new Runnable() {
                        public void run() {
                            try {
                                System.out.println("Simulating device restart...");
                                client.publish("s/us", "501,c8y_Restart".getBytes(), 2, false);
                                System.out.println("...restarting...");
                                Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                                client.publish("s/us", "503,c8y_Restart".getBytes(), 2, false);
                                System.out.println("...done...");
                            } catch (MqttException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });

        //Generar una temperatura aleatoria (10º-20º) y enviar cada 7 segundos
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(new Runnable() {
            public void run () {
                try {
                    int temp = (int) (Math.random() * 10 + 10);

                    System.out.println("Sending temperature measurement (" + temp + "º) ...");
                    client.publish("s/us", new MqttMessage(("211," + temp).getBytes()));
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }, 1, 7, TimeUnit.SECONDS);
    }
}