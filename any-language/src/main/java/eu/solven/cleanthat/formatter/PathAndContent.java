package eu.solven.cleanthat.formatter;

import java.nio.file.Path;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

/**
 * Couple a {@link Path} (which may not be based on FileSystems.default()) and its content. The content is fetched
 * lazily. Once it is fetched, it is cached.
 * 
 * @author Benoit Lacelle
 *
 */
public class PathAndContent {
	final Path path;
	final Supplier<String> contentSupplier;

	public PathAndContent(Path path, Supplier<String> contentSupplier) {
		this.path = path;
		this.contentSupplier = Suppliers.memoize(contentSupplier::get);
	}

	public PathAndContent(Path path, String content) {
		this(path, () -> content);
	}

	public Path getPath() {
		return path;
	}

	public String getContent() {
		return contentSupplier.get();
	}

	public PathAndContent withContent(String newContent) {
		return new PathAndContent(getPath(), () -> newContent);
	}
}
