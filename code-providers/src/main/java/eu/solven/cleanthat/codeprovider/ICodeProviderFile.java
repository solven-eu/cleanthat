package eu.solven.cleanthat.codeprovider;

import java.io.IOException;

/**
 * 
 * @author Benoit Lacelle
 *
 */
public interface ICodeProviderFile {
	// boolean fileIsRemoved(ICodeProvider codeProvider);

	String loadContent(ICodeProvider codeProvider) throws IOException;

	// String getFilePath(ICodeProvider codeProvider);

	String getPath();

	/**
	 * The core object materializing this object/blob/path
	 * 
	 * @return
	 */
	Object getRaw();
}
