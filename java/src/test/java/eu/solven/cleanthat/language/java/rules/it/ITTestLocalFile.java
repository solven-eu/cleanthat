package eu.solven.cleanthat.language.java.rules.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;

import eu.solven.cleanthat.java.mutators.JavaRulesMutatorProperties;
import eu.solven.cleanthat.java.mutators.RulesJavaMutator;
import eu.solven.cleanthat.language.LanguageProperties;
import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.rules.mutators.VariableEqualsConstant;

/**
 * This is useful to investigate a misbehavior over current project file
 * 
 * @author Benoit Lacelle
 *
 */
public class ITTestLocalFile {
	private static final Logger LOGGER = LoggerFactory.getLogger(ITTestLocalFile.class);

	final String path =
			// "./config/src/main/java/" + "eu.solven.cleanthat.config.ConfigHelpers".replace('.', '/') + ".java"
			"/Users/blacelle/workspace3/mitrust-datasharing"
					+ "/exec/retriever-poolip-master/src/test/java/io/mitrust/retriever/poolip/master/core/TestNodeResourceImpl.java";

	@Test
	public void testCleanLocalFile() throws IOException {
		File file = new File(
				// "../" +
				path);

		LOGGER.info("Process: {}", file);

		if (!file.isFile()) {
			throw new IllegalArgumentException("Can not read: " + file.getAbsolutePath());
		}

		String pathAsString = Files.readString(file.toPath());

		RulesJavaMutator rulesJavaMutator =
				new RulesJavaMutator(new LanguageProperties(), new JavaRulesMutatorProperties());

		CompilationUnit compilationUnit =
				rulesJavaMutator.parseRawCode(rulesJavaMutator.makeJavaParser(), pathAsString);

		// TODO Refactor to rely on RulesJavaMutator
		IClassTransformer rule = new VariableEqualsConstant();
		boolean changed = rule.walkNode(compilationUnit);

		if (!changed) {
			throw new IllegalArgumentException(rule + " did not change: " + file.getAbsolutePath());
		}

		DiffMatchPatch dmp = new DiffMatchPatch();
		String newAsString = compilationUnit.toString();

		// TODO We may need to reformat to have a nice diff
		// see eu.solven.cleanthat.java.mutators.RulesJavaMutator.fixJavaparserUnexpectedChanges(String, String)
		List<DiffMatchPatch.Diff> diff = dmp.diffMain(pathAsString, newAsString, false);
		diff.forEach(d -> LOGGER.info("{}", d));
	}

}
