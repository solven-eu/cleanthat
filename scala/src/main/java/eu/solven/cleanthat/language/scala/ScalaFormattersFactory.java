package eu.solven.cleanthat.language.scala;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.language.ASourceCodeFormatterFactory;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.LanguageProperties;
import eu.solven.cleanthat.language.scala.scalafix.ScalafixFormatter;
import eu.solven.cleanthat.language.scala.scalafix.ScalafixProperties;
import eu.solven.cleanthat.language.scala.scalafmt.ScalafmtProperties;
import eu.solven.cleanthat.language.scala.scalafmt.ScalafmtStyleEnforcer;
import eu.solven.pepper.collection.PepperMapHelper;

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

	@Override
	public Set<String> getFileExtentions() {
		return Set.of("scala", "sc");
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	public ILintFixer makeLintFixer(Map<String, ?> rawProcessor,
			ILanguageProperties languageProperties,
			ICodeProvider codeProvider) {
		String engine = PepperMapHelper.getRequiredString(rawProcessor, KEY_ENGINE);
		Map<String, ?> parameters = getParameters(rawProcessor);
		LOGGER.info("Processing: {}", engine);

		ILintFixerWithId processor;
		switch (engine) {
		case "scalafmt": {
			ScalafmtProperties properties = getObjectMapper().convertValue(parameters, ScalafmtProperties.class);
			processor = new ScalafmtStyleEnforcer(languageProperties.getSourceCode(), properties);

			break;
		}
		case "scalafix": {
			ScalafixProperties properties = getObjectMapper().convertValue(parameters, ScalafixProperties.class);
			processor = new ScalafixFormatter(languageProperties.getSourceCode(), properties);

			break;
		}
		default:
			throw new IllegalArgumentException("Unknown engine: " + engine);
		}

		return processor;
	}

	@Override
	public LanguageProperties makeDefaultProperties() {
		LanguageProperties languageProperties = new LanguageProperties();

		languageProperties.setLanguage(getLanguage());

		List<Map<String, ?>> processors = new ArrayList<>();

		{
			ScalafmtProperties engineParameters = new ScalafmtProperties();

			processors.add(ImmutableMap.<String, Object>builder()
					.put(KEY_ENGINE, "scalafmt")
					.put(KEY_PARAMETERS, engineParameters)
					.build());
		}
		{
			ScalafixProperties engineParameters = new ScalafixProperties();

			processors.add(ImmutableMap.<String, Object>builder()
					.put(KEY_ENGINE, "scalafix")
					.put(KEY_PARAMETERS, engineParameters)
					.build());
		}

		languageProperties.setProcessors(processors);

		return languageProperties;
	}
}
