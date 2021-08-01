package eu.solven.cleanthat.github;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * The configuration of which ref are concerned by CleanThat or not.
 * 
 * Initial version: given a list of branches, we clean any PR/MR-head with one of these branches as base, and we open a
 * PR if we detect any of these branches is dirty
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class CleanthatRefFilterProperties {
	// By default, we clean a set of standard default branch names
	private List<String> branches = Arrays.asList("refs/heads/master", "refs/heads/develop");

	public List<String> getBranches() {
		return branches;
	}

	public void setBranches(List<String> labels) {
		this.branches = List.copyOf(labels);
	}

	@Override
	public int hashCode() {
		return Objects.hash(branches);
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
		CleanthatRefFilterProperties other = (CleanthatRefFilterProperties) obj;
		return Objects.equals(branches, other.branches);
	}

}
