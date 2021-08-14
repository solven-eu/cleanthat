package eu.solven.cleanthat.language.nodejs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prepare the work to enable execution of Node, in order to enable execution of Prettier
 * 
 * @author Benoit Lacelle
 *
 */
public class NodeJsInstallAndExecute {
	private static final Logger LOGGER = LoggerFactory.getLogger(NodeJsInstallAndExecute.class);
	private static final int BUFFER_SIZE = 1024;

	protected NodeJsInstallAndExecute() {
		// hidden
	}

	// See
	// https://github.com/eirslett/frontend-maven-plugin/blob/master/frontend-maven-plugin/src/main/java/com/github/eirslett/maven/plugins/frontend/mojo/InstallNodeAndNpmMojo.java
	public static void main(String[] args) throws IOException {
		String url = "https://nodejs.org/dist/v14.17.5/node-v14.17.5-darwin-x64.tar.gz";
		Path urlAsPath = Paths.get(url);

		URL aUrl = new URL(url);

		String filename = urlAsPath.getFileName().toString();
		Path tmpPath = Files.createTempFile("cleanthat-", filename);

		LOGGER.info("File.deleteOnExit over {}", tmpPath);
		tmpPath.toFile().deleteOnExit();

		// TODO Why not Untar directly from remote?
		LOGGER.info("Downloading {} into {}", url, tmpPath);
		try (InputStream is = aUrl.openStream()) {
			Files.copy(is, tmpPath, StandardCopyOption.REPLACE_EXISTING);
		}
		LOGGER.info("Downloaded {} into {}", url, tmpPath);

		String folderName;
		if (url.endsWith(".tar.gz")) {
			String tmpFilename = tmpPath.getFileName().toString();
			folderName = tmpFilename.substring(0, tmpFilename.length() - ".tar.gz".length());
		} else {
			throw new IllegalArgumentException("Not-managed extention: " + filename);
		}
		Path extractedPath = tmpPath.resolveSibling(folderName);
		extractedPath.toFile().mkdir();
		LOGGER.info("Extracting {} into {}", url, extractedPath);
		try (InputStream tarGzIs = Files.newInputStream(tmpPath)) {
			extractTarGZ(extractedPath, tarGzIs);
		}
		LOGGER.info("Extracted {} into {}", url, extractedPath);

		File[] extractedFiles = extractedPath.toFile().listFiles();
		if (extractedFiles.length != 1) {
			throw new IllegalArgumentException("Expected a single folder but got N-files: " + extractedFiles.length);
		}

		Path nodePath = extractedFiles[0].toPath().resolve("bin").resolve("node");
		if (!nodePath.toFile().setExecutable(true)) {
			throw new IllegalStateException("Issue allowing executable permission over " + nodePath);
		}
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.command(nodePath.toString(), "--version");
		Process process = processBuilder.start();

		LOGGER.info("Waiting for completion");
		try {
			process.onExit().get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		LOGGER.info("Completed");

		String output = new String(process.getInputStream().readAllBytes());
		LOGGER.info("Output: {}{}", System.lineSeparator(), output);
	}

	// https://stackoverflow.com/questions/7128171/how-to-compress-decompress-tar-gz-files-in-java
	@SuppressWarnings("PMD.AssignmentInOperand")
	public static void extractTarGZ(Path output, InputStream in) throws IOException {
		try (GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(in);
				TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {
			TarArchiveEntry entry;

			while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
				Path f = output.resolve(entry.getName());

				// If the entry is a directory, create the directory.
				if (entry.isDirectory()) {
					boolean created = f.toFile().mkdir();
					if (!created) {
						LOGGER.info("Unable to create directory '{}', during extraction of archive contents.",
								f.toAbsolutePath());
					}
				} else {
					try (OutputStream fos = Files.newOutputStream(f);
							BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE)) {
						int count;
						byte[] data = new byte[BUFFER_SIZE];
						while ((count = tarIn.read(data, 0, BUFFER_SIZE)) != -1) {
							dest.write(data, 0, count);
						}
					}
				}
			}

			LOGGER.debug("Untar completed successfully!");
		}
	}
}
