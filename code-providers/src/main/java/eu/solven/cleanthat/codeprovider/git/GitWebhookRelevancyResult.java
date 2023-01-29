/*
 * Copyright 2023 Solven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.codeprovider.git;

import java.util.Optional;

/**
 * Holds details about a filter over an Event
 * 
 * @author Benoit Lacelle
 *
 */
public class GitWebhookRelevancyResult implements IExternalWebhookRelevancyResult {
	final boolean rrOpen;
	final boolean pushRef;
	final Optional<GitRepoBranchSha1> optRef;
	final Optional<GitPrHeadRef> optOpenRr;

	// This is a compatible base: trivial in case of PR event, implicit in case of commit_push (i.e. we take the base of
	// the first PR matching the head). We may prefer taking the latest matching PR (supposing older PR will not be
	// merged soon). However, it would mean a single head is being merged into different base: edge-case.
	final Optional<GitRepoBranchSha1> oBaseRef;

	public GitWebhookRelevancyResult(boolean rrOpen,
			boolean pushRef,
			Optional<GitRepoBranchSha1> optRef,
			Optional<GitPrHeadRef> optOpenRr,
			Optional<GitRepoBranchSha1> optBaseRef) {
		this.rrOpen = rrOpen;
		this.pushRef = pushRef;

		this.optRef = optRef;
		this.optOpenRr = optOpenRr;
		this.oBaseRef = optBaseRef;

		if (rrOpen && pushRef) {
			throw new IllegalArgumentException("Can not be both a rrOpen and a pushRef event");
		}
	}

	@Override
	public boolean isReviewRequestOpen() {
		return rrOpen;
	}

	@Override
	public boolean isPushRef() {
		return pushRef;
	}

	/**
	 * present only on PR event: when this POJO is built, we forbid ourselves scanning PRs, hence this can not be
	 * inferred on push events. This behavior may change if a CodeProvider enables push events, listing open PR for
	 * impacted branch in the original event.
	 * 
	 * @return
	 */
	@Override
	public Optional<GitPrHeadRef> optOpenPr() {
		return optOpenRr;
	}

	@Override
	public Optional<GitRepoBranchSha1> optBaseRef() {
		return oBaseRef;
	}

	/**
	 * In case of a RR event, this holds the HEAD of the PR, not the base.
	 * 
	 * @return
	 */
	public Optional<GitRepoBranchSha1> optPushedRefOrRrHead() {
		return optRef;
	}
}
