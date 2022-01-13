package eu.solven.cleanthat.language.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.language.ASourceCodeFormatterFactory;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.LanguageProperties;
import eu.solven.cleanthat.language.xml.ec4j.Ec4jXmlFormatter;
import eu.solven.cleanthat.language.xml.ec4j.Ec4jXmlFormatterProperties;
import eu.solven.cleanthat.language.xml.javax.JavaxXmlFormatter;
import eu.solven.cleanthat.language.xml.javax.JavaxXmlFormatterProperties;
import eu.solven.cleanthat.language.xml.revelc.RevelcXmlFormatter;
import eu.solven.cleanthat.language.xml.revelc.RevelcXmlFormatterProperties;

/**
 * Formatter for Json
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
        Map<String, Object> parameters = getParameters(rawProcessor);
        LOGGER.info("Processing: {}", engine);

        ILintFixerWithId processor;
        switch (engine) {
        case "revelc": {
            RevelcXmlFormatterProperties processorConfig =
                getObjectMapper().convertValue(parameters, RevelcXmlFormatterProperties.class);
            processor = new RevelcXmlFormatter(languageProperties.getSourceCode(), processorConfig);

            break;
        }
        case "javax": {
            JavaxXmlFormatterProperties processorConfig =
                getObjectMapper().convertValue(parameters, JavaxXmlFormatterProperties.class);
            processor = new JavaxXmlFormatter(languageProperties.getSourceCode(), processorConfig);

            break;
        }
        case "ec4j": {
            Ec4jXmlFormatterProperties processorConfig =
                getObjectMapper().convertValue(parameters, Ec4jXmlFormatterProperties.class);
            processor = new Ec4jXmlFormatter(languageProperties.getSourceCode(), processorConfig);

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

        languageProperties.setProcessors(processors);

        return languageProperties;
    }
}
