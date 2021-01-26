package eu.solven.cleanthat.codeprovider;

import java.io.IOException;

/**
 * Simply wraps an {@link Object}
 * 
 * @author Benoit Lacelle
 *
 */
public class DummyCodeProviderFile implements ICodeProviderFile {
	private final Object raw;

	public DummyCodeProviderFile(Object raw) {
		if (raw instanceof DummyCodeProviderFile) {
			throw new IllegalArgumentException("input can not be an instance of " + this.getClass());
		}

		this.raw = raw;
	}

	public Object getRaw() {
		return raw;
	}

	@Override
	public boolean fileIsRemoved(ICodeProvider codeProvider) {
		return codeProvider.deprecatedFileIsRemoved(getRaw());
	}

	@Override
	public String loadContent(ICodeProvider codeProvider) throws IOException {
		return codeProvider.deprecatedLoadContent(getRaw());
	}

	@Override
	public String getFilePath(ICodeProvider codeProvider) {
		return codeProvider.deprecatedGetFilePath(getRaw());
	}
}
