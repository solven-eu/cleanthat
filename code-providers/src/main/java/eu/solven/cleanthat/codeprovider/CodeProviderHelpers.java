package eu.solven.cleanthat.codeprovider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import eu.solven.cleanthat.config.ConfigHelpers;

/**
 * Helpers working for any {@link ICodeProvider}
 * 
 * @author Benoit Lacelle
 *
 */
public class CodeProviderHelpers {
	private static final Logger LOGGER = LoggerFactory.getLogger(CodeProviderHelpers.class);

	public static final String FILENAME_CLEANTHAT_FOLDER = ".cleanthat";

	public static final String FILENAME_CLEANTHAT_YAML = "cleanthat.yaml";
	public static final String FILENAME_CLEANTHAT_YML = "cleanthat.yml";
	public static final String FILENAME_CLEANTHAT_JSON = "cleanthat.json";

	public static final List<String> FILENAMES_CLEANTHAT =
			Arrays.asList(FILENAME_CLEANTHAT_FOLDER + FILENAME_CLEANTHAT_YAML,
					FILENAME_CLEANTHAT_FOLDER + FILENAME_CLEANTHAT_YML,
					FILENAME_CLEANTHAT_YAML,
					FILENAME_CLEANTHAT_YML,
					FILENAME_CLEANTHAT_JSON);

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
		Optional<Map.Entry<String, String>> optPathAndContent;
		optPathAndContent = PATH_CLEANTHAT.stream().map(p -> {
			try {
				return codeProvider.loadContentForPath(p).map(content -> Maps.immutableEntry(p, content));
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		}).flatMap(Optional::stream).findFirst();

		if (optPathAndContent.isEmpty()) {
			return Optional.empty();
		}

		ObjectMapper objectMapper;
		Map.Entry<String, String> pathAndContent = optPathAndContent.get();
		LOGGER.info("Loaded config from {}", pathAndContent.getKey());
		if (pathAndContent.getKey().endsWith(".json")) {
			objectMapper = ConfigHelpers.getJson(objectMappers);
		} else if (pathAndContent.getKey().endsWith(".yml") || pathAndContent.getKey().endsWith(".yaml")) {
			objectMapper = ConfigHelpers.getJson(objectMappers);
		} else {
			throw new IllegalArgumentException("Not managed extention: " + pathAndContent.getKey());
		}

		return optPathAndContent.map(content -> {
			try {
				return objectMapper.readValue(content.getValue(), Map.class);
			} catch (JsonProcessingException e) {
				throw new IllegalArgumentException("Invalid config (json vs yaml?)", e);
			}
		});
	}

	public static File pathToConfig(Path localFolder) {
		return CodeProviderHelpers.FILENAMES_CLEANTHAT.stream()
				.map(s -> localFolder.resolve(s).toFile())
				.filter(File::exists)
				.findAny()
				.orElseThrow(() -> new IllegalStateException(
						"No configuration at pathes: " + CodeProviderHelpers.FILENAMES_CLEANTHAT));
	}

}
