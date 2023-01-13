package eu.solven.cleanthat.language.spotless;

import java.io.IOException;

import com.diffplug.spotless.Formatter;

import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.ISourceCodeProperties;
import eu.solven.cleanthat.spotless.ExecuteSpotless;
import eu.solven.cleanthat.spotless.FormatterFactory;

public class SpotlessLintFixer implements ILintFixerWithId {
	final ISourceCodeProperties sourceCode;
	final SpotlessCleanthatProperties processorConfig;

	public SpotlessLintFixer(ISourceCodeProperties sourceCode, SpotlessCleanthatProperties processorConfig) {
		this.sourceCode = sourceCode;
		this.processorConfig = processorConfig;
	}

	@Override
	public String doFormat(String code, LineEnding ending) throws IOException {
		Formatter formatter = new FormatterFactory(codeProvider, includes, excludes).makeFormatter(spotlessProperties, provisionner);
		return new ExecuteSpotless(formatter);
	}

	@Override
	public String getId() {
		return "spotless";
	}

}
