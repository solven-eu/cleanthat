package eu.solven.cleanthat.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

import cormoran.pepper.collection.PepperMapHelper;

/**
 * Helps providing information about Git through a REST api. It implements {@link BeanFactoryPostProcessor} to ensure
 * being loaded very early, in order to log early during startup.
 * 
 * @author Benoit Lacelle
 *
 */
public class GitService implements IGitService, InitializingBean {
	protected static final Logger LOGGER = LoggerFactory.getLogger(GitService.class);

	private static final String KEY_GIT_COMMIT_ID = "git.commit.id";

	@Override
	public void afterPropertiesSet() throws IOException {
		getProperties().entrySet().forEach(e -> {
			if (KEY_GIT_COMMIT_ID.equals(e.getKey())) {
				LOGGER.info("Git info: {}", e);
			} else {
				LOGGER.debug("Git info: {}", e);
			}
		});
	}

	@Override
	public Map<String, ?> getProperties() throws IOException {
		ClassPathResource resource = new ClassPathResource("/git.json");
		if (!resource.exists()) {
			// https://github.com/solven-eu/mitrust-datasharing/issues/8871
			LOGGER.warn("We failed finding the resource: {}", resource.getPath());
			resource = new ClassPathResource("/git.fallback.json");
		}

		return new ObjectMapper().readValue(resource.getInputStream(), Map.class);
	}

	@Override
	public String getSha1() {
		Map<String, ?> properties;
		try {
			properties = getProperties();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return PepperMapHelper.getRequiredString(properties, KEY_GIT_COMMIT_ID);
	}
}
