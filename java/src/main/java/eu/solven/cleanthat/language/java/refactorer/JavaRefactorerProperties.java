package eu.solven.cleanthat.language.java.refactorer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * The configuration of {@link JavaRefactorer}.
 * 
 * 'excluded' and 'included': we include any rule which is included (by exact match, or if '*' is included), and not
 * excluded (by exact match)
 * 
 * @author Benoit Lacelle
 *
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@SuppressWarnings("PMD.ImmutableField")
@Data
public class JavaRefactorerProperties {
	public static final String WILDCARD = "*";

	/**
	 * A {@link List} of excluded rules (by ID)
	 */
	private List<String> excluded = List.of();
	/**
	 * A {@link List} of included rules (by ID). '*' can be used to include all rules
	 */
	private List<String> included = List.of(WILDCARD);

	/**
	 * One may activate not-production-ready rules. It may be useful to test a new rule over some external repository
	 */
	@Deprecated
	private boolean productionReadyOnly = true;

	public static JavaRefactorerProperties defaults() {
		return new JavaRefactorerProperties();
	}

}
