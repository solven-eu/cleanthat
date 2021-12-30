package eu.solven.cleanthat.codeprovider.git;

/**
 * Given PR may be cross repositories, we need to qualify the head by the actual repository (which may differ from the
 * base branch repo).
 * 
 * @author Benoit Lacelle
 *
 */
// https://docs.github.com/en/developers/webhooks-and-events/webhooks/webhook-events-and-payloads#webhook-payload-example-32
public class GitPrHeadRef {
	final String repoName;
	final Object id;

	final String baseRef;
	final String headRef;

	public GitPrHeadRef(String repoName, Object id, String baseRef, String headRef) {
		this.repoName = repoName;
		this.id = id;
		this.baseRef = baseRef;
		this.headRef = headRef;
	}

	public String getRepoName() {
		return repoName;
	}

	public Object getId() {
		return id;
	}

	public String getBaseRef() {
		return baseRef;
	}

	public String getHeadRef() {
		return headRef;
	}
}
