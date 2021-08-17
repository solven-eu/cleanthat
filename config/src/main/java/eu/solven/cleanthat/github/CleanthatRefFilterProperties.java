package eu.solven.cleanthat.github;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

/**
 * The configuration of which ref are concerned by CleanThat or not.
 * 
 * Initial version: given a list of branches, we clean any PR/MR-head with one of these branches as base, and we open a
 * PR if we detect any of these branches is dirty
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Data
public final class CleanthatRefFilterProperties {
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanthatRefFilterProperties.class);

	// https://git-scm.com/book/en/v2/Git-Internals-Git-References
	// In a local Git repository, refs are available at 'refs/heads/XXX'
	public static final String REFS_PREFIX = "refs/";
	public static final String BRANCHES_PREFIX = REFS_PREFIX + "heads/";

	// https://stackoverflow.com/questions/1526471/git-difference-between-branchname-and-refs-heads-branchname
	static final String REF_REMOTES = REFS_PREFIX + "remotes/";
	static final String REF_TAGS = REFS_PREFIX + "tags/";

	// By default, we clean a set of standard default branch names
	// https://docs.github.com/en/github/administering-a-repository/managing-branches-in-your-repository/changing-the-default-branch
	// 'main' is the new default branch as Github
	/**
	 * 
	 * @return the fully qualified branches (i.e. heads refs)
	 */
	// https://stackoverflow.com/questions/51388545/how-to-override-lombok-setter-methods
	@Setter(AccessLevel.NONE)
	private List<String> branches =
			Stream.of("develop", "main", "master").map(s -> BRANCHES_PREFIX + s).collect(Collectors.toList());

	public void setBranches(List<String> labels) {
		labels = labels.stream().map(branch -> {
			if (!branch.startsWith(REFS_PREFIX)) {
				String qualifiedRef = BRANCHES_PREFIX + branch;
				LOGGER.debug("We qualify {} into {}", branch, qualifiedRef);
				return qualifiedRef;
			} else {
				if (!branch.startsWith(BRANCHES_PREFIX)) {
					LOGGER.warn("Unusual ref: {}", branch);
				}
				return branch;
			}
		}).distinct().collect(Collectors.toList());

		this.branches = List.copyOf(labels);
	}
}
