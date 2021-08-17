package eu.solven.cleanthat.codeprovider;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Enable writing/commiting code
 *
 * @author Benoit Lacelle
 */
public interface ICodeProviderWriterLogic {

	void persistChanges(Map<String, String> pathToMutatedContent, List<String> prComments, Collection<String> prLabels);

}
