package eu.solven.cleanthat.anything.event;

import java.util.Map;
import java.util.Optional;

import eu.solven.cleanthat.github.event.pojo.GitRepoBranchSha1;
import eu.solven.cleanthat.github.event.pojo.IExternalWebhookRelevancyResult;

/**
 * Holds the logic to clean a code poointer (e.g. local folder, or Git ref, ...)
 *
 * @author Benoit Lacelle
 */
public interface ICodePointerCleaner {

	/**
	 * 
	 * @param offlineResult
	 * @param theRef
	 * @return the ref to clean. Typically different to the input ref when we want to clean through a PR (e.g. not to
	 *         modify directly the dirty branch).
	 */
	Optional<String> prepareRefToClean(IExternalWebhookRelevancyResult offlineResult, GitRepoBranchSha1 theRef);

	Map<String, ?> formatRef(String ref);

	Map<String, ?> formatRefDiff(String baseRef, String headRef);
}
