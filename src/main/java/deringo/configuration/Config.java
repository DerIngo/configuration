package deringo.configuration;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementierung folgt folgender Logik:
<ul>
<li>Properties werden aus src/main/resources/application.properties geladen</li>
<li>Properties werden aus der Datei geladen, auf die die Property "localconf" zeigt, falls gesetzt.
  <ul>
	<li>localconf kann über System Environment gesetzt werden oder</li>
	<li>localconf kann über System Properties gesetzt werden</li>
	<li>der Properties Eintrag würde ggf. gegenüber Environment Eintrag bevorzugt</li>
  </ul>
</li>
<li>Properties werden aus System Environment gezogen</li>
<li>Properties werden aus System Properties gezogen</li>
<li>Die Einträge werden in obiger Reihenfolge überschrieben.</li>
<li>Über die Property "config.includeSystemEnvironmentAndProperties" kann gesteuert werden,
  <ul>
    <li>ob alle System Environment und Properties in die Conf Properties übertragen werden (mit  config.includeSystemEnvironmentAndProperties=true) oder</li>
    <li>ob nur vorhandene Properties überschrieben werden sollen (config.includeSystemEnvironmentAndProperties=false)</li>
  </ul>
</li>
 *  
 *  @author Ingo Kaulbach
 *
 */
public class Config {
	private static Logger logger = LoggerFactory.getLogger(Config.class.getName());
	
	private static String basePropertiesFilename = "application.properties";
	private static String localPropertiesProperty = "localconf";
	private static String includeSystemEnvironmentAndPropertiesProperty = "config.includeSystemEnvironmentAndProperties";
	
	private static Properties properties = null;

	static {
		init();
	}
	
	// BaseProperties:  application.properties
	// LocalProperties: aus System Environment File laden
	// LocalProperties: aus System Properties File laden
	// LocalProperties: aus System Environment direkt
	// LocalProperties: aus System Properties direkt
	
	// config.includeSystemEnvironmentAndProperties -> SystemEnv/Props in properties hinzufügen, falls sie NICHT in den anderen Properties drin sind

	
	public static String get(String key) {
		return properties.getProperty(key);
	}
	
	public static boolean is(String key) {
		return BooleanUtils.toBoolean(get(key));
	}
	
	public static Properties getAllProperties() {
		return (Properties)properties.clone();
	}
	
	public static void reinit() {
		logger.info("Config reinit requested");
		init();
	}
	
	private static void init() {
		
		// Get HostName
		String hostname = "unkown Host";
		try {
			hostname = java.net.InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			logger.error("Could not read HostName", e);
		}
		logger.debug("----------------------------------------------------------------------------------");
		logger.debug(new Date(System.currentTimeMillis()).toString());
		logger.debug("HostName: {}", hostname);
		logger.debug("----------------------------------------------------------------------------------");

		
		// Get System Environment
		Properties systemEnvironmentProperties = new Properties();		
		systemEnvironmentProperties.putAll(System.getenv());
		logger.debug("----------------------------------------------------------------------------------");
		logger.debug("System Environment:");
		systemEnvironmentProperties.forEach((key, value) -> logger.debug("{} : {}", key, value));
		logger.debug("----------------------------------------------------------------------------------");

		
		// Get System Properties
		Properties systemPropertiesProperties = new Properties();		
		systemPropertiesProperties.putAll(System.getProperties());
		logger.debug("----------------------------------------------------------------------------------");
		logger.debug("System Properties:");
		systemPropertiesProperties.forEach((key, value) -> logger.debug("{} : {}", key, value));
		logger.debug("----------------------------------------------------------------------------------");

		
		// Load BaseProperties
		Properties baseProperties = new Properties();
		try {
			InputStream is = Config.class.getClassLoader().getResourceAsStream(basePropertiesFilename);
			baseProperties.load(is);
		} catch (Exception e) {
			logger.error("Could not read {} from ClassLoader", basePropertiesFilename, e);
		}
		logger.debug("----------------------------------------------------------------------------------");
		logger.debug("BaseProperties from {}:", basePropertiesFilename);
		baseProperties.forEach((key, value) -> logger.debug("{} : {}", key, value));
		logger.debug("----------------------------------------------------------------------------------");

		
		// Load LocalProperties
		Properties localProperties = new Properties();
		logger.debug("----------------------------------------------------------------------------------");
		logger.debug("LocalProperties Path from System Environment: {}", systemEnvironmentProperties.getProperty(localPropertiesProperty));
		logger.debug("LocalProperties Path from System Properties: {}", systemPropertiesProperties.getProperty(localPropertiesProperty));
		String localPropertiesPath = systemPropertiesProperties.getProperty(localPropertiesProperty) != null ? systemPropertiesProperties.getProperty(localPropertiesProperty) : systemEnvironmentProperties.getProperty(localPropertiesProperty);
		if (localPropertiesPath == null) {
			logger.debug("LocalProperties Path is not set, skip loading Local Properties");
		} else {
			logger.debug("Load LocalProperties from {}", localPropertiesPath);
			try {
				localProperties.load(new FileInputStream(localPropertiesPath));
			} catch (Exception e) {
				logger.error("Could not read {} from File", localPropertiesPath, e);
			}
		}
		logger.debug("LocalProperties:");
		localProperties.forEach((key, value) -> logger.debug("{} : {}", key, value));
		logger.debug("----------------------------------------------------------------------------------");
		
		
		// includeSystemEnvironmentAndProperties?
		String includeS = Stream.of(
				systemPropertiesProperties.getProperty(includeSystemEnvironmentAndPropertiesProperty),
				systemEnvironmentProperties.getProperty(includeSystemEnvironmentAndPropertiesProperty),
				localProperties.getProperty(includeSystemEnvironmentAndPropertiesProperty),
				baseProperties.getProperty(includeSystemEnvironmentAndPropertiesProperty))
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);		
		Boolean include = Boolean.parseBoolean(includeS);
		logger.debug("includeSystemEnvironmentAndProperties: {}", include);
		
		
		// Merge Properties
		Properties mergedProperties = new Properties();
		mergedProperties.putAll(baseProperties);
		mergedProperties.putAll(localProperties);
		if (include) {
			mergedProperties.putAll(systemEnvironmentProperties);
			mergedProperties.putAll(systemPropertiesProperties);
		} else {
			mergedProperties.forEach((key, value) -> {
				value = systemEnvironmentProperties.getProperty((String)key, (String)value);
				value =  systemPropertiesProperties.getProperty((String)key, (String)value);
				mergedProperties.setProperty((String)key, (String)value);
			});
		}
		logger.debug("----------------------------------------------------------------------------------");
		logger.debug("Merged Properties:");
		mergedProperties.forEach((key, value) -> logger.debug("{} : {}", key, value));
		logger.debug("----------------------------------------------------------------------------------");
		
		properties = mergedProperties;
		logger.info("Config init completed");
	}
}
