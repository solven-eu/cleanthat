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
package eu.solven.cleanthat.config.pojo;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.github.IGitRefsConstants;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

/**
 * The configuration of which ref are concerned by CleanThat or not.
 * 
 * Initial version: given a list of branches, we clean any PR/MR-head with one of these branches as base, and we open a
 * PR if we detect any of these branches is dirty
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties({ "branches" })
@Data
public final class CleanthatRefFilterProperties implements IGitRefsConstants {
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanthatRefFilterProperties.class);

	// By default, we clean a set of standard default branch names
	// https://docs.github.com/en/github/administering-a-repository/managing-branches-in-your-repository/changing-the-default-branch
	// 'main' is the new default branch as Github
	// https://github.blog/changelog/2020-10-01-the-default-branch-for-newly-created-repositories-is-now-main/
	public static final List<String> SIMPLE_DEFAULT_BRANCHES = ImmutableList.of("develop", "main", "master");

	/**
	 * 
	 * @return the fully qualified branches (i.e. heads refs)
	 */
	// https://stackoverflow.com/questions/51388545/how-to-override-lombok-setter-methods
	@Setter(AccessLevel.NONE)
	private List<String> protectedPatterns =
			SIMPLE_DEFAULT_BRANCHES.stream().map(s -> BRANCHES_PREFIX + s).collect(Collectors.toList());

	/**
	 * 
	 * @param protectedPatterns
	 *            the branches-patterns considered as protected. These branches should never be cleaned by themselves,
	 *            but only through RR.
	 * 
	 *            A pattern is prefixed by 'refs/heads' if not prefixed by 'refs/'.
	 * 
	 *            The pattern is later tested with java {@link Pattern#matches(String, CharSequence)}
	 */
	public void setProtectedPatterns(List<String> protectedPatterns) {
		protectedPatterns = protectedPatterns.stream().map(branch -> {
			if (!branch.startsWith(REFS_PREFIX)) {
				String qualifiedRef = BRANCHES_PREFIX + branch;
				LOGGER.debug("We qualify {} into {} (i.e. we supposed it is a branch name)", branch, qualifiedRef);
				return qualifiedRef;
			} else {
				if (!branch.startsWith(BRANCHES_PREFIX)) {
					LOGGER.warn("Unusual protected ref: {}", branch);
				}
				return branch;
			}
		}).distinct().collect(Collectors.toList());

		this.protectedPatterns = List.copyOf(protectedPatterns);
	}
}
