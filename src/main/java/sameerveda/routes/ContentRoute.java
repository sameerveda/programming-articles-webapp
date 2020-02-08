package sameerveda.routes;

import org.eclipse.jetty.http.MimeTypes;
import org.json.JSONObject;

import spark.Request;
import spark.Response;
import spark.Route;

public class ContentRoute implements Route {
	final String content;
	final String contentType;
	
	public ContentRoute(JSONObject source) {
		this.content = source.getString("content");
		this.contentType = source.optString("contentType", MimeTypes.Type.TEXT_PLAIN.asString());
	}

	@Override
	public Object handle(Request req, Response res) throws Exception {
		res.type(contentType);
		return content;
	}

}
