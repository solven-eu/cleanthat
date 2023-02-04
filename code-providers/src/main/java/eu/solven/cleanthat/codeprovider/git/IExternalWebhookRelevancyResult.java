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
