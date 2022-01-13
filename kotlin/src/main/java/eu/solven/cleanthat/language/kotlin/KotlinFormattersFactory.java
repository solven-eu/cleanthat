package eu.solven.cleanthat.language.kotlin;

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
import eu.solven.cleanthat.language.kotlin.ktfmt.KtfmtProperties;
import eu.solven.cleanthat.language.kotlin.ktfmt.KtfmtStyleEnforcer;
import eu.solven.cleanthat.language.kotlin.ktlint.KtlintFormatter;
import eu.solven.cleanthat.language.kotlin.ktlint.KtlintProperties;

/**
 * Formatter for Kotlin
 *
 * @author Benoit Lacelle
 */
public class KotlinFormattersFactory extends ASourceCodeFormatterFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(KotlinFormattersFactory.class);

    public KotlinFormattersFactory(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public String getLanguage() {
        return "kotlin";
    }

    @Override
    public Set<String> getFileExtentions() {
        return Set.of(".kt", ".kts", ".ktm");
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
        case "ktfmt": {
            KtfmtProperties config = getObjectMapper().convertValue(parameters, KtfmtProperties.class);
            processor = new KtfmtStyleEnforcer(languageProperties.getSourceCode(), config);

            break;
        }
        case "ktlint": {
            KtlintProperties config = getObjectMapper().convertValue(parameters, KtlintProperties.class);
            processor = new KtlintFormatter(languageProperties.getSourceCode(), config);

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
            KtfmtProperties engineParameters = new KtfmtProperties();

            processors.add(ImmutableMap.<String, Object>builder()
                .put(KEY_ENGINE, "scalafmt")
                .put(KEY_PARAMETERS, engineParameters)
                .build());
        }
        {
            KtlintProperties engineParameters = new KtlintProperties();

            processors.add(ImmutableMap.<String, Object>builder()
                .put(KEY_ENGINE, "scalafix")
                .put(KEY_PARAMETERS, engineParameters)
                .build());
        }

        languageProperties.setProcessors(processors);

        return languageProperties;
    }
}
