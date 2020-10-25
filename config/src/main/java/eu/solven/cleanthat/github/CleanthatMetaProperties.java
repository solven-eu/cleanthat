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

	// Are we allowed to mutate pull-requests, or should we create new ones
	private boolean commitPullRequests = true;

	// Are we allowed to mutate the main branch, or should we create a pull-request/merge-request
	private boolean commitMainBranch = false;

	// The labels to apply to create branches
	private List<String> labels = Arrays.asList();

	public boolean isCommitPullRequests() {
		return commitPullRequests;
	}

	public void setCommitPullRequests(boolean commitPullRequests) {
		this.commitPullRequests = commitPullRequests;
	}

	public boolean isCommitMainBranch() {
		return commitMainBranch;
	}

	public void setCommitMainBranch(boolean commitMainBranch) {
		this.commitMainBranch = commitMainBranch;
	}

	public List<String> getLabels() {
		return labels;
	}

	public void setLabels(List<String> labels) {
		this.labels = List.copyOf(labels);
	}
}
