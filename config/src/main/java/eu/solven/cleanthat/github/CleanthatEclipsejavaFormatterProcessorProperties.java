package eu.solven.cleanthat.github;

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
public class CleanthatEclipsejavaFormatterProcessorProperties {

	private String url;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	// Needed to handle caching in JavaFormatter
	@Override
	public int hashCode() {
		return Objects.hash(url);
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
		CleanthatEclipsejavaFormatterProcessorProperties other = (CleanthatEclipsejavaFormatterProcessorProperties) obj;
		return Objects.equals(url, other.url);
	}

}
