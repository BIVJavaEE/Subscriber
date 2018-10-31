package application;
import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Configuration {
	
	private ObjectMapper mapper;
	
	private ObjectNode node;

	private String path;
	
	public Configuration(String path) {
		this.mapper = new ObjectMapper();
		this.path = path;
		this.node = null;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public ObjectNode read() throws ConfigurationException{
		if(this.node == null) {
			try {
				File file = new File(getPath());
				if(file.exists()) {
					this.node = (ObjectNode) mapper.readTree(file);					
				}
			}catch(IOException e) {
				throw new ConfigurationException("Can't load configuration : " + e.getMessage());
			}
		}
		return this.node;
	}
	
	public void save(ObjectNode node) throws ConfigurationException {
		this.node = node;
		this.save();
	}
	
	public void save() throws ConfigurationException {
		ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
		try {
			File file = new File(getPath());
			file.getParentFile().mkdirs();
			file.createNewFile();
			writer.writeValue(file, this.node);
		}catch(IOException e) {
			throw new ConfigurationException("Can't save configuration : " + e.getMessage());
		}
	}
}
