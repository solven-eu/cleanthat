package eu.solven.cleanthat.language.scala.scalafix;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.ISourceCodeProperties;
import scalafix.interfaces.Scalafix;
import scalafix.interfaces.ScalafixError;
import scalafix.interfaces.ScalafixException;

/**
 * Formatter for Scala
 * 
 * See https://github.com/scalameta/scalafmt
 *
 * @author Benoit Lacelle
 */
public class ScalafixFormatter implements ILintFixerWithId {
	private static final Logger LOGGER = LoggerFactory.getLogger(ScalafixFormatter.class);

	final ISourceCodeProperties sourceCodeProperties;
	final ScalafixProperties properties;

	final Scalafix scalafix;

	public ScalafixFormatter(ISourceCodeProperties sourceCodeProperties, ScalafixProperties properties) {
		this.sourceCodeProperties = sourceCodeProperties;
		this.properties = properties;

		try {
			scalafix = Scalafix.classloadInstance(Thread.currentThread().getContextClassLoader());
		} catch (ScalafixException e) {
			throw new RuntimeException("Issue loading Scalafix", e);
		}
	}

	@Override
	public String getId() {
		return "scalafix";
	}

	@Override
	public String doFormat(String code, LineEnding ending) throws IOException {
		Path tmpPath;
		try {
			tmpPath = Files.createTempFile("cleanthat", "anything.scala");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		Files.write(tmpPath, code.getBytes(StandardCharsets.UTF_8));

		File file = tmpPath.toFile();
		LOGGER.info("File.deleteOnExit() over {}", file);
		file.deleteOnExit();

		ScalafixError[] errors = scalafix.newArguments()
				.withPaths(List.of(tmpPath))
				// https://scalacenter.github.io/scalafix/docs/rules/overview.html
				// 'RemoveUnused' requires SemanticDB: How can it be installed?
				// https://scalacenter.github.io/scalafix/docs/rules/NoValInForComprehension.html
				.withRules(List.of("NoValInForComprehension", "ProcedureSyntax"))
				.withScalacOptions(List.of("-Ywarn-unused"))
				.run();

		if (errors.length >= 1) {
			throw new IllegalStateException("Issue applying Scalafix: "
					+ Stream.of(errors).map(se -> se.name()).collect(Collectors.joining("&")));
		}
		String formattedCode = Files.readString(tmpPath);

		if (code.equals(formattedCode)) {
			// Return the original reference
			LOGGER.debug("No impact");
			return code;
		}

		return formattedCode;
	}

}
