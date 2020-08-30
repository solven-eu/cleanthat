package eu.solven.cleanthat.formatter.eclipse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.github.CleanthatJavaProcessorProperties;
import eu.solven.cleanthat.github.ILanguageProperties;
import eu.solven.cleanthat.github.IStringFormatter;
import eu.solven.cleanthat.java.imports.JavaRevelcImportsCleaner;
import eu.solven.cleanthat.java.imports.JavaRevelcImportsCleanerProperties;
import eu.solven.cleanthat.java.mutators.RulesJavaMutator;

/**
 * Formatter for Java
 * 
 * @author Benoit Lacelle
 *
 */
public class JavaFormatter implements IStringFormatter {
	private static final Logger LOGGER = LoggerFactory.getLogger(JavaFormatter.class);

	final ObjectMapper objectmapper;

	public JavaFormatter(ObjectMapper objectmapper) {
		this.objectmapper = objectmapper;
	}

	@Override
	public String format(ILanguageProperties properties, String asString) throws IOException {
		LineEnding eolToApply;

		if (properties.getLineEnding() == LineEnding.KEEP) {
			eolToApply = LineEnding.determineLineEnding(asString);
		} else {
			eolToApply = properties.getLineEnding();
		}

		AtomicReference<String> output = new AtomicReference<>(asString);
		properties.getProcessors().forEach(pAsMap -> {
			ICodeProcessor processor;
			String engine = PepperMapHelper.getRequiredString(pAsMap, "engine");

			if ("eclipse_formatter".equals(engine)) {
				CleanthatJavaProcessorProperties processorConfig =
						objectmapper.convertValue(pAsMap, CleanthatJavaProcessorProperties.class);
				processor = new EclipseJavaFormatter(processorConfig);
			} else if ("revelc_imports".equals(engine)) {
				JavaRevelcImportsCleanerProperties processorConfig =
						objectmapper.convertValue(pAsMap, JavaRevelcImportsCleanerProperties.class);

				processor = new JavaRevelcImportsCleaner(processorConfig);
			} else if ("rules".equals(engine)) {
				CleanthatJavaProcessorProperties processorConfig =
						objectmapper.convertValue(pAsMap, CleanthatJavaProcessorProperties.class);
				processor = new RulesJavaMutator(processorConfig);
			} else {
				throw new IllegalArgumentException("Unknown engine: " + engine);
			}

			try {
				output.set(processor.doFormat(output.get(), eolToApply));
			} catch (IOException | RuntimeException e) {
				// Log and move to next processor
				LOGGER.warn("Issue with " + processor, e);
			}
		});

		return output.getAcquire();
	}

}
