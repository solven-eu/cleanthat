package eu.solven.cleanthat.language.java.rules.meta;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.ast.Node;

/**
 * For classes knowing how to modify code
 *
 * @author Benoit Lacelle
 */
public interface IClassTransformer {

	// For java, prefer Checkstyle name, else PMD name
	@Deprecated
	default String getId() {
		return "TODO";
	}

	default Set<String> getIds() {
		Set<String> ids = Stream.of(Optional.of(getId()), getPmdId(), getCheckstyleId())
				.flatMap(Optional::stream)
				.filter(s -> !"TODO".equals(s))
				.collect(Collectors.toSet());

		if (ids.isEmpty()) {
			throw new IllegalStateException("We miss an id for : " + this.getClass());
		}
		return ids;
	}

	default Optional<String> getPmdId() {
		return Optional.empty();
	}

	default Optional<String> getCheckstyleId() {
		return Optional.empty();
	}

	String minimalJavaVersion();

	default boolean isProductionReady() {
		return true;
	}

	/**
	 * 
	 * @param pre
	 * @return true if the AST has been modified.
	 */
	boolean walkNode(Node pre);

}
