package sameerveda.routes.programming.articles;

import static programming.articles.model.dynamo.ConstantDataItem.ADDED_ON;
import static programming.articles.model.dynamo.ConstantDataItem.FAVICON;
import static programming.articles.model.dynamo.ConstantDataItem.ID;
import static programming.articles.model.dynamo.ConstantDataItem.REDIRECT;
import static programming.articles.model.dynamo.ConstantDataItem.SOURCE;
import static programming.articles.model.dynamo.ConstantDataItem.TITLE;
import static programming.articles.model.dynamo.DataItem.NOTES;
import static programming.articles.model.dynamo.DataItem.STATUS;
import static programming.articles.model.dynamo.DataItem.TAGS;

import java.util.List;

import org.json.JSONWriter;

import programming.articles.api.JsonWritable;
import programming.articles.api.StateManager;
import programming.articles.model.DataStatus;
import programming.articles.model.Tag;
import programming.articles.model.dynamo.ConstantDataItem;
import programming.articles.model.dynamo.DataItem;
import sam.myutils.Checker;

class Result implements JsonWritable {
	public int page;
	public int pageSize;
	public DataStatus status;
	public List<ConstantDataItem> data;

	@Override
	public void write(JSONWriter w) {
		w.key("page").value(page)
		.key("pageSize").value(pageSize)
		.key("status").value(status)
		.key("data")
		.array();
		data.forEach(d -> serialize(d, w));
		w.endArray();
	}

	public static void serialize(ConstantDataItem d, JSONWriter w) {
		w.object()
		.key(ID).value(d.getId())
		.key(TITLE).value(d.getTitle());
		put(w, "url", Checker.isEmptyTrimmed(d.getRedirect()) ? d.getSource() : d.getRedirect());
		put(w, FAVICON, d.getFavicon());
		
		w.endObject();
	}
	private static void put(JSONWriter w, String key, String value) {
		if(value != null && !value.isEmpty())
			w.key(key).value(value);
	}

	public static void serialize(DataItem d, JSONWriter w, StateManager manager) {
		w.key(ID).value(d.getId())
		.key(TITLE).value(d.getTitle());
		put(w, SOURCE, d.getSource());
		put(w, REDIRECT, d.getRedirect());
		put(w, FAVICON, d.getFavicon());

		w.key(STATUS).value(d.getStatus());
		w.key(ADDED_ON).value(d.getAddedOn());
		put(w, NOTES, d.getNotes());

		w.key(TAGS).array();

		Tag.parse(d.getTags(), s -> {
			Tag t = manager.getTagById(s);
			w.value(t.getName());
		});

		w.endArray();
	}
}