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
package eu.solven.cleanthat.code_provider.github.event;

import com.google.common.base.Ascii;
import eu.solven.cleanthat.config.IGitService;
import java.io.IOException;
import java.util.Optional;
import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCheckRun.Status;
import org.kohsuke.github.GHCheckRunBuilder;
import org.kohsuke.github.GHCheckRunBuilder.Action;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * manages CheckRun in GitHub API
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubCheckRunManager {
	private static final int LIMIT_IDENTIFIER = 20;
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubCheckRunManager.class);
	public static final String PERMISSION_CHECKS = "checks";

	final IGitService gitService;

	public GithubCheckRunManager(IGitService gitService) {
		this.gitService = gitService;
	}

	// https://docs.github.com/fr/rest/checks/runs?apiVersion=2022-11-28#create-a-check-run
	public Optional<GHCheckRun> createCheckRun(GithubAndToken githubAuthAsInst,
			GHRepository baseRepo,
			String sha1,
			String eventKey) {
		if (GHPermissionType.WRITE == githubAuthAsInst.getPermissions().get(PERMISSION_CHECKS)) {
			// https://docs.github.com/en/developers/webhooks-and-events/webhooks/webhook-events-and-payloads#check_run
			// https://docs.github.com/en/rest/reference/checks#runs
			// https://docs.github.com/en/rest/reference/permissions-required-for-github-apps#permission-on-checks

			// Limitted to 20 characters
			String identifier = Ascii.truncate(gitService.getSha1(), LIMIT_IDENTIFIER, "");
			// Limitted to 40 characters
			String description = "CleanThat cleaning/refactoring";
			GHCheckRunBuilder checkRunBuilder = baseRepo.createCheckRun("CleanThat", sha1)
					.withExternalID(eventKey)
					.withDetailsURL("https://github.com/solven-eu/cleanthat?event=" + eventKey)
					.add(new Action("CleanThat", description, identifier));
			try {
				GHCheckRun checkRun = checkRunBuilder.withStatus(Status.IN_PROGRESS).create();

				return Optional.of(checkRun);
			} catch (IOException e) {
				// https://github.community/t/resource-not-accessible-when-trying-to-read-write-checkrun/193493
				LOGGER.warn("Issue creating the CheckRun", e);
				return Optional.empty();
			}
		} else {
			// Invite users to go into:
			// https://github.com/organizations/solven-eu/settings/installations/9086720
			LOGGER.warn("We are not allowed to write checks (permissions=checks:write)");
			return Optional.empty();
		}
	}
}
