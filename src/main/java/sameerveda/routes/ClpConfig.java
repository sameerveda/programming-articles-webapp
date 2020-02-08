package sameerveda.routes;

import static sameerveda.routes.ClipboardRoute.PATH;
import static sameerveda.routes.ClipboardRoute.PATH_SLASHED;
import static spark.Spark.get;
import static spark.Spark.post;

import sam.di.Injector;
import sameerveda.api.RouteConfig;
import spark.Route;

public final class ClpConfig implements RouteConfig {

	@Override
	public void configure(Injector injector) {
		Route r = RouteConfig.lazyRoute(injector, ClipboardRoute.class); 
		get(PATH, r);
		String s = PATH_SLASHED.concat(":username");
		get(s, r);
		post(s, r);
	}

	@Override
	public String[] paths() {
		return new String[]{PATH};
	}

	@Override
	public String description() {
		return "a simple clipboard";
	}

	@Override
	public String example() {
		return PATH;
	}
}

