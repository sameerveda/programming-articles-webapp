package sameerveda.routes;

import static sam.full.access.dynamodb.DynamoConnection.value;
import static sam.myutils.Checker.isEmpty;
import static sam.myutils.Checker.isEmptyTrimmed;
import static sameerveda.utils.StaticSiteBuilder.SKIP;
import static sameerveda.utils.StaticSiteBuilder.callFunc;
import static sameerveda.utils.StaticSiteBuilder.join;
import static sameerveda.utils.StaticSiteBuilder.script;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.MimeTypes;

import sam.full.access.dynamodb.DynamoConnection;
import sam.nopkg.EnsureSingleton;
import spark.Request;
import spark.Response;
import spark.Route;

@Singleton
public class ClipboardRoute implements Route {
	private static final EnsureSingleton SINGLETON = new EnsureSingleton();
	public static final String PATH = "/clipboard";
	public static final String PATH_SLASHED = "/clipboard/";
	{
		SINGLETON.init();
	}
	final String wrapper_start = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" /><meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\" /><link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/css?family=Roboto:300,300italic,700,700italic\" /><link rel=stylesheet href=https://cdnjs.cloudflare.com/ajax/libs/normalize/5.0.0/normalize.css /><link rel=stylesheet href=https://cdnjs.cloudflare.com/ajax/libs/milligram/1.3.0/milligram.css /><title>Clipboard</title></head><style>html,body,#clipboard{height:100%}#clipboard{display:flex;flex-direction:column}#clipboard>*{flex-grow:0}#clipboard>*:nth-child(2){flex-grow:1}#clipboard:not(:valid) button{color:red}#clipboard .title {margin-top: 1rem;} #clipboard .bottom{font-size:1rem;display:flex;justify-content:space-between;align-items:center}#clipboard-open{display:flex;width:30vw;position:absolute;left:50%;top:50%;transform:translate(-50%, -50%)}#clipboard-open button{margin-left:0.2rem}#clipboard-open:invalid button{background-color:gray}.error-msg{display:none;background:darkred;color:white;padding:5px;position:absolute;width:100%;bottom:0}#clipboard-open:invalid+.error-msg{display:block}#banner,#banner-sub{display:block;width:100%;text-align:center}#banner{font-size:10rem}</style><body> <script>function setValueById(...array){let n=0;while(n<array.length){document.getElementById(array[n++]).value=array[n++];}} function setInnerTextById(...array){let n=0;while(n<array.length){document.getElementById(array[n++]).innerText=array[n++];}} function usernameError(username,error){if(username)setValueById(\"username\",username);if(error)alert(error);}</script>";
	final String wrapper_end = "</body></html>";

	final String home_page = "<h1 id=\"banner\">CLIPBOARD</h1> <span id=\"banner-sub\">Keep/Share your text chunks</span><form id=\"clipboard-open\" method=\"post\"> <input type=\"text\" name=\"username\" id=\"username\" minlength=\"3\" maxlength=\"20\" pattern=\"\\w+\" required placeholder=\"Your Username? (anything will be file)\" title=\"An alphanumeric string\" /><button type=\"submit\">GO</button></form>";

	final String cliboard_page_start = "<form id=\"clipboard\" class=\"container\" method=\"post\"><h1 class=\"title\">Clipboard</h1><textarea name=\"clipboard-data\" id=\"clipboard-data\">";
	final String cliboard_page_end = "</textarea><div class=\"bottom\"> <span id=\"last-modified-date\"></span> <button class=\"float-right\" type=\"submit\">Save</button></div></form>";
	
	private final DynamoConnection connection;

	@Inject
	public ClipboardRoute(DynamoConnection connection) {
		this.connection = connection;
	}

	@Override
	public Object handle(Request req, Response res) throws Exception {
		res.type(MimeTypes.Type.TEXT_HTML.asString());
		String username;
		username = req.params(":username");

		boolean isPost = req.requestMethod().equalsIgnoreCase("POST");

		if (isEmptyTrimmed(username) && isPost) {
			username = req.queryParams("username");
			if (isEmptyTrimmed(username))
				res.redirect(PATH, HttpStatus.BAD_REQUEST_400);
			else
				res.redirect(PATH_SLASHED.concat(username));
			return null;
		}

		if (isEmptyTrimmed(username))
			return homePage(null, null);
		else if (username.length() < 3 || username.length() > 20 || !username.matches("\\w+"))
			return homePage(username, "bad username");

		if (isPost) {
			String data = req.queryParams("clipboard-data");
			if (isEmpty(data)) {
				connection.delete(ClipboardData.TABLE_NAME, ClipboardData.ID, value(username));
			} else {
				ClipboardData clp = new ClipboardData(username, data);
				connection.mapper.save(clp);
			}
			res.redirect(req.url());
			return null;
		} else {
			return clipboardPage(connection.mapper.load(ClipboardData.class, username));
		}
	}

	private Object clipboardPage(ClipboardData clp) {
		return join(
				wrapper_start, 
				cliboard_page_start, 
				(clp == null ? SKIP : clp.stringData), 
				cliboard_page_end,
				(clp == null ? SKIP : script(callFunc("setInnerTextById", "last-modified-date", "Last Modified: " + clp.updatedOn))),
				wrapper_end
				);
	}

	private String homePage(String username, String error) {
		Object script = username == null && error == null ? SKIP : script(callFunc("usernameError", username, error));
		return join(wrapper_start, home_page, script, wrapper_end);
	}

}

