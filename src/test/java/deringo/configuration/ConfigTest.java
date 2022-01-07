package deringo.configuration;

public class ConfigTest {

	public static void main(String[] args) {
		System.out.println(Config.getAllProperties());
		
		System.setProperty("config.includeSystemEnvironmentAndProperties", "true");
		System.out.println(Config.getAllProperties());
		
		Config.reinit();
		System.out.println(Config.getAllProperties());
	}
}
