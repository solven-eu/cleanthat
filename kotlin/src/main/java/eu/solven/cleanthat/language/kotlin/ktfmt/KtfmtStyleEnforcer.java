package eu.solven.cleanthat.language.kotlin.ktfmt;

import java.io.IOException;

import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.IStyleEnforcer;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.ISourceCodeProperties;

/**
 * {@link IStyleEnforcer} for Kotlin
 * 
 * See https://github.com/facebookincubator/ktfmt
 *
 * @author Benoit Lacelle
 */
public class KtfmtStyleEnforcer implements IStyleEnforcer, ILintFixerWithId {
	// private static final Logger LOGGER = LoggerFactory.getLogger(KtfmtStyleEnforcer.class);

	final ISourceCodeProperties sourceCodeProperties;
	final KtfmtProperties properties;

	public KtfmtStyleEnforcer(ISourceCodeProperties sourceCodeProperties, KtfmtProperties properties) {
		// https://github.com/diffplug/spotless/tree/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/kotlin
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public String getId() {
		return "ktfmt";
	}

	@Override
	public String doFormat(String code, LineEnding ending) throws IOException {
		throw new UnsupportedOperationException("TODO");
	}

}
