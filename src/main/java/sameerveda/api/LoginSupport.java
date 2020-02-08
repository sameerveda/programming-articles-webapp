package sameerveda.api;

import java.util.UUID;

import org.apache.http.HttpStatus;

import sameerveda.routes.LoginRoute;
import spark.Request;
import spark.Response;
import spark.Session;

public interface LoginSupport {
	String LOGIN_REQUESTER_KEY = UUID.randomUUID().toString();
	String REDIRECT_KEY = UUID.randomUUID().toString();

	/**
	 * should return ROLE
	 * 
	 * @param username
	 * @param password
	 * @return
	 */
	String validate(String username, String password);

	default String getRole(Request req) {
		Session session = req.session(false);
		return session == null  ? null : session.attribute(roleKey());
	}

	default Object login(Request req, Response res) {
		Session session = req.session(true);
		session.removeAttribute(roleKey());
		session.attribute(REDIRECT_KEY, req.uri());
		session.attribute(LOGIN_REQUESTER_KEY, this);
		
		res.status(HttpStatus.SC_MOVED_TEMPORARILY);
		res.redirect(LoginRoute.PATH);
		return null;
	}

	default String roleKey() {
		return getClass().getCanonicalName().concat("_role");
	}

	default boolean isLoggedIn(Request req) {
		return getRole(req) != null;
	}
}
