package eu.solven.cleanthat.codeprovider.git;

/**
 * Given PR may be cross repositories, we need to qualify a sha by its repo. A single sha1 may be associated to multiple
 * references.
 * 
 * @author Benoit Lacelle
 *
 */
// https://docs.github.com/en/developers/webhooks-and-events/webhooks/webhook-events-and-payloads#webhook-payload-example-32
public class GitRepoBranchSha1 {
	final String repoName;
	final String ref;
	final String sha;

	public GitRepoBranchSha1(String repoName, String ref, String sha) {
		this.repoName = repoName;
		this.ref = ref;
		this.sha = sha;
	}

	public String getRepoName() {
		return repoName;
	}

	public String getRef() {
		return ref;
	}

	public String getSha() {
		return sha;
	}
}
