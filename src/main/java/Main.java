
import static j2html.TagCreator.a;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.iffElse;
import static j2html.TagCreator.span;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static spark.Spark.after;
import static spark.Spark.awaitInitialization;
import static spark.Spark.before;
import static spark.Spark.defaultResponseTransformer;
import static spark.Spark.get;
import static spark.Spark.initExceptionHandler;
import static spark.Spark.ipAddress;
import static spark.Spark.notFound;
import static spark.Spark.port;
import static spark.Spark.staticFiles;

import java.net.Inet4Address;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpHeaderValue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import sam.di.FeatherInjector;
import sam.di.Injector;
import sam.di.InjectorProvider;
import sam.myutils.Checker;
import sam.myutils.System2;
import sameerveda.api.JsonRouteConfig;
import sameerveda.api.RouteConfig;
import sameerveda.routes.LoginRoute;
import sameerveda.utils.DefaultHtmlTemplate;
import sameerveda.utils.DefaultTransformer;
import sameerveda.utils.Utils;
import spark.ResponseTransformer;

public class Main {
	public static void main(String[] args) throws Exception {
		new Main(args);
	}

	private Logger logger;
	private final Injector injector;
	private Runnable addressPrinter;
	private boolean enableCors;

	public Main(String[] args) throws Exception {
		logger = LoggerFactory.getLogger(Main.class);
		List<RouteConfig> configs = new ArrayList<>();

		ServiceLoader.load(RouteConfig.class).forEach(configs::add);
		JSONArray routes = Utils.loadResourceJsonArray("routes.json");
		if (routes != null)
			routes.forEach(t -> configs.add(new JsonRouteConfig((JSONObject) t)));

		injector = Injector.init(new FeatherInjector(providers(new DefaultProviders(), configs)));
		defaultSpark(configs, injector);

		configs.forEach(c -> c.configure(injector));
		String notFound = new String(Files.readAllBytes(Paths.get("static-files/notfound.html")));
		notFound(notFound);
		
		if(enableCors)
			enableCors();

		awaitInitialization();

		addressPrinter.run();
		addressPrinter = null;
	}

	private void enableCors() {
		after((req, res) -> {
			res.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
			res.header("Access-Control-Allow-Origin", "*");
			res.header("Access-Control-Allow-Headers", "Content-Type, content-type,Authorization,X-Requested-With,Content-Length,Accept,Origin,");
			res.header("Access-Control-Allow-Credentials", "true");
		});
		logger.info("cors enabled");
	}

	private void defaultSpark(List<RouteConfig> configs, Injector injector) throws Exception {
		if(System2.lookup("heroku.port") != null) {
			port(Integer.parseInt(System2.lookup("heroku.port")));
			addressPrinter = () -> logger.info("running at port: {}", port());
		} else {
			String address = Inet4Address.getLocalHost().getHostAddress();
			ipAddress(address);
			port(8181);
			addressPrinter = () -> logger.info("running at: http://{}:{}", address, 8181);
			enableCors = true;
		}

		if (System2.lookupBoolean("cache-static", true)) {
			int n = System2.lookupInt("spark.expireTime", Integer.MAX_VALUE);
			staticFiles.expireTime(n);
			logger.info("caching static files for: {}", n);
		} else {
			staticFiles.expireTime(0);
			logger.info("caching static files disabled");
		}

		ResponseTransformer trasformer = injector.instance(DefaultTransformer.class);
		defaultResponseTransformer(trasformer);
		String index = trasformer.render(index(configs, injector.instance(DefaultHtmlTemplate.class)));

		initExceptionHandler(e -> e.printStackTrace());
		// see: https://github.com/perwendel/spark-kotlin/issues/23
		staticFiles.externalLocation("static-files");

		get("/", (req, res) -> index);
		before((req, res) -> {
			String s = req.uri();
			String normalize = Utils.normalizeUrl(s);
			if (s != normalize)
				res.redirect(normalize, HttpStatus.SC_MOVED_PERMANENTLY);
			else {
				res.header(HttpHeader.CONTENT_ENCODING.asString(), HttpHeaderValue.GZIP.asString());
			}
		});

		LoginRoute.configure(injector);
	}

	private static ContainerTag index(List<RouteConfig> routes, DefaultHtmlTemplate htmlTemplate) {
		DomContent body = iffElse(routes.isEmpty(), h1("NO ROUTES DEFINED").withClass("no-routes"),
				table(thead(th("path"), th("description"), th("example")), tbody(each(routes, item -> {
					return tr(td(of(item.paths()).map(s -> s.length < 2 ? s[0] : Arrays.toString(s)).get()),
							td(emptyString(item.description())), td(ofNullable(item.example())
									.filter(Checker::isNotEmpty).map(s -> a(s).withHref(s)).orElse(span("--"))));
				}))).withId("routes-table"));
		return htmlTemplate.create("HOME", null, Arrays.asList("style.css"), body);
	}

	private static String emptyString(String s) {
		return s == null ? "--" : s;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List providers(DefaultProviders defaultProviders, List<RouteConfig> configs) {
		List providers = InjectorProvider.detect();
		if (!providers.isEmpty())
			logger.info(providers.stream().map(Object::toString).collect(Collectors.joining("  ", "providers:  ", ""))
					.toString());

		providers.add(defaultProviders);
		providers.addAll(configs);
		return providers;
	}
}
