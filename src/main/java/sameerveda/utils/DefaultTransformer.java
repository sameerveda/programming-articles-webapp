package sameerveda.utils;

import org.apache.commons.io.output.StringBuilderWriter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import j2html.tags.Renderable;
import programming.articles.api.JsonWritable;
import spark.ResponseTransformer;

public class DefaultTransformer implements ResponseTransformer {

	private final StringBuilderWriter sw = new StringBuilderWriter();

	@Override
	public String render(Object model) throws Exception {
		if (model == null)
			return null;
		
		if(model.getClass() == String.class)
			return (String) model;

		synchronized (sw) {
			sw.getBuilder().setLength(0);
			
			if(model instanceof JsonWritable) {
				JSONWriter w = new JSONWriter(sw);
				w.object();
				((JsonWritable) model).write(w);
				w.endObject();
			} else if (model instanceof JSONObject) {
				((JSONObject) model).write(sw);
			} else if (model instanceof JSONArray) {
				((JSONArray) model).write(sw);
			} else if (model instanceof Renderable) {
				((Renderable) model).render(sw);
			} else if (model instanceof CharSequence) {
				return model.toString();
			} else {
				throw new IllegalArgumentException("unknown data type: " + model.getClass());
			}
			return sw.toString();
		}
	}
}
