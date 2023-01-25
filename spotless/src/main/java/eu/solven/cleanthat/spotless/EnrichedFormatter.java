package eu.solven.cleanthat.spotless;

import com.diffplug.spotless.Formatter;

public class EnrichedFormatter {
	final AFormatterStepFactory formatterStepFactory;
	final Formatter formatter;

	public EnrichedFormatter(AFormatterStepFactory formatterStepFactory, Formatter formatter) {
		this.formatterStepFactory = formatterStepFactory;
		this.formatter = formatter;
	}

}
