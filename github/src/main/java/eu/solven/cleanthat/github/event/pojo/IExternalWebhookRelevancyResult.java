package eu.solven.cleanthat.github.event.pojo;

public interface IExternalWebhookRelevancyResult {

	/**
	 * 
	 * @return true if current event refer to a ReviewRequest being open (e.g. the last commit may be old, but the PR
	 *         has just been opened)
	 */
	boolean isPrOpen();

	/**
	 * 
	 * @return true if current event refer to a Commit being pushed
	 */
	boolean isPushBranch();

	/**
	 * 
	 * @return true if given ref has at least one active review_request (e.g. a Github pull_request)
	 */
	boolean refHasOpenReviewRequest();

}
