package sameerveda.routes.programming.articles;

import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;
import static programming.articles.model.DataItemMeta.TAGS;
import static sameerveda.utils.Utils.appendJsonArray;
import static spark.Spark.halt;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import javax.inject.Singleton;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.http.HttpStatus;
import org.eclipse.jetty.http.MimeTypes;
import org.json.JSONArray;
import org.json.JSONObject;

import j2html.tags.Renderable;
import programming.articles.api.JsonWritable;
import programming.articles.api.StateManager;
import programming.articles.app.providers.DefaultProviders;
import programming.articles.impl.DefaultDataItemPagination;
import programming.articles.model.DataItem;
import programming.articles.model.DataItemMeta;
import programming.articles.model.DataStatus;
import programming.articles.model.Tag;
import sam.myutils.Checker;
import sam.myutils.System2;
import sam.nopkg.EnsureSingleton;
import sam.reference.ReferenceUtils;
import sameerveda.api.LoginSupport;
import sameerveda.utils.Utils;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

@Singleton
public class ProgrammingArticlesRoute implements Route, LoginSupport {
	private static final EnsureSingleton SINGLETON = new EnsureSingleton();
	{
		SINGLETON.init();
	}

	public static final String JSON_MIME = APPLICATION_JSON.asString();
	public static final String PATH = "/programming-articles";
	public static final String DATA_PATH = "/programming-articles/data";
	private static final String PAGINATION = "pagination";
	private final List<WeakReference<Session>> sessions = new ArrayList<>(); 

	private volatile Prdr _providers;
	private final AtomicLong lastAccess = new AtomicLong(System.currentTimeMillis());

	private Prdr providers() {
		if (_providers != null)
			return _providers;

		try {
			this._providers = new Prdr();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		startTimer();
		return _providers;
	}

	private void startTimer() {
		Timer timer = new Timer(true);
		long interval = Duration.ofMinutes(15).toMillis();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (System.currentTimeMillis() - lastAccess.get() < interval)
					return;
				synchronized (ProgrammingArticlesRoute.this) {
					if (System.currentTimeMillis() - lastAccess.get() < interval)
						return;
					
					sessions.forEach(w -> {
						Session s = ReferenceUtils.get(w);
						if(s != null)
							s.removeAttribute(PAGINATION);
					});
					sessions.clear();
					timer.cancel();
					DefaultProviders p = _providers;
					_providers = null;

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
		
		if (!req.uri().startsWith(DATA_PATH))
			return index();

		res.type(JSON_MIME);
		synchronized (this) {
			switch (req.uri().substring(DATA_PATH.length() + 1)) {
				case "item":  return item(req, res);
				case "metas": return metas(res);
				case "tags":  return tags(res);
				case "page":  return page(req, res);
				default:
					return halt(SC_BAD_REQUEST, "unknown type: " + req.uri());
			}
		}
	}

	private Object item(Request req, Response res) throws Exception {
		if (req.requestMethod().equalsIgnoreCase("POST")) {
			JSONObject json = new JSONObject(req.body());
			short id = toShort((int) json.remove(DataItemMeta.ID));
			if (json.isEmpty())
				halt(HttpStatus.SC_BAD_REQUEST, "no update specified");

			Map<String, String> updates = new HashMap<>();
			StateManager sm = providers().stateManager();

			json.keySet().forEach(s -> {
				if (s.equals(TAGS)) {
					JSONArray arry = json.getJSONArray(s);
					String tags;
					if (arry == null || arry.length() == 0) {
						tags = "";
					} else {
						tags = Tag.serialize(
								IntStream.range(0, arry.length())
								.map(n -> sm.getTagByName(arry.getString(n)).getId())
								.distinct()
								);
					}
					updates.put(TAGS, tags);
					sm.commitNewTags();
				} else {
					updates.put(s, json.getString(s));
				}
			});
			sm.commit((short) id, updates);
			return halt(SC_ACCEPTED);
		} else {
			String idS = req.queryParams("itemId");
			if (Checker.isEmptyTrimmed(idS))
				return halt(HttpStatus.SC_BAD_REQUEST, "itemId not specified");
			short id = toShort(Integer.parseInt(idS));
			DataItem d = providers().stateManager().getItem(id);
			return d == null ? halt(HttpStatus.SC_NOT_FOUND, "item not found with id: " + id)
					: Utils.jsonWritable(w -> Result.serialize(d, w, providers().stateManager()));
		}
	}

	private short toShort(int n) {
		if (n > Short.MAX_VALUE)
			halt(SC_BAD_REQUEST, "value value higher than: " + Short.MAX_VALUE);
		return (short) n;
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

	private Renderable tags(Response res) throws Exception {
		res.type(MimeTypes.Type.TEXT_PLAIN.asString());
		return (sb, model) -> {
			for (Tag t : providers().stateManager().allTagsNames())
				sb.append(t.getName()).append('\n');
		};
	}

	private Object page(Request req, Response res) throws Exception {
		Result result = new Result();
		result.pageSize = getInt(req, "pageSize", 25);
		result.startingId = getInt(req, "startingId", -1);
		result.page = getInt(req, "page", 0);
		String s = req.queryParamOrDefault("status", "UNREAD").toUpperCase();
		result.status = s.trim().equalsIgnoreCase("all") ? null : DataStatus.parse(s);

		DefaultDataItemPagination pagination = ReferenceUtils.get(req.session(true).attribute(PAGINATION));
		if (pagination == null) {
			pagination = new DefaultDataItemPagination(providers().stateManager());
			Session ses = req.session();
			ses.attribute(PAGINATION, new WeakReference<>(pagination));
			sessions.add(new WeakReference<>(ses));
		}

		pagination.setPageSize(result.pageSize);
		pagination.setPage(result.page);
		pagination.setStartingId((short) result.startingId);
		pagination.setStatus(result.status);

		result.manager = providers().stateManager();
		result.data = pagination.getData();
		return result;
	}

	private int getInt(Request req, String key, int defaultValue) {
		String s = req.queryParams(key);
		return s == null ? defaultValue : Integer.parseInt(s);
	}

	@Override
	public String validate(String username, String password) {
		// TODO
		return Objects.hashCode(username) == 92668751 && Objects.hashCode(password) == 92584519 ? "admin" : null;
	}

}
