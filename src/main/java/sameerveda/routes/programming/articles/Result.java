package sameerveda.routes.programming.articles;

import static programming.articles.model.DataItemMeta.TAGS;

import java.util.List;

import org.json.JSONWriter;

import programming.articles.api.JsonWritable;
import programming.articles.api.StateManager;
import programming.articles.model.DataItem;
import programming.articles.model.DataStatus;
import programming.articles.model.Tag;

class Result implements JsonWritable {
	int page = -1, startingId = -1;
	int pageSize;
	DataStatus status;
	List<DataItem> data;
	StateManager manager;

	@Override
	public void write(JSONWriter w) {
		if(startingId >= 0) 
			w.key("startingId").value(startingId);
		w.key("page").value(page)
		.key("pageSize").value(pageSize)
		.key("status").value(status)
		.key("data")
		.array();
		data.forEach(d -> {
			w.object();
			serialize(d, w, manager);
			w.endObject();
		});
		w.endArray();
	}

	public static void serialize(DataItem d, JSONWriter w, StateManager manager) {
		d.write(w);
		w.key(TAGS+"_parsed").array();

		Tag.parse(d.getTags(), s -> {
			Tag t = manager.getTagById(s);
			w.value(t.getName());
		});

		w.endArray();
	}
}