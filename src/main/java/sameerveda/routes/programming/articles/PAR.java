package sameerveda.routes.programming.articles;

import static sameerveda.routes.programming.articles.ProgrammingArticlesRoute.JSON_MIME;
import static sameerveda.routes.programming.articles.ProgrammingArticlesRoute.*;
import static spark.Spark.get;
import static spark.Spark.post;

import sam.di.Injector;
import sameerveda.api.RouteConfig;
import spark.Route;

public final class PAR implements RouteConfig {
	
		@Override
		public void configure(Injector injector) {
			Route r = RouteConfig.lazyRoute(injector, ProgrammingArticlesRoute.class);
			
			String[] urls = {"/item", "/metas", "/tags", "/page"};

			post(DATA_PATH.concat(urls[0]), JSON_MIME, r);
			for (String s : urls) 
				get(DATA_PATH.concat(s), JSON_MIME, r);
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