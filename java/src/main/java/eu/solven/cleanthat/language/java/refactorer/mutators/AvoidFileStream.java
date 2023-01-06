package eu.solven.cleanthat.language.java.refactorer.mutators;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;

import eu.solven.cleanthat.language.java.IJdkVersionConstants;
import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.rules.ATodoJavaParserRule;

/**
 * Avoid use of {@link FileInputStream}, {@link FileOutputStream}, {@link FileReader} and {@link FileWriter}
 * 
 * @author Benoit Lacelle
 *
 */
@Deprecated(since = "Not-ready")
public class AvoidFileStream extends ATodoJavaParserRule implements IClassTransformer {
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_7;
	}

	@Override
	public String sonarUrl() {
		return "";
	}

	@Override
	public String pmdUrl() {
		// PMD.AvoidFileStream
		return "https://pmd.github.io/latest/pmd_rules_java_performance.html#avoidfilestream";
	}

}
