package mqtt;


import javax.persistence.PersistenceException;
import javax.persistence.TransactionRequiredException;

import org.eclipse.paho.client.mqttv3.*;

import com.fasterxml.jackson.databind.JsonNode;

import mapping.MapperFactoryException;

public class Subscriber implements MqttCallback {
	
	// JSON parser to unserialize objects
	private Handler handler;
	
	private IMqttClient client;
	

	public Subscriber(JsonNode configuration) {
		this.handler = new Handler(configuration);
	}
	
	public void connect(MqttConnectOptions options, String uri, String topic, String uuid, int qos) throws MqttException, MapperFactoryException {
		this.client = new MqttClient(uri, uuid);
		this.client.setCallback(this);
		this.client.connect(options);	    
        this.client.subscribe(topic, qos);
        this.handler.initialize();
        Thread thread = new Thread(this.handler);
        thread.start();
	}
	
    public void connectionLost(Throwable cause) {
    	System.out.println("Connection lost because of an unhandled error: ");
    	cause.printStackTrace();
    }

    public void messageArrived(String topic, MqttMessage message) throws MqttException {
	    try {
	    	System.out.println("Data received: " + message.toString());
	    	this.handler.handleMeasure(message.toString());
	    }catch (IllegalArgumentException | TransactionRequiredException e) {
			System.out.println("Ignoring last sensor data: " + message.toString());
		}catch (PersistenceException e) {
			System.out.println("Ignoring data that not respect model and database constraints. Reasons: " + e.getMessage());
		}catch (IllegalStateException e) { // If database state is invalid, stop the listening of sensors
			e.printStackTrace();
		}
    }

	public void deliveryComplete(IMqttDeliveryToken token) {
		// Do nothing, this class is only a receiver
	}
	
}