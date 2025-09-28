/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.junit.Test;

import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorerProperties;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LiteralsFirstInComparisons;
import lombok.extern.slf4j.Slf4j;

/**
 * This is useful to investigate a misbehavior over current project file
 * 
 * @author Benoit Lacelle
 *
 */
@Slf4j
public class ITTestLocalFile {

	final String path =
			// "./config/src/main/java/" + "eu.solven.cleanthat.config.ConfigHelpers".replace('.', '/') + ".java"
			"/Users/blacelle/workspace3/mitrust-datasharing"
					+ "/exec/retriever-poolip-master/src/test/java/io/mitrust/retriever/poolip/master/core/TestNodeResourceImpl.java";

	@Test
	public void testCleanLocalFile() throws IOException {
		var file = new File(
				// "../" +
				path);

		LOGGER.info("Process: {}", file);

		if (!file.isFile()) {
			throw new IllegalArgumentException("Can not read: " + file.getAbsolutePath());
		}

		var pathAsString = Files.readString(file.toPath());

		var rulesJavaMutator =
				new JavaRefactorer(CleanthatEngineProperties.builder().build(), new JavaRefactorerProperties());

		// TODO Refactor to rely on RulesJavaMutator
		IJavaparserAstMutator rule = new LiteralsFirstInComparisons();

		var optCompilationUnit =
				rulesJavaMutator.parseSourceCode(JavaRefactorer.makeDefaultJavaParser(rule.isJreOnly()), pathAsString);

		var changed = rule.walkAstHasChanged(optCompilationUnit.get());

		if (!changed) {
			throw new IllegalArgumentException(rule + " did not change: " + file.getAbsolutePath());
		}

		DiffMatchPatch dmp = new DiffMatchPatch();
		var newAsString = optCompilationUnit.toString();

		// TODO We may need to reformat to have a nice diff
		// see eu.solven.cleanthat.java.mutators.RulesJavaMutator.fixJavaparserUnexpectedChanges(String, String)
		List<DiffMatchPatch.Diff> diff = dmp.diffMain(pathAsString, newAsString, false);
		diff.forEach(d -> LOGGER.info("{}", d));
	}

}
