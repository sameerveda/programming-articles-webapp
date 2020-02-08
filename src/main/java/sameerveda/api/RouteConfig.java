package sameerveda.api;
import org.json.JSONObject;

import sam.di.Injector;
import sameerveda.utils.DefaultHtmlTemplate;
import sameerveda.utils.StaticSiteBuilder;
import spark.Request;
import spark.Response;
import spark.Route;

public interface RouteConfig {
	void configure(Injector injector);
	String[] paths();
	String description();
	String example();
	
	public static Route lazyRoute(Injector injector, Class<? extends Route> routeCls) {
		return lazyRoute(injector, routeCls, null);
	}
	
	public static Route lazyRoute(Injector injector, Class<?> routeCls, JSONObject json) {
		return new Route() {
			Route delegate;
			Exception error;

			@Override
			public Object handle(Request req, Response res) throws Exception {
				if (delegate == null) {
					try {
						delegate = (Route) injector.instance(routeCls);
						if (delegate instanceof SetJson)
							((SetJson) delegate).setJson(json);
					} catch (Exception e) {
						error = e;
					}
				}
				if(error != null)
					return StaticSiteBuilder.errorPage(error, injector.instance(DefaultHtmlTemplate.class));
				
				return delegate.handle(req, res);
			}
		};
	}
}

