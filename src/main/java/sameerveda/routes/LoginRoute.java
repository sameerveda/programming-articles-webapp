package sameerveda.routes;

import static sameerveda.utils.StaticSiteBuilder.alertScript;
import static sameerveda.utils.StaticSiteBuilder.join;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.post;

import org.json.JSONArray;
import org.json.JSONObject;

import sam.di.Injector;
import sameerveda.api.LoginSupport;
import sameerveda.utils.Utils;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

public class LoginRoute implements Route {
	public static void configure(Injector injector) {
		JSONArray loginRequired = Utils.loadResourceJsonArray("login-required.json");
		if(loginRequired != null) {
			for (int i = 0; i < loginRequired.length(); i++) {
				JSONObject json = loginRequired.getJSONObject(i);
				LoginSupport[] login = {null};
				Filter filter = (req, res) -> {
					if(login[0] == null)
						login[0] = injector.instance(Utils.toClass(json.getString("handler")));
					if(!login[0].isLoggedIn(req)) {
						login[0].login(req, res);
					}
				};
				Utils.eachOf(json.get("path"), s -> before(s, filter));
			}
		}
		
		LoginRoute route = new LoginRoute();
		get(LoginRoute.PATH, route);
		post(LoginRoute.PATH, route);
	}
	
	private static final String LOGIN_PAGE_START = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" /><meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\" /><link rel=stylesheet href=https://cdnjs.cloudflare.com/ajax/libs/normalize/5.0.0/normalize.css /><link rel=stylesheet href=https://cdnjs.cloudflare.com/ajax/libs/milligram/1.3.0/milligram.css /><link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/animate.css/3.7.2/animate.min.css\" /><title>Login</title></head><style>html,body{font-family:\"Consolas\",monospace;height:100%;overflow:hidden}h1{position:absolute;left:50%;transform:translateX(-50%);animation:hinge 6s forwards;animation-delay:5s}.container{min-width:200px;max-width:300px;width:30%;top:50%;transform:translateY(-50%);padding:2rem;border:0.1rem solid #d1d1d1;border-radius:0.4rem}.container button {margin-bottom:0}</style><body><h1>Login</h1><form class=\"container\" method=\"post\"> <input type=\"text\" name=\"username\" id=\"username\" placeholder=\"Username\" /> <input type=\"password\" name=\"password\" id=\"password\" placeholder=\"Password\" /><button type=\"submit\">Login</button></form>";
	private static final String LOGIN_PAGE_END = "</body></html>";
	private static final String LOGIN_PAGE = join(LOGIN_PAGE_START, LOGIN_PAGE_END);
	private static final String NOT_ASKED = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" /><meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\" /><title>NO LOGIN</title></head><style>html,body{font-family:\"Consolas\",monospace;height:100%;overflow:hidden;animation:toRed 200s forwards}h1{font-size:20vh;width:100%;text-align:center;padding:0;margin:0;position:absolute;top:50%;transform:translateY(-50%)}@keyframes toRed{50%{background-color:red}100%{background-color:black}}</style><body><h1>Nobody asked this login</h1><a href=\"/\">GO BACK</a></body></html>";
	public static final String PATH = "/login";

	@Override
	public Object handle(Request req, Response res) throws Exception {
		Session session = req.session(false);
		LoginSupport requester = session == null ? null : session.attribute(LoginSupport.LOGIN_REQUESTER_KEY);
		if (requester == null)
			return NOT_ASKED;

		if ("GET".equalsIgnoreCase(req.requestMethod()))
			return LOGIN_PAGE;
		if ("POST".equalsIgnoreCase(req.requestMethod())) {
			String role = requester.validate(req.queryParams("username"), req.queryParams("password"));
			if (role != null) {
				session.attribute(requester.roleKey(), role);
				res.redirect(session.attribute(LoginSupport.REDIRECT_KEY));
				return null;
			} else {
				return join(
						LOGIN_PAGE_START, 
						alertScript("invalid username/password"),
						LOGIN_PAGE_END
						);
			}
		}
		return null;
	}
}
