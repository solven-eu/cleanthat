package eu.solven.cleanthat.config;

import java.io.IOException;
import java.util.Map;

/**
 * Interface for service providing information about Git
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGitService {

	Map<String, ?> getProperties() throws IOException;

	String getSha1();

}
