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
package eu.solven.cleanthat.github;

import eu.solven.cleanthat.config.pojo.CleanthatRefFilterProperties;

/**
 * Constants related to Cleanthat Git references
 * 
 * @author Benoit Lacelle
 *
 */
public interface ICleanthatGitRefsConstants extends IGitRefsConstants {

	String REF_DOMAIN_CLEANTHAT = "cleanthat";
	String REF_DOMAIN_CLEANTHAT_WITH_TRAILING_SLASH = REF_DOMAIN_CLEANTHAT + "/";

	String PREFIX_REF_CLEANTHAT =
			CleanthatRefFilterProperties.BRANCHES_PREFIX + REF_DOMAIN_CLEANTHAT_WITH_TRAILING_SLASH;

	// 2023-01: Renamed from 'cleanthat/configure' to 'cleanthat/configure_v2' as the configuration change
	// It enables handling easily repository with a PR open a long-time ago, with old configuration
	@Deprecated
	String REF_NAME_CONFIGURE_V1 = PREFIX_REF_CLEANTHAT + "configure";
	String REF_NAME_CONFIGURE = PREFIX_REF_CLEANTHAT + "configure_v2";

	// Used to clean a protected branch: we'll clean it in a dedicated branch
	String PREFIX_REF_CLEANTHAT_TMPHEAD = PREFIX_REF_CLEANTHAT + "headfor-";

	// Used for tests from ITs
	String PREFIX_REF_CLEANTHAT_MANUAL = PREFIX_REF_CLEANTHAT + "headfor-manual-";
}
