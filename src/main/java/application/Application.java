package application;

import java.nio.file.Paths;
import java.util.UUID;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import mqtt.Subscriber;

public class Application {
	
	public static final String DB_NAME = "bivjee";
	public static Configuration configuration;
	
	private JsonNode initConfiguration(String path) throws ConfigurationException {
		configuration = new Configuration(path);
        
        JsonNode config = configuration.read();
        
        if(config == null) {
        	System.out.println("Can't find the configuration file at: " + configuration.getPath());

        	System.out.println("Creating configuration file on path: " + Paths.get(configuration.getPath()).toAbsolutePath().toString());
        	ObjectNode parameters = new ObjectNode(JsonNodeFactory.instance);
        	parameters.put("logging", true);
        	parameters.putObject(("database"))
        	.put("driver", "org.mariadb.jdbc.driver")
        	.put("hibernate_dialect", "org.hibernate.dialect.MySQL5Dialect")
        	.put("uri", "localhost")
        	.put("user", "")
        	.put("password", "");
        	parameters.putObject("subscriber")
        	.put("uri", "localhost")
        	.put("qos", 1)
        	.put("autoReconnect", true)
        	.put("cleanSession", false)
        	.put("topic", "/")
        	.put("uuid", "")
        	.put("connexionTimeout", "60");
        	configuration.save(parameters);
        	System.out.println("Configuration created, please complete it.");
        }
		return config;
	}
	
	private MqttConnectOptions initMQTT(JsonNode subscriberConfiguration) {
		MqttConnectOptions options = new MqttConnectOptions();
	    options.setAutomaticReconnect(subscriberConfiguration.get("autoReconnect").asBoolean());
	    options.setCleanSession(subscriberConfiguration.get("cleanSession").asBoolean());
	    options.setConnectionTimeout(subscriberConfiguration.get("connexionTimeout").asInt());
	    return options;
	}
	
	private void initLogger(JsonNode loggerConfiguration) {
		FileAppender appender = new FileAppender();
		appender.setName("file");
		appender.setFile(loggerConfiguration.get("path").asText());
		appender.setLayout(new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n"));
		appender.setAppend(true);
		appender.activateOptions();
		Logger.getRootLogger().addAppender(appender);
		if(loggerConfiguration.get("active").asBoolean()) {
			Logger.getRootLogger().setLevel(Level.INFO);
		}else {
			Logger.getRootLogger().setLevel(Level.OFF);
		}
	}
	
	private void startSubscriber(JsonNode config) throws ConfigurationException {
        JsonNode database = config.get("database");
        JsonNode loggerConfiguration = config.get("logger");
        JsonNode subscriberConfiguration = config.get("subscriber");
		
		String uri = subscriberConfiguration.get("uri").asText();
		String topic = subscriberConfiguration.get("topic").asText();
		String uuid = subscriberConfiguration.get("uuid").asText();
		int qos = subscriberConfiguration.get("qos").asInt();
		if(uuid.equals("")) {
			uuid = UUID.randomUUID().toString();
			((ObjectNode) subscriberConfiguration).put("uuid", uuid);
			configuration.save();
		}
		
		MqttConnectOptions options = initMQTT(subscriberConfiguration);

		initLogger(loggerConfiguration);
		
		Subscriber subscriber = new Subscriber(database);
	    try {
			subscriber.connect(options, uri, topic, uuid, qos);
		} catch (MqttException e) {
			System.out.println("Connection to message broker failed - reason is: " + e.getMessage());
		}
	}
	
	public static void main(String[] args) throws Exception {
		Application application = new Application();
		JsonNode config = application.initConfiguration("config/config.json");
        if(config == null) {
        	System.exit(0); // If the system can't recognize the configuration stop it.
        }
        application.startSubscriber(config);
        	
	}
}
