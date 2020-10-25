package eu.solven.cleanthat.github;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * The configuration of what is not related to a language.
 * 
 * @author Benoit Lacelle
 *
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class CleanthatMetaProperties {

	// Are we allowed to mutate pull-requests, or should we wait for a merge in master before cleaning master?
	// This option will clean ONLY the files modified within the PR (or all files if cleanthat configuration is
	// modified)
	// Default: it is OK to mutate PR before they are merged
	private boolean cleanPullRequests = true;

	// TODO We could offer to input a list of branches to keep clean
	// Are we allowed to mutate the main branch, or should we create a pull-request/merge-request
	// private boolean commitMainBranch = false;

	// Should we create pull-requests/merge-requests to clean the main branch?
	private boolean cleanMainBranch = true;

	// Are we allowed to mutate the branches which has not PR, or should we wait for a PR?
	// We prefer not to open PR to clean (not-main) branches, to prevent opening too many PRs, which may be out of the
	// branch commiter radar
	// Default: false as, as of 2020-10, the feature is not implemented yet
	private boolean cleanOrphanBranches = false;

	// The labels to apply to created PRs
	private List<String> labels = Arrays.asList();

	public boolean isCleanPullRequests() {
		return cleanPullRequests;
	}

	public void setCleanPullRequests(boolean cleanPullRequests) {
		this.cleanPullRequests = cleanPullRequests;
	}

	public boolean isCleanMainBranch() {
		return cleanMainBranch;
	}

	public void setCleanMainBranch(boolean cleanMainBranch) {
		this.cleanMainBranch = cleanMainBranch;
	}

	public boolean isCleanOrphanBranches() {
		return cleanOrphanBranches;
	}

	public List<String> getLabels() {
		return labels;
	}

	public void setLabels(List<String> labels) {
		this.labels = List.copyOf(labels);
	}
}
