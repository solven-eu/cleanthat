package eu.solven.cleanthat.code_provider.github.code_provider;

import java.io.IOException;
import java.io.InputStreamReader;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import eu.solven.cleanthat.codeprovider.ICodeProvider;

/**
 * An {@link ICodeProvider} for Github code. Sub-classes manages PR, ref/branches/...
 *
 * @author Benoit Lacelle
 */
public abstract class AGithubCodeProvider implements ICodeProvider {
	// private static final Logger LOGGER = LoggerFactory.getLogger(AGithubCodeProvider.class);

	public static String loadContent(GHRepository repository, String filename, String sha1) throws IOException {
		GHContent content = repository.getFileContent(filename, sha1);
		String asString;
		try (InputStreamReader reader = new InputStreamReader(content.read(), Charsets.UTF_8)) {
			asString = CharStreams.toString(reader);
		}
		return asString;
	}
}
