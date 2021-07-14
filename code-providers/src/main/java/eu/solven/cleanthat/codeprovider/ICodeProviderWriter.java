package eu.solven.cleanthat.codeprovider;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Enable writing/commiting code in addition of reading it
 *
 * @author Benoit Lacelle
 */
public interface ICodeProviderWriter extends ICodeProvider {

	void commitIntoBranch(Map<String, String> pathToMutatedContent,
			List<String> prComments,
			Collection<String> prLabels);

}
