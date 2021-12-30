package eu.solven.cleanthat.code_provider.github.event.pojo;

import java.util.Optional;

import eu.solven.cleanthat.codeprovider.git.GitRepoBranchSha1;
import eu.solven.cleanthat.codeprovider.git.HeadAndOptionalBase;

/**
 * POJO indicating if an event has to be processed or not
 * 
 * @author Benoit Lacelle
 *
 */
public class WebhookRelevancyResult {
	final Optional<HeadAndOptionalBase> oHeadAndOptBase;

	final Optional<String> oRejectedReason;

	public WebhookRelevancyResult(Optional<HeadAndOptionalBase> optHeadAndOptBase, Optional<String> rejectedReason) {
		this.oHeadAndOptBase = optHeadAndOptBase;
		this.oRejectedReason = rejectedReason;
	}

	public Optional<GitRepoBranchSha1> optHeadToClean() {
		return oHeadAndOptBase.map(HeadAndOptionalBase::getHeadToClean);
	}

	/**
	 * If present, this event shall be rejected
	 * 
	 * @return
	 */
	public Optional<String> optRejectedReason() {
		return oRejectedReason;
	}

	public Optional<GitRepoBranchSha1> optBaseForHead() {
		return oHeadAndOptBase.flatMap(HeadAndOptionalBase::optBaseForHead);
	}

	public static WebhookRelevancyResult relevant(HeadAndOptionalBase headAndBase) {
		return new WebhookRelevancyResult(Optional.of(headAndBase), Optional.empty());
	}

	public static WebhookRelevancyResult dismissed(String reason) {
		return new WebhookRelevancyResult(Optional.empty(), Optional.of(reason));
	}
}
