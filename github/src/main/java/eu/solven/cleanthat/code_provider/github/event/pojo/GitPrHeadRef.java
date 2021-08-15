package eu.solven.cleanthat.code_provider.github.event.pojo;

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

	public GitPrHeadRef(String repoName, Object id) {
		this.repoName = repoName;
		this.id = id;
	}

	public String getRepoName() {
		return repoName;
	}

	public Object getId() {
		return id;
	}
}
