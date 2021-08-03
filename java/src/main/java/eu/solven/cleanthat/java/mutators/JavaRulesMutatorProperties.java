package eu.solven.cleanthat.java.mutators;

import java.util.List;
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
public class JavaRulesMutatorProperties {

	private List<String> excluded = List.of();
	private boolean productionReadyOnly = true;

	@Override
	public int hashCode() {
		return Objects.hash(excluded, productionReadyOnly);
	}

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
		JavaRulesMutatorProperties other = (JavaRulesMutatorProperties) obj;
		return Objects.equals(excluded, other.excluded)
				&& Objects.equals(productionReadyOnly, other.productionReadyOnly);
	}

	public void setExcluded(List<String> excluded) {
		this.excluded = excluded;
	}

	public List<String> getExcluded() {
		return excluded;
	}

	public boolean isProductionReadyOnly() {
		return productionReadyOnly;
	}

	public void setProductionReadyOnly(boolean productionReadyOnly) {
		this.productionReadyOnly = productionReadyOnly;
	}

}
