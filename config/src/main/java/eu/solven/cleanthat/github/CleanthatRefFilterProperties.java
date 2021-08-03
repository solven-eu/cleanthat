package eu.solven.cleanthat.github;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanthatRefFilterProperties.class);

	// https://git-scm.com/book/en/v2/Git-Internals-Git-References
	private static final String REFS_PREFIX = "refs/heads/";

	// By default, we clean a set of standard default branch names
	// https://docs.github.com/en/github/administering-a-repository/managing-branches-in-your-repository/changing-the-default-branch
	// 'main' is the new default branch as Github
	private List<String> branches =
			Stream.of("master", "develop", "main").map(s -> REFS_PREFIX + s).collect(Collectors.toList());

	public List<String> getBranches() {
		return branches;
	}

	// TODO If a ref does not starts with 'refs/heads', add automatically 'refs/heads/' as prefix?
	public void setBranches(List<String> labels) {
		labels.stream()
				.filter(s -> !s.startsWith(REFS_PREFIX))
				.forEach(weirdRef -> LOGGER.warn("Weird ref: {}", weirdRef));

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
