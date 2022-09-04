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
		if (!repoName.contains("/")) {
			throw new IllegalArgumentException("It seems we did not receive a fullName: " + repoName);
		}
		this.repoName = repoName;
		this.ref = ref;
		this.sha = sha;
	}

	/**
	 * 
	 * @return the repository fullname. e.g. 'solven-eu/cleanthat'
	 */
	public String getRepoFullName() {
		return repoName;
	}

	public String getRef() {
		return ref;
	}

	public String getSha() {
		return sha;
	}

	@Override
	public String toString() {
		return "GitRepoBranchSha1 [repoName=" + repoName + ", ref=" + ref + ", sha=" + sha + "]";
	}

}
