package eu.solven.cleanthat.language.scala.scalafmt;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import org.scalafmt.interfaces.Scalafmt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.base.Suppliers;
import com.google.common.io.ByteStreams;

import eu.solven.cleanthat.formatter.IStyleEnforcer;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.ISourceCodeProperties;

/**
 * {@link IStyleEnforcer} for Scala
 * 
 * See https://github.com/scalameta/scalafmt
 *
 * @author Benoit Lacelle
 */
public class ScalafmtStyleEnforcer implements IStyleEnforcer {
	private static final Logger LOGGER = LoggerFactory.getLogger(ScalafmtStyleEnforcer.class);

	final ISourceCodeProperties sourceCodeProperties;
	final ScalafmtProperties properties;

	final Scalafmt scalaFmt;

	final Supplier<Path> configPath;

	public ScalafmtStyleEnforcer(ISourceCodeProperties sourceCodeProperties, ScalafmtProperties properties) {
		this.sourceCodeProperties = sourceCodeProperties;
		this.properties = properties;

		scalaFmt = Scalafmt.create(Thread.currentThread().getContextClassLoader());

		configPath = Suppliers.memoize(() -> {
			Path tmpPath;
			try {
				tmpPath = Files.createTempFile("cleanthat", ".scalafmt.conf");
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			// File file = tmpPath.toFile();
			try (OutputStream fis = Files.newOutputStream(tmpPath)) {
				ByteStreams.copy(new ClassPathResource("/scala/scalafmt.conf").getInputStream(), fis);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			LOGGER.info("File.deleteOnExit() over {}", tmpPath);
			tmpPath.toFile().deleteOnExit();

			return tmpPath;
		});
	}

	@Override
	public String doFormat(String code, LineEnding ending) throws IOException {
		String formattedCode = scalaFmt.format(configPath.get(), Paths.get("/how/is/this/useful.scala"), code);

		if (code.equals(formattedCode)) {
			// Return the original reference
			LOGGER.debug("No impact");
			return code;
		}

		return formattedCode;
	}

}
