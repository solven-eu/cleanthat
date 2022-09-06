package eu.solven.cleanthat.language.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.language.ASourceCodeFormatterFactory;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.LanguageProperties;
import eu.solven.cleanthat.language.SourceCodeProperties;
import eu.solven.cleanthat.language.xml.ec4j.Ec4jXmlFormatter;
import eu.solven.cleanthat.language.xml.ec4j.Ec4jXmlFormatterProperties;
import eu.solven.cleanthat.language.xml.ekryd_sortpom.EcrydSortPomFormatter;
import eu.solven.cleanthat.language.xml.ekryd_sortpom.EcrydSortPomFormatterProperties;
import eu.solven.cleanthat.language.xml.javax.JavaxXmlFormatter;
import eu.solven.cleanthat.language.xml.javax.JavaxXmlFormatterProperties;
import eu.solven.cleanthat.language.xml.revelc.RevelcXmlFormatter;
import eu.solven.cleanthat.language.xml.revelc.RevelcXmlFormatterProperties;
import eu.solven.pepper.collection.PepperMapHelper;

/**
 * Formatter for XML and specialized-XMLs (e.g. pom.xml)
 *
 * @author Benoit Lacelle
 */
public class XmlFormattersFactory extends ASourceCodeFormatterFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(XmlFormattersFactory.class);

	public XmlFormattersFactory(ObjectMapper objectMapper) {
		super(objectMapper);
	}

	@Override
	public String getLanguage() {
		return "xml";
	}

	@Override
	public Set<String> getFileExtentions() {
		return Set.of("xml");
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	public ILintFixer makeLintFixer(Map<String, ?> rawProcessor,
			ILanguageProperties languageProperties,
			ICodeProvider codeProvider) {
		String engine = PepperMapHelper.getRequiredString(rawProcessor, KEY_ENGINE);
		Map<String, ?> rawParameters = getParameters(rawProcessor);
		LOGGER.info("Relying on: {}", engine);

		ILintFixerWithId processor;
		switch (engine) {
		case "revelc": {
			RevelcXmlFormatterProperties processorConfig =
					getObjectMapper().convertValue(rawParameters, RevelcXmlFormatterProperties.class);
			processor = new RevelcXmlFormatter(languageProperties.getSourceCode(), processorConfig);

			break;
		}
		case "javax": {
			JavaxXmlFormatterProperties processorConfig =
					getObjectMapper().convertValue(rawParameters, JavaxXmlFormatterProperties.class);
			processor = new JavaxXmlFormatter(languageProperties.getSourceCode(), processorConfig);

			break;
		}
		case "ec4j": {
			Ec4jXmlFormatterProperties processorConfig =
					getObjectMapper().convertValue(rawParameters, Ec4jXmlFormatterProperties.class);
			processor = new Ec4jXmlFormatter(languageProperties.getSourceCode(), processorConfig);

			break;
		}
		case "ecryd_sortpom": {
			EcrydSortPomFormatterProperties processorConfig =
					getObjectMapper().convertValue(rawParameters, EcrydSortPomFormatterProperties.class);
			processor = new EcrydSortPomFormatter(languageProperties.getSourceCode(), processorConfig);

			break;
		}
		default:
			throw new IllegalArgumentException("Unknown engine: " + engine);
		}

		if (!processor.getId().equals(engine)) {
			throw new IllegalStateException("Inconsistency: " + processor.getId() + " vs " + engine);
		}

		return processor;
	}

	@Override
	public LanguageProperties makeDefaultProperties() {
		LanguageProperties languageProperties = new LanguageProperties();

		languageProperties.setLanguage(getLanguage());

		List<Map<String, ?>> processors = new ArrayList<>();

		{
			RevelcXmlFormatterProperties engineParameters = new RevelcXmlFormatterProperties();
			RevelcXmlFormatter engine = new RevelcXmlFormatter(languageProperties.getSourceCode(), engineParameters);

			processors.add(ImmutableMap.<String, Object>builder()
					.put(KEY_ENGINE, engine.getId())
					.put(KEY_PARAMETERS, engineParameters)
					.build());
		}

		{
			EcrydSortPomFormatterProperties engineParameters = new EcrydSortPomFormatterProperties();
			EcrydSortPomFormatter engine =
					new EcrydSortPomFormatter(languageProperties.getSourceCode(), engineParameters);

			SourceCodeProperties ecrydSourceCode = new SourceCodeProperties();
			ecrydSourceCode.setIncludes(Arrays.asList("pom.xml"));

			processors.add(ImmutableMap.<String, Object>builder()
					.put(KEY_ENGINE, engine.getId())
					.put(KEY_PARAMETERS, engineParameters)
					.put(ConfigHelpers.KEY_SOURCE_CODE, getObjectMapper().convertValue(ecrydSourceCode, Map.class))
					.build());
		}

		languageProperties.setProcessors(processors);

		return languageProperties;
	}
}
