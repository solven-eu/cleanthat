package eu.solven.cleanthat.codeprovider;

/**
 * Enable writing/commiting code in addition of reading it
 *
 * @author Benoit Lacelle
 */
public interface ICodeProviderWriter extends ICodeProvider, ICodeProviderWriterLogic {

	void cleanTmpFiles();

}
