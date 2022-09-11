package eu.solven.cleanthat.language.bash.beautysh;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.ISourceCodeProperties;

/**
 * Formatter for sh/bash scripts
 *
 * @author Benoit Lacelle
 */
// https://github.com/lovesegfault/beautysh
// TODO Unclear how to efficiently/simply execute Python from Java
// This should be pushed as its own lambda function?
public class BeautyshShFormatter implements ILintFixerWithId {
	private static final Logger LOGGER = LoggerFactory.getLogger(BeautyshShFormatter.class);

	final ISourceCodeProperties sourceCodeProperties;
	final BeautyshFormatterProperties properties;

	public BeautyshShFormatter(ISourceCodeProperties sourceCodeProperties, BeautyshFormatterProperties properties) {
		this.sourceCodeProperties = sourceCodeProperties;
		this.properties = properties;
	}

	@Override
	public String getId() {
		return "beautysh";
	}

	@Override
	public String doFormat(String code, LineEnding ending) throws IOException {
		// String eol = LineEnding.getOrGuess(ending, () -> code);

		String formattedCode;

		// https://bugs.jython.org/issue2355
		{
			Properties props = new Properties();
			// if we do the next line, then it works, but os import fails
			props.put("python.import.site", "false");
			Properties preprops = System.getProperties();
			PythonInterpreter.initialize(preprops, props, new String[0]);
		}

		// ScriptEngineManagerUtils.listEngines();

		try (PythonInterpreter pyInterp = new PythonInterpreter()) {
			StringWriter output = new StringWriter();
			pyInterp.setOut(output);

			pyInterp.exec("import ensurepip");
			pyInterp.exec("pip install beautysh");

			formattedCode = output.toString();
		}

		if (code.equals(formattedCode)) {
			// Return the original reference
			LOGGER.debug("No impact");
			return code;
		}

		return formattedCode;
	}

}
