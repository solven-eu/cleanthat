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

/**
 * Constants related to Git standard
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGitRefsConstants {

	// https://git-scm.com/book/en/v2/Git-Internals-Git-References
	// In a local Git repository, refs are available at 'refs/heads/XXX'
	String REFS_PREFIX = "refs/";
	String BRANCHES_PREFIX = REFS_PREFIX + "heads/";

	// https://stackoverflow.com/questions/1526471/git-difference-between-branchname-and-refs-heads-branchname
	String REF_REMOTES = REFS_PREFIX + "remotes/";
	String REF_TAGS = REFS_PREFIX + "tags/";

	String SHA1_CLEANTHAT_UP_TO_REF_ROOT = "CLEANTHAT_UP_TO_REF_ROOT";
}
