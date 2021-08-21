package eu.solven.cleanthat.language.scala;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.language.ASourceCodeFormatterFactory;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.scala.scalafix.ScalafixFormatter;
import eu.solven.cleanthat.language.scala.scalafix.ScalafixProperties;
import eu.solven.cleanthat.language.scala.scalafmt.ScalafmtProperties;
import eu.solven.cleanthat.language.scala.scalafmt.ScalafmtStyleEnforcer;

/**
 * Formatter for Scala
 *
 * @author Benoit Lacelle
 */
public class ScalaFormattersFactory extends ASourceCodeFormatterFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(ScalaFormattersFactory.class);

	public ScalaFormattersFactory(ObjectMapper objectMapper) {
		super(objectMapper);
	}

	@Override
	public String getLanguage() {
		return "scala";
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	public ILintFixer makeLintFixer(Map<String, ?> rawProcessor,
			ILanguageProperties languageProperties,
			ICodeProvider codeProvider) {
		String engine = PepperMapHelper.getRequiredString(rawProcessor, "engine");
		// Some engine takes no parameter
		Map<String, ?> parameters =
				PepperMapHelper.<Map<String, ?>>getOptionalAs(rawProcessor, "parameters").orElse(Map.of());
		LOGGER.info("Processing: {}", engine);

		ILintFixer processor;
		switch (engine) {
		case "scalafmt":
			ScalafmtProperties scalafmtConfig = getObjectMapper().convertValue(parameters, ScalafmtProperties.class);
			processor = new ScalafmtStyleEnforcer(languageProperties.getSourceCode(), scalafmtConfig);

			break;
		case "scalafix":
			ScalafixProperties scalafixConfig = getObjectMapper().convertValue(parameters, ScalafixProperties.class);
			processor = new ScalafixFormatter(languageProperties.getSourceCode(), scalafixConfig);

			break;

		default:
			throw new IllegalArgumentException("Unknown engine: " + engine);
		}

		return processor;
	}
}
