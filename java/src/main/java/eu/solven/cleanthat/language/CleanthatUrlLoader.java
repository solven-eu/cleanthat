package eu.solven.cleanthat.language;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import eu.solven.cleanthat.codeprovider.ICodeProvider;

/**
 * Knows how to load resource given a URL, with a flexible format handling 'classpath:' to load from the classpath, and
 * 'code:' to load from the repository.
 * 
 * @author Benoit Lacelle
 *
 */
public class CleanthatUrlLoader {
	public static final String PREFIX_CODE = "code:";

	protected CleanthatUrlLoader() {
		// hidden
	}

	public static Resource loadUrl(ICodeProvider codeProvider, String javaConfigFile) {
		Resource resource;
		if (javaConfigFile.startsWith(PREFIX_CODE)) {
			// This is inspired by Spring 'classpath:'
			// Here, it is used to load files from current repository
			String path = javaConfigFile.substring(PREFIX_CODE.length());
			Optional<String> optContent;
			try {
				optContent = codeProvider.loadContentForPath(path);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			if (optContent.isEmpty()) {
				throw new IllegalStateException(
						"There is no content at: " + path + " (provider: " + codeProvider + ")");
			}
			resource = new ByteArrayResource(optContent.get().getBytes(StandardCharsets.UTF_8), path);
		} else {
			resource = new DefaultResourceLoader().getResource(javaConfigFile);
		}

		return resource;
	}

}
