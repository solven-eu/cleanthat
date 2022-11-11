package eu.solven.cleanthat.codeprovider.git;

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
	boolean isPushRef();

	/**
	 * 
	 * @return true if given ref has at least one active review_request (e.g. a Github pull_request)
	 */
	// @Deprecated
	// boolean refHasOpenReviewRequest();

	/**
	 * 
	 * @return true if current event refer to a ReviewRequest being open (e.g. the last commit may be old, but the PR
	 *         has just been opened)
	 */
	boolean isReviewRequestOpen();

	/**
	 * For ReviewRequest events, this is the base. For push, this is the sha1 before the push.
	 * 
	 * Is empty in case of ref-creation.
	 * 
	 * @return the privileged base Ref for given event
	 */
	@Deprecated
	Optional<GitRepoBranchSha1> optBaseRef();

	Optional<GitPrHeadRef> optOpenPr();

}
