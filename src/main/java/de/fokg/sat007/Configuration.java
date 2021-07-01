package de.fokg.sat007;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Configuration {
	
	Properties prop = new Properties();
	FileInputStream input;
	
	public Configuration() throws IOException{
		input = new FileInputStream("config.properties");
		prop.load(input);
	}
	
	public String getPropertyValue( String key) {
		String value = "";
		
		value = prop.getProperty(key);
		
		return value;
	}
	

	
}

