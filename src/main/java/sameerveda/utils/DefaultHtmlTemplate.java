package sameerveda.utils;

import static j2html.TagCreator.body;
import static j2html.TagCreator.each;
import static j2html.TagCreator.head;
import static j2html.TagCreator.html;
import static j2html.TagCreator.iff;
import static j2html.TagCreator.link;
import static j2html.TagCreator.meta;
import static j2html.TagCreator.script;
import static j2html.TagCreator.scriptWithInlineFile;
import static j2html.TagCreator.styleWithInlineFile;
import static j2html.TagCreator.title;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Singleton;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import sam.myutils.Checker;

@Singleton
public class DefaultHtmlTemplate {
	public ContainerTag create(String title, List<Object> scripts, List<Object> stylesSheets, DomContent bodyContent) {
		return html(
				head(
						meta().withCharset("UTF-8"),
						meta().withName("viewport").withContent("width=device-width, initial-scale=1.0"),
						meta().attr("http-equiv", "X-UA-Compatible").withContent("ie=edge"),
						iff(Optional.ofNullable(title).filter(s -> !s.isEmpty()), s -> title(s)))
				.with(styles(stylesSheets))
				.with(scripts(scripts)),
				body(bodyContent));
	}

	private DomContent scripts(List<Object> scripts) {
		return iff(Optional.ofNullable(scripts).filter(Checker::isNotEmpty), list -> each(list, s -> {
			if (s.getClass() == String.class)
				return script().withSrc((String) s);
			if (s.getClass() == WithAttrs.class) {
				ContainerTag d = script().withSrc(((WithAttrs) s).src);
				((WithAttrs) s).attrs.forEach((name, value) -> d.attr(name, value));
				return d;
			} else if (s.getClass() == FromFile.class)
				return scriptWithInlineFile(((FromFile) s).path);
			else
				return (DomContent) s;
		}));
	}

	private DomContent styles(List<Object> styles) {
		return iff(Optional.ofNullable(styles).filter(Checker::isNotEmpty), list -> each(list, s -> {
			if (s.getClass() == String.class)
				return link().withRel("stylesheet").withHref((String) s);
			else if (s.getClass() == FromFile.class)
				return styleWithInlineFile(((FromFile) s).path);
			else
				return (DomContent) s;
		}));

	}

	public static final class WithAttrs {
		public final String src;
		private final Map<String, String> attrs;

		public WithAttrs(String src, Map<String, String> attrs) {
			this.src = Objects.requireNonNull(src);
			this.attrs = Objects.requireNonNull(attrs);
		}
	}

	public static final class FromFile {
		public final String path;

		public FromFile(String path) {
			this.path = Objects.requireNonNull(path);
		}
	}
}
