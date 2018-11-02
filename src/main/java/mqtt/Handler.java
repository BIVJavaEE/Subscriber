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
import entity.Measure;
import entity.Sensor;

public class Handler implements Runnable{
	
	private final BlockingQueue<String> measures = new LinkedBlockingQueue<>();
	private EntityManagerFactory factory = null;
	
	public Handler(JsonNode configuration) {
		this.factory = Persistence.createEntityManagerFactory(configuration.get("name").asText());
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
		EntityManager manager = this.factory.createEntityManager();
		try {
			manager.getTransaction().begin();
			Sensor sensor = manager.find(Sensor.class, sensorID);
			if(sensor != null) {
				measure.setSensor(sensor);
				manager.persist(measure);
				manager.getTransaction().commit();			
			}else {
				manager.close();
			}
		}catch(IllegalArgumentException | TransactionRequiredException e) {
			manager.getTransaction().rollback();
			throw e;
		}catch(IllegalStateException e) {
			System.out.println("Can't insert data can't use transaction on JTA entity manager: " + e.getMessage());
			throw e;
		}finally {
			manager.close();
		}
	}
	
}
