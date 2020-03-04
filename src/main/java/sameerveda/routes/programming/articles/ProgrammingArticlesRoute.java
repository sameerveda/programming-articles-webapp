package sameerveda.routes.programming.articles;

import static java.lang.Integer.parseInt;
import static java.lang.Short.parseShort;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;
import static programming.articles.model.dynamo.ConstantDataItem.ID;
import static programming.articles.model.dynamo.DataItem.TAGS;
import static sameerveda.utils.Utils.appendJsonArray;
import static spark.Spark.halt;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import javax.inject.Singleton;

import org.apache.commons.io.output.StringBuilderWriter;
import org.eclipse.jetty.http.MimeTypes;
import org.json.JSONArray;
import org.json.JSONObject;

import j2html.tags.Renderable;
import programming.articles.api.JsonWritable;
import programming.articles.api.StateManager;
import programming.articles.app.providers.DefaultProviders;
import programming.articles.impl.DefaultDataItemPagination;
import programming.articles.model.DataStatus;
import programming.articles.model.Tag;
import programming.articles.model.dynamo.DataItem;
import sam.myutils.System2;
import sam.nopkg.EnsureSingleton;
import sameerveda.api.LoginSupport;
import spark.Request;
import spark.Response;
import spark.Route;

@Singleton
public class ProgrammingArticlesRoute implements Route, LoginSupport {
	private static final EnsureSingleton SINGLETON = new EnsureSingleton();
	{
		SINGLETON.init();
	}

	public static final String JSON_MIME = APPLICATION_JSON.asString();
	public static final String PATH = "/programming-articles";

	private volatile Prdr providers;
	private final AtomicLong lastAccess = new AtomicLong(System.currentTimeMillis());

	private void init() throws Exception {
		if (providers != null)
			return;

		this.providers = new Prdr();
		startTimer();
	}

	private void startTimer() {
		Timer timer = new Timer(true);
		long interval = Duration.ofMinutes(15).toMillis();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (System.currentTimeMillis() - lastAccess.get() < interval)
					return;
				synchronized (ProgrammingArticlesRoute.class) {
					timer.cancel();
					DefaultProviders p = providers;
					providers = null;

					try {
						p.close();
						System.out.println("closed resources: " + LocalDateTime.now());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}, 0, interval);
	}

	@Override
	public Object handle(Request req, Response res) throws Exception {
		lastAccess.set(System.currentTimeMillis());
		String role = getRole(req);
		if (role == null)
			return login(req, res);

		if (req.requestMethod().equalsIgnoreCase("POST")) {
			JSONObject json = new JSONObject(req.body());
			update(json);
			return halt(SC_ACCEPTED);
		}

		String type = req.params(":type");
		if (type == null)
			return index();

		String numS = req.params(":number");

		if (numS == null) {
			switch (type.toLowerCase()) {
				case "metas":
					return metas(res);
				case "tags":
					return tags(res);
			}
		}

		if (type != null && numS == null)
			return halt(SC_BAD_REQUEST, "number not defined");

		return create(req, res, type, numS);
	}

	private JsonWritable metas(Response res) {
		res.type(JSON_MIME);
		return w -> {
			w.key("allStatus");
			appendJsonArray(w, DataStatus.values());
		};
	}

	private volatile String index_page;
	private final boolean index_page_cached = System2.lookupBoolean("programming-articles-cached", true);

	{
		System.out.println("programming-articles-cached: " + index_page_cached);
	}

	private String index() throws IOException {
		if (index_page != null)
			return index_page;

		String page;
		try {
			String s = System2.lookup("programming-articles-file");
			Path p = s == null ? null : Paths.get(s);
			if (p == null)
				page = "programming-articles-file not set";
			else if (Files.notExists(p))
				page = "programming-articles-file: not found: " + s;
			else {
				page = new String(Files.readAllBytes(p));
				System.out.println("programming-articles-cached: " + s + "  " + page.length());
			}
		} catch (Exception e) {
			StringBuilderWriter w = new StringBuilderWriter();
			PrintWriter pw = new PrintWriter(w);
			e.printStackTrace(pw);
			page = w.toString();
		}

		this.index_page = index_page_cached ? page : null;
		return page;
	}

	private synchronized Object update(JSONObject json) throws Exception {
		int id = (int) json.remove(ID);
		if (id > Short.MAX_VALUE)
			return halt(SC_BAD_REQUEST, "id value higher than: " + Short.MAX_VALUE);
		if (json.isEmpty())
			return null;

		Map<String, String> updates = new HashMap<>();
		init();
		StateManager sm = providers.stateManager();

		try {
			json.keySet().forEach(s -> {
				if (s.equals(TAGS)) {
					JSONArray arry = json.getJSONArray(s);
					String tags = arry.isEmpty() ? ""
							: Tag.serialize(IntStream.range(0, arry.length())
									.map(n -> sm.getTagByName(arry.getString(n)).getId()));
					updates.put(TAGS, tags);
					sm.commitNewTags();
				} else {
					updates.put(s, json.getString(s));
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return sm.commit((short) id, updates);
	}

	private synchronized Object create(Request req, Response res, String type, String numS)
			throws NumberFormatException, Exception {
		res.type(JSON_MIME);
		switch (type.toLowerCase()) {
			case "page":
				return page(req, res, parseInt(numS));
			case "item":
				return item(parseShort(numS));
			default:
				return halt(SC_BAD_REQUEST, "unknown type: " + type);
		}
	}

	private Renderable tags(Response res) throws Exception {
		init();
		res.type(MimeTypes.Type.TEXT_PLAIN.asString());
		return (sb, model) -> {
			for (Tag t : providers.stateManager().allTagsNames())
				sb.append(t.getName()).append('\n');
		};
	}

	private JsonWritable item(short id) throws Exception {
		init();
		DataItem d = providers.stateManager().getItem(id);
		return w -> Result.serialize(d, w, providers.stateManager());
	}

	private Object page(Request req, Response res, int page) throws Exception {
		init();
		Result result = new Result();
		result.pageSize = parseInt(req.queryParamOrDefault("page_size", "25"));
		result.page = page;
		String s = req.queryParamOrDefault("status", "UNREAD").toUpperCase();
		result.status = s.trim().equalsIgnoreCase("all") ? null : DataStatus.parse(s);

		DefaultDataItemPagination pagination = req.session(true).attribute("pagination");
		if (pagination == null) {
			pagination = new DefaultDataItemPagination(providers.stateManager());
			req.session().attribute("pagination", pagination);
		}

		pagination.setPageSize(result.pageSize);
		pagination.setPage(result.page);
		pagination.setStatus(result.status);

		result.data = pagination.getData();
		res.type(JSON_MIME);

		return result;
	}

	@Override
	public String validate(String username, String password) {
		// TODO
		return Objects.hashCode(username) == 92668751 && Objects.hashCode(password) == 92584519 ? "admin" : null;
	}

}
