package sameerveda.routes;

import java.util.Optional;

import org.eclipse.jetty.http.MimeTypes;

import spark.Request;
import spark.Response;
import spark.Route;

public class EchoRoute implements Route {

	@Override
	public Object handle(Request req, Response res) throws Exception {
		res.type(MimeTypes.Type.TEXT_PLAIN.asString());
		return Optional.ofNullable(req.queryParams("text")).orElseGet(() -> req.params("text"));
	}

}
