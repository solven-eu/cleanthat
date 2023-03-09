/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
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
	@Deprecated
	public static final String KEY_HEAD_TO_CLEAN = "refToClean";

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
