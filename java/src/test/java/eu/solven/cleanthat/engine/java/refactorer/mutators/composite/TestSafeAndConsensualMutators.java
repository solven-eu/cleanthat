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
package eu.solven.cleanthat.engine.java.refactorer.mutators.composite;

import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.ATodoJavaParserMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.scanner.MutatorsScanner;

public class TestSafeAndConsensualMutators {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestSafeAndConsensualMutators.class);

	final JavaVersion last = JavaVersion.parse(IJdkVersionConstants.LAST);

	final CompositeMutator<IMutator> safeAndConsensual = new SafeAndConsensualMutators(last);
	final CompositeMutator<IMutator> safeButNotConsensual = new SafeButNotConsensualMutators(last);
	final CompositeMutator<IMutator> safeButControversial = new SafeButControversialMutators(last);

	@Before
	@After
	public void checkErrorCount() {
		Assertions.assertThat(MutatorsScanner.getErrorCount()).isZero();
	}

	@Test
	public void testIds() {
		Assertions.assertThat(safeAndConsensual.getIds()).doesNotContainAnyElementsOf(safeButNotConsensual.getIds());
		Assertions.assertThat(safeButNotConsensual.getIds()).doesNotContainAnyElementsOf(safeAndConsensual.getIds());
	}

	@Test
	public void testTodos() {
		Assertions.assertThat(safeAndConsensual.getUnderlyings()).noneMatch(ATodoJavaParserMutator.class::isInstance);
		Assertions.assertThat(safeButNotConsensual.getUnderlyings())
				.noneMatch(ATodoJavaParserMutator.class::isInstance);
		Assertions.assertThat(safeButControversial.getUnderlyings())
				.noneMatch(ATodoJavaParserMutator.class::isInstance);
	}

	@Test
	public void testScanComposite() {
		Set<String> safeAndConsensualIds = safeAndConsensual.getUnderlyingIds();
		Set<String> safeButNotConsensualIds = safeButNotConsensual.getUnderlyingIds();
		Set<String> safeButControversialIds = safeButControversial.getUnderlyingIds();

		// Check the intersection is empty
		Assertions.assertThat(safeAndConsensualIds).doesNotContainAnyElementsOf(safeButNotConsensualIds);
		Assertions.assertThat(safeButNotConsensualIds).doesNotContainAnyElementsOf(safeAndConsensualIds);

		Assertions.assertThat(safeAndConsensualIds).doesNotContainAnyElementsOf(safeButControversialIds);
		Assertions.assertThat(safeButControversialIds).doesNotContainAnyElementsOf(safeAndConsensualIds);

		Assertions.assertThat(safeButControversialIds).doesNotContainAnyElementsOf(safeButNotConsensualIds);
		Assertions.assertThat(safeButNotConsensualIds).doesNotContainAnyElementsOf(safeButControversialIds);

		List<IMutator> allSingle = new AllIncludingDraftSingleMutators(last).getUnderlyings();

		allSingle.stream()
				.filter(s -> Sets.intersection(s.getIds(), safeAndConsensualIds).isEmpty()
						&& Sets.intersection(s.getIds(), safeButNotConsensualIds).isEmpty()
						&& Sets.intersection(s.getIds(), safeButControversialIds).isEmpty())
				.forEach(notInComposite -> LOGGER.warn("{} is neither in {} nor in {} nor in {}",
						notInComposite.getClass().getSimpleName(),
						SafeAndConsensualMutators.class.getSimpleName(),
						SafeButNotConsensualMutators.class.getSimpleName(),
						SafeButControversialMutators.class.getSimpleName()));
	}
}
