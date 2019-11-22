import static spark.Spark.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class Main {
	public static void main(String[] args) {
		List<String> argsList = Arrays.asList(args);
		// set port
		port(getProp("heroku.port", Integer::parseInt, 8181));

		if(argsList.contains("--cache-static")) {
			int n = getProp("spark.expireTime", Integer::parseInt, Integer.MAX_VALUE);
			staticFiles.expireTime(n);
			System.out.println("caching static files for: "+n);
		}
			
		
		staticFiles.externalLocation("static-files");
		
		before("/", (req, res) -> res.redirect("/index.html"));
		System.out.printf("running at: %s:%d", getProp("heroku.host", "http://localhost"), port());
	}
	
	private static String getProp(String key, String defaultValue) {
		return System.getProperty(key, defaultValue);
	}

	private static <R> R getProp(String key, Function<String, R> mapper, R defaultValue) {
		return Optional.ofNullable(System.getProperty(key)).map(mapper).orElse(defaultValue);
	}
}
