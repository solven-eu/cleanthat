package eu.solven.cleanthat.language.spotless;

import java.io.IOException;

import com.diffplug.spotless.Formatter;

import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.spotless.ExecuteSpotless;

public class SpotlessLintFixer implements ILintFixerWithId {
	final Formatter formatter;

	public SpotlessLintFixer(Formatter formatter) {
		this.formatter = formatter;
	}

	@Override
	public String doFormat(String code, LineEnding ending) throws IOException {
		return new ExecuteSpotless(formatter).doStuff(code, code);
	}

	@Override
	public String getId() {
		return "spotless";
	}

}
