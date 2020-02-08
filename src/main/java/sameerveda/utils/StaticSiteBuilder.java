package sameerveda.utils;

import static j2html.TagCreator.pre;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.output.StringBuilderWriter;

import j2html.tags.Renderable;
import sam.myutils.Checker;
import sam.nopkg.StringCollect;
import sam.reference.WeakPool;

public final class StaticSiteBuilder {
	private StaticSiteBuilder() {
	}

	private static final AtomicInteger count = new AtomicInteger();
	private static final WeakPool<StringBuilder> sbPool = StringCollect.add(
			new WeakPool<>(true, WeakPool.counted(StringBuilder::new, count)),
			sb -> sb.append("StaticSiteBuilder.sb.pool: ").append(count.get()));

	public static Renderable callFunc(String functionName, Object... args) {
		return (sink, model) -> {
			if (Checker.isEmpty(args)) {
				sink.append(functionName).append("(); ");
				return;
			}

			sink.append(functionName).append('(');
			for (int i = 0; i < args.length; i++) {
				Object o = args[i];
				if (o instanceof CharSequence)
					appendString(sink, (CharSequence) o);
				else
					sink.append(String.valueOf(o));

				if (i < args.length - 1)
					sink.append(',');
			}

			sink.append(')');
			sink.append(';');
		};
	}

	private static void appendString(Appendable sink, CharSequence s) throws IOException {
		sink.append('\'');
		int size = s.length();
		for (int i = 0; i < size; i++) {
			char c = s.charAt(i);
			if (c == '\'')
				sink.append('\\');
			sink.append(c);
		}
		sink.append('\'');
	}

	public static final Object SKIP = new Object();

	public static String join(Object... args) {
		StringBuilder sb = sbPool.poll();
		sb.setLength(0);

		int size = 0;
		for (Object o : args) {
			if (o instanceof CharSequence)
				size += ((CharSequence) o).length();
		}

		sb.ensureCapacity(size + 50);

		try {
			for (Object o : args) {
				if (o == SKIP)
					continue;

				if (o == null || o instanceof CharSequence) {
					sb.append((CharSequence) o);
				} else {
					try {
						((Renderable) o).render(sb);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
			return sb.toString();
		} finally {
			sbPool.add(sb);
		}
	}
	
	public static Renderable script(Renderable content) {
		return (sb, model) -> {
			sb.append("<script>");
			content.render(sb);
			sb.append("</script>");	
		};
	}

	public static Renderable alertScript(String msg) {
		return (sb, model) -> {
			sb.append("<script>alert(");
			appendString(sb, msg);
			sb.append(");</script>");
		};
	}

	public static Renderable errorPage(Exception e, DefaultHtmlTemplate defaultHtmlTemplate) {
		StringBuilderWriter sw = new StringBuilderWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		pw.close();
		return defaultHtmlTemplate.create("Error", null, null, pre(sw.toString()));
	}
}
