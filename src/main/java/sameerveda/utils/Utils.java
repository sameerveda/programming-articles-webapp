package sameerveda.utils;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.function.Consumer;

import org.json.JSONArray;
import org.json.JSONTokener;
import org.json.JSONWriter;

import j2html.tags.Renderable;
import programming.articles.api.JsonWritable;
import sam.reference.WeakPool;

public interface Utils {
	public static final int MAX_BYTE_COUNT = 350 * 1024;
	public static final int MAX_STRING_SIZE = MAX_BYTE_COUNT / 6; // utf can use 1 - 6 bytes per charator
	public final Charset CHARSET = StandardCharsets.UTF_8;
	public final CharsetEncoder ENCODER = CHARSET.newEncoder();
	public final CharsetDecoder DECODER = CHARSET.newDecoder();

	public final WeakPool<ByteBuffer> bufferMax = new WeakPool<>(true, () -> ByteBuffer.allocate(MAX_BYTE_COUNT));
	public final WeakPool<ByteBuffer> buffer4kb = new WeakPool<>(true, () -> ByteBuffer.allocate(4 * 1024));
	public final WeakPool<CharBuffer> charbuffer = new WeakPool<>(true, () -> CharBuffer.allocate(100));
	public final WeakPool<StringBuilder> stringbuilder = new WeakPool<>(true, () -> new StringBuilder(2000));
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void eachOf(Object value, Consumer<String> action) {
		if (value instanceof String) {
			action.accept((String) value);
		} else if (value instanceof Iterable) {
			((Iterable) value).forEach(s -> action.accept((String) s));
		} else if (value instanceof Iterator) {
			((Iterator) value).forEachRemaining(s -> action.accept((String) s));
		} else if (value instanceof String[]) {
			for (String s : (String[]) value)
				action.accept(s);
		} else {
			throw new IllegalArgumentException("unknown type: " + value.getClass());
		}
	}
	
	public static JsonWritable jsonWritable(JsonWritable w) {
		return w;
	}

	static String normalizeUrl(String url) {
		if(!url.isEmpty() && url.charAt(url.length() - 1) == '/' && url.length() != 1)
			return url.substring(0, url.length() - 1);
		return url;
	}

	static JSONArray loadResourceJsonArray(String name) {
		InputStream is = ClassLoader.getSystemResourceAsStream(name);
		return is == null ? null : new JSONArray(new JSONTokener(is));
	}
	
	@SuppressWarnings("unchecked")
	public static <E> Class<E> toClass(String clsName) {
		try {
			return (Class<E>) Class.forName(clsName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(clsName, e);
		}
	}

	public static Renderable renderable(Renderable r) {
		return r;
	}
	
	public static Renderable withJsonWriter(Consumer<JSONWriter> w) {
		return (sb, model) -> w.accept(new JSONWriter(sb));
	}
	public static <E> JSONWriter appendJsonArray(JSONWriter w, E[] values) {
		w.array();
		for (E e : values) 
			w.value(e);
			
		w.endArray();
		return w;
	}
}
