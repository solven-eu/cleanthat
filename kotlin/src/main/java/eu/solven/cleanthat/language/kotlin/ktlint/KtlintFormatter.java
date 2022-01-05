package eu.solven.cleanthat.language.kotlin.ktlint;

import java.io.IOException;

import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.ISourceCodeProperties;

/**
 * Lint for Kotlin
 * 
 * See https://github.com/pinterest/ktlint
 *
 * @author Benoit Lacelle
 */
public class KtlintFormatter implements ILintFixerWithId {
	// private static final Logger LOGGER = LoggerFactory.getLogger(KtlintFormatter.class);

	final ISourceCodeProperties sourceCodeProperties;
	final KtlintProperties properties;

	public KtlintFormatter(ISourceCodeProperties sourceCodeProperties, KtlintProperties properties) {
		// https://github.com/diffplug/spotless/tree/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/kotlin
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public String getId() {
		return "ktlint";
	}

	@Override
	public String doFormat(String code, LineEnding ending) throws IOException {
		throw new UnsupportedOperationException("TODO");
	}

}
