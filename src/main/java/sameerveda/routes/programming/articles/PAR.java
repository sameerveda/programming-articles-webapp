package sameerveda.routes.programming.articles;

import static sameerveda.routes.programming.articles.ProgrammingArticlesRoute.JSON_MIME;
import static sameerveda.routes.programming.articles.ProgrammingArticlesRoute.PATH;
import static spark.Spark.get;
import static spark.Spark.post;

import sam.di.Injector;
import sameerveda.api.RouteConfig;
import spark.Route;

public final class PAR implements RouteConfig {
	
		@Override
		public void configure(Injector injector) {
			Route r = RouteConfig.lazyRoute(injector, ProgrammingArticlesRoute.class);

			post(PATH.concat("/item_update"), JSON_MIME, r);
			get(PATH.concat("/:type/:number"), JSON_MIME, r);
			get(PATH.concat("/:type"), JSON_MIME, r);
			get(PATH, r);
		}

		@Override
		public String[] paths() {
			return new String[]{PATH};
		}

		@Override
		public String description() {
			return "articles to read";
		}

		@Override
		public String example() {
			return PATH;
		}
		
	}