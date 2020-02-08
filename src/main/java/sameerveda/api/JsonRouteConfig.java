package sameerveda.api;

import static sameerveda.utils.Utils.eachOf;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

import sam.di.Injector;
import sameerveda.routes.ContentRoute;
import sameerveda.routes.EchoRoute;
import sameerveda.utils.DefaultTransformer;
import sameerveda.utils.Utils;
import spark.ResponseTransformer;
import spark.Route;
import spark.Spark;
public class JsonRouteConfig implements RouteConfig {
	private final JSONObject json;
	private String[] paths;

	public JsonRouteConfig(JSONObject json) {
		this.json = json;
		Object obj = json.get("path");
		if (obj.getClass() == String.class)
			this.paths = new String[] { (String) obj };
		else {
			JSONArray arr = (JSONArray) obj;
			this.paths = new String[arr.length()];
			for (int i = 0; i < paths.length; i++)
				paths[i] = arr.getString(i);
		}
	}

	@Override
	public void configure(Injector injector) {
		Object path = json.get("path");
		Object method = json.opt("method");
		ResponseTransformer transformer = Optional.ofNullable(json.optString("transformer")).filter(s -> !s.isEmpty())
				.<ResponseTransformer>map(clsName -> injector.instance(Utils.toClass(clsName)))
				.orElseGet(() -> injector.instance(DefaultTransformer.class));

		Route route = null;

		switch (json.getString("type").toLowerCase()) {
			case "redirect": {
				String redirect = json.getString("redirect");
				eachOf(path, p -> Spark.redirect.get(p, redirect));
				return;
			}
			case "echo":
				route = new EchoRoute();
				break;
			case "content":
				route = new ContentRoute(json);
				break;
			case "handled":
				Class<?> cls = Utils.toClass(json.getString("handler"));
				route = RouteConfig.lazyRoute(injector, cls, json);
				break;
			default:
				throw new RuntimeException(String.format("unknown type: \"%s\", in:\n%s", json.getString("type"), json.toString(4)));
		}

		if (route != null) {
			Route rt = route;
			if (method == null)
				method = "GET";

			eachOf(method, m -> eachOf(path, p -> {
				p = Utils.normalizeUrl(p);
				switch (m.toUpperCase()) {
					case "GET":
						get(p, rt, transformer);
						break;
					case "POST":
						post(p, rt, transformer);
						break;
					case "PUT":
						put(p, rt, transformer);
						break;
					case "DELETE":
						delete(p, rt, transformer);
						break;
					default:
						throw new IllegalArgumentException("unknown method: " + m + ", in" + json);
				}
			}));
		}
	}

	@Override
	public String[] paths() {
		return paths;
	}

	@Override
	public String description() {
		return json.optString("description");
	}

	@Override
	public String example() {
		return json.optString("example");
	}
}
