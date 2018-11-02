package mqtt;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TransactionRequiredException;

import com.fasterxml.jackson.databind.JsonNode;

import application.Main;
import entity.Measure;
import entity.Sensor;

public class Handler implements Runnable{
	
	private final BlockingQueue<String> measures = new LinkedBlockingQueue<>();
	private EntityManager manager = null;
	
	public Handler(JsonNode configuration) {
		Map<String, String> properties = new HashMap<>();
		properties.put("javax.persistence.jdbc.driver", configuration.get("driver").asText());
		properties.put("hibernate.dialect", configuration.get("hibernate_dialect").asText());
		properties.put("javax.persistence.jdbc.url", configuration.get("uri").asText());
		properties.put("javax.persistence.jdbc.user", configuration.get("user").asText());
		properties.put("javax.persistence.jdbc.password", configuration.get("password").asText());
		EntityManagerFactory factory = Persistence.createEntityManagerFactory(Main.DB_NAME, properties);
		this.manager = factory.createEntityManager();
	}
	
	@Override
	public void run() {
		while(true) {
			String data;
			Measure measure;
			try {
				data = measures.take();
				if(data != null && !data.equals("")) {
					String[] measureData = data.split(";");		
					measure = parse(measureData);
					save(measure, Long.parseLong(measureData[0]));
					System.out.println("Data saved: " + data);
					
				}
			} catch (InterruptedException e) {
				System.out.println("Error while handling measure: ");
				e.printStackTrace();
			}
		}
	}
	
	public synchronized void handleMeasure(String measure) {
		try {
			measures.put(measure);
		} catch (InterruptedException e) {
			System.out.println("Impossible to add " + measure + " to the handler queue for reason: ");
			e.printStackTrace();
		}
	}
	
	private Measure parse(String[] measure){
		Measure res = new Measure();
		res.setValue(Double.parseDouble(measure[1]));
		res.setTimestamp(new Timestamp(new Date().getTime()));
		return res;
	}
	
	private void save(Measure measure, Long sensorID) {
		try {
			Sensor sensor = manager.find(Sensor.class, sensorID);
			if(sensor != null) {
				measure.setSensor(sensor);
				manager.getTransaction().begin();
				manager.persist(measure);
				manager.getTransaction().commit();			
			}
		}catch(IllegalArgumentException | TransactionRequiredException e) {
			manager.getTransaction().rollback();
			throw e;
		}catch(IllegalStateException e) {
			System.out.println("Can't insert data can't use transaction on JTA entity manager: " + e.getMessage());
			throw e;
		}
	}
	
}
