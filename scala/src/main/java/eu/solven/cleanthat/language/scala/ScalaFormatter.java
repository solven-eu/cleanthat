package eu.solven.cleanthat.language.scala;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.formatter.ALanguageFormatter;
import eu.solven.cleanthat.formatter.ISourceCodeFormatter;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.IStringFormatter;
import eu.solven.cleanthat.language.scala.scalafix.ScalafixFormatter;
import eu.solven.cleanthat.language.scala.scalafix.ScalafixProperties;
import eu.solven.cleanthat.language.scala.scalafmt.ScalafmtFormatter;
import eu.solven.cleanthat.language.scala.scalafmt.ScalafmtProperties;

/**
 * Formatter for Scala
 *
 * @author Benoit Lacelle
 */
public class ScalaFormatter extends ALanguageFormatter implements IStringFormatter {
	private static final Logger LOGGER = LoggerFactory.getLogger(ScalaFormatter.class);

	public ScalaFormatter(ObjectMapper objectMapper) {
		super(objectMapper);
	}

	@Override
	public String getLanguage() {
		return "scala";
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	protected ISourceCodeFormatter makeFormatter(Map<String, ?> rawProcessor, ILanguageProperties languageProperties) {
		String engine = PepperMapHelper.getRequiredString(rawProcessor, "engine");
		Map<String, Object> parameters = PepperMapHelper.getAs(rawProcessor, "parameters");
		if (parameters == null) {
			// Some engine takes no parameter
			parameters = Map.of();
		}
		LOGGER.info("Processing: {}", engine);

		ISourceCodeFormatter processor;
		switch (engine) {
		case "scalafmt":
			ScalafmtProperties scalafmtConfig = getObjectMapper().convertValue(parameters, ScalafmtProperties.class);
			processor = new ScalafmtFormatter(languageProperties.getSourceCodeProperties(), scalafmtConfig);

			break;
		case "scalafix":
			ScalafixProperties scalafixConfig = getObjectMapper().convertValue(parameters, ScalafixProperties.class);
			processor = new ScalafixFormatter(languageProperties.getSourceCodeProperties(), scalafixConfig);

			break;

		default:
			throw new IllegalArgumentException("Unknown engine: " + engine);
		}

		return processor;
	}
}
