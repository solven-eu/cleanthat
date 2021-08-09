package eu.solven.cleanthat.java.mutators;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * The configuration of what is not related to a language.
 * 
 * @author Benoit Lacelle
 *
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@SuppressWarnings("PMD.ImmutableField")
@Data
public class JavaRulesMutatorProperties {

	private List<String> excluded = List.of();
	private boolean productionReadyOnly = true;

	public static JavaRulesMutatorProperties defaults() {
		return new JavaRulesMutatorProperties();
	}

}
