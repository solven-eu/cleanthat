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
package eu.solven.cleanthat.engine.java.refactorer.it;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.SourceCodeProperties;
import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorerProperties;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LiteralsFirstInComparisons;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.SafeAndConsensualMutators;
import eu.solven.cleanthat.formatter.LineEnding;

/**
 * This is useful to investigate a misbehavior over current project file
 * 
 * @author Benoit Lacelle
 *
 */
public class ITTestRemoteFile {
	private static final Logger LOGGER = LoggerFactory.getLogger(ITTestRemoteFile.class);

	final String hashMapPath =
			"https://raw.githubusercontent.com/openjdk/jdk/master/src/java.base/share/classes/java/util/HashMap.java";
	final String literalsFirstCasesPath =
			"https://raw.githubusercontent.com/solven-eu/cleanthat/master/java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/LiteralsFirstInComparisonsCases.java";

	@Test
	public void testCleanLocalFile() throws IOException, URISyntaxException {
		processOneUrl(literalsFirstCasesPath);
	}

	private void processOneUrl(String path) throws IOException, MalformedURLException, URISyntaxException {
		LOGGER.info("Process: {}", path);

		String pathAsString =
				new String(ByteStreams.toByteArray(new URI(path).toURL().openStream()), StandardCharsets.UTF_8);

		JavaRefactorerProperties properties = new JavaRefactorerProperties();
		properties.setIncluded(
				Arrays.asList(SafeAndConsensualMutators.class.getName(), LiteralsFirstInComparisons.class.getName()));

		JavaRefactorer rulesJavaMutator = new JavaRefactorer(CleanthatEngineProperties.builder()
				.sourceCode(SourceCodeProperties.defaultRoot())
				.engineVersion(IJdkVersionConstants.LAST)
				.build(), properties);

		String cleaned = rulesJavaMutator.doFormat(pathAsString, LineEnding.KEEP);

		if (cleaned.equals(pathAsString)) {
			LOGGER.warn("Not a single change");
		} else {
			DiffMatchPatch dmp = new DiffMatchPatch();
			String newAsString = cleaned.toString();

			// TODO We may need to reformat to have a nice diff
			// see eu.solven.cleanthat.java.mutators.RulesJavaMutator.fixJavaparserUnexpectedChanges(String, String)
			List<DiffMatchPatch.Diff> diff = dmp.diffMain(pathAsString, newAsString, false)
					.stream()
					.filter(d -> d.operation != Operation.EQUAL)
					.collect(Collectors.toList());
			diff.forEach(d -> LOGGER.info("{}", d));
		}
	}

}
