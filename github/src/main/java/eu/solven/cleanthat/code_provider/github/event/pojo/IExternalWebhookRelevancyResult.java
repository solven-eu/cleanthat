package eu.solven.cleanthat.code_provider.github.event.pojo;

import java.util.Optional;

/**
 * Details about events being filtered through cleanthat pipeline
 * 
 * @author Benoit Lacelle
 *
 */
public interface IExternalWebhookRelevancyResult {

	/**
	 * 
	 * @return true if current event refer to a Commit being pushed
	 */
	boolean isPushBranch();

	/**
	 * 
	 * @return true if given ref has at least one active review_request (e.g. a Github pull_request)
	 */
	@Deprecated
	boolean refHasOpenReviewRequest();

	/**
	 * 
	 * @return true if current event refer to a ReviewRequest being open (e.g. the last commit may be old, but the PR
	 *         has just been opened)
	 */
	boolean isReviewRequestOpen();

	/**
	 * This is filled only for ReviewRequest specific events.
	 * 
	 * @return the privileged base Ref for given event
	 */
	@Deprecated
	Optional<GitRepoBranchSha1> optBaseRef();

}
