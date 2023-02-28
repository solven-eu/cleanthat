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
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.scanner.MutatorsScanner;

public class TestSafeAndConsensualMutators {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestSafeAndConsensualMutators.class);

	final JavaVersion last = JavaVersion.parse(IJdkVersionConstants.LAST);

	@Before
	@After
	public void checkErrorCount() {
		Assertions.assertThat(MutatorsScanner.getErrorCount()).isZero();
	}

	@Test
	public void testIds() {
		var safeAndConsensual = new SafeAndConsensualMutators(last);
		var safeButNotConsensual = new SafeButNotAndConsensualMutators(last);

		Assertions.assertThat(safeAndConsensual.getIds()).doesNotContainAnyElementsOf(safeButNotConsensual.getIds());
		Assertions.assertThat(safeButNotConsensual.getIds()).doesNotContainAnyElementsOf(safeAndConsensual.getIds());
	}

	@Test
	public void testScanComposite() {
		Set<String> safeAndConsensual = new SafeAndConsensualMutators(last).getUnderlyingIds();
		Set<String> safeButNotConsensual = new SafeButNotAndConsensualMutators(last).getUnderlyingIds();

		// Check the intersection is empty
		Assertions.assertThat(safeAndConsensual).doesNotContainAnyElementsOf(safeButNotConsensual);
		Assertions.assertThat(safeButNotConsensual).doesNotContainAnyElementsOf(safeAndConsensual);

		List<IMutator> allSingle = new AllIncludingDraftSingleMutators(last).getUnderlyings();

		allSingle.stream()
				.filter(s -> Sets.intersection(s.getIds(), safeAndConsensual).isEmpty()
						&& Sets.intersection(s.getIds(), safeButNotConsensual).isEmpty())
				.forEach(notInComposite -> {
					LOGGER.warn("{} is neither in {} nor in {}",
							notInComposite.getClass().getSimpleName(),
							SafeAndConsensualMutators.class.getSimpleName(),
							SafeButNotAndConsensualMutators.class.getSimpleName());
				});
	}
}
