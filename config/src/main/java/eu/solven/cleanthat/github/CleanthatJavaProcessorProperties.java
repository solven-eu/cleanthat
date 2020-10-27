package eu.solven.cleanthat.github;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * The configuration of what is not related to a language.
 * 
 * @author Benoit Lacelle
 *
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@SuppressWarnings("PMD.ImmutableField")
public class CleanthatJavaProcessorProperties {

	// private ILanguageProperties languageProperties;
	private Map<String, ?> parameters;

	// @JsonIgnore
	// public ILanguageProperties getLanguageProperties() {
	// return languageProperties;
	// }
	//
	// public void setLanguageProperties(ILanguageProperties languageProperties) {
	// this.languageProperties = languageProperties;
	// }

	public Map<String, ?> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, ?> parameters) {
		this.parameters = parameters;
	}

	// Needed to handle caching in JavaFormatter
	@Override
	public int hashCode() {
		return Objects.hash(parameters);
	}

	// Needed to handle caching in JavaFormatter
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		CleanthatJavaProcessorProperties other = (CleanthatJavaProcessorProperties) obj;
		return Objects.equals(parameters, other.parameters);
	}

}
