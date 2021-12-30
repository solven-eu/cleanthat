package eu.solven.cleanthat.codeprovider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.config.ConfigHelpers;

/**
 * Helpers working for any {@link ICodeProvider}
 * 
 * @author Benoit Lacelle
 *
 */
public class CodeProviderHelpers {

	public static final String FILENAME_CLEANTHAT_YAML = "cleanthat.yaml";
	public static final String FILENAME_CLEANTHAT_YML = "cleanthat.yml";
	public static final String FILENAME_CLEANTHAT_JSON = "cleanthat.json";

	public static final List<String> FILENAMES_CLEANTHAT =
			Arrays.asList(FILENAME_CLEANTHAT_YAML, FILENAME_CLEANTHAT_YML, FILENAME_CLEANTHAT_JSON);

	public static final List<String> PATH_CLEANTHAT =
			FILENAMES_CLEANTHAT.stream().map(s -> "/" + s).collect(Collectors.toList());

	// public static final String PATH_CLEANTHAT_JSON = "/" + FILENAME_CLEANTHAT_JSON;

	protected Collection<ObjectMapper> objectMappers;

	public CodeProviderHelpers(Collection<ObjectMapper> objectMappers) {
		this.objectMappers = objectMappers;
	}

	// TODO Get the merged configuration head -> base
	// It will enable cleaning a PR given the configuration of the base branch
	public Optional<Map<String, ?>> unsafeConfig(ICodeProvider codeProvider) {
		Optional<String> optContent;
		AtomicReference<String> name = new AtomicReference<>();
		optContent = PATH_CLEANTHAT.stream().map(p -> {
			try {
				return codeProvider.loadContentForPath(p);
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			} finally {
				name.set(p);
			}
		}).flatMap(Optional::stream).findFirst();

		ObjectMapper objectMapper;
		if (name.get().endsWith("json")) {
			objectMapper = ConfigHelpers.getJson(objectMappers);
		} else {
			objectMapper = ConfigHelpers.getYaml(objectMappers);
		}

		return optContent.map(content -> {
			try {
				return objectMapper.readValue(content, Map.class);
			} catch (JsonProcessingException e) {
				throw new IllegalArgumentException("Invalid json", e);
			}
		});
	}

	public static File pathToConfig(Path localFolder) {
		// String overridePackage = "/eu.solven/mitrust-datasharing/";
		// pathToConfig =
		// new ClassPathResource("/overrides" + overridePackage + GithubPullRequestCleaner.FILENAME_CLEANTHAT_JSON)
		// .getFile();
		return CodeProviderHelpers.FILENAMES_CLEANTHAT.stream()
				.map(s -> localFolder.resolve(s).toFile())
				.filter(File::exists)
				.findAny()
				.get();
	}

}
