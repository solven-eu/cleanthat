package eu.solven.cleanthat.codeprovider;

/**
 * Simply wraps an {@link Object}
 * 
 * @author Benoit Lacelle
 *
 */
public class DummyCodeProviderFile implements ICodeProviderFile {
	private final String path;
	private final Object raw;

	/**
	 * 
	 * @param path
	 *            path of the file, consider '/' is the root of the repository
	 * @param raw
	 */
	public DummyCodeProviderFile(String path, Object raw) {
		if (raw instanceof DummyCodeProviderFile) {
			throw new IllegalArgumentException("input can not be an instance of " + this.getClass());
		} else if (!path.startsWith("/")) {
			throw new IllegalArgumentException("Invalid path: " + path + " (missing '/' at the beginning)");
		} else if (path.startsWith("//")) {
			throw new IllegalArgumentException("Invalid path: " + path + " ('//' at the beginning)");
		}

		this.path = path;
		this.raw = raw;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public Object getRaw() {
		return raw;
	}

	// @Override
	// public boolean fileIsRemoved(ICodeProvider codeProvider) {
	// return codeProvider.deprecatedFileIsRemoved(getRaw());
	// }

	// @Override
	// public String loadContent(ICodeProvider codeProvider) throws IOException {
	// return codeProvider.deprecatedLoadContent(getRaw());
	// }

	// @Override
	// public String getFilePath(ICodeProvider codeProvider) {
	// return codeProvider.deprecatedGetFilePath(getRaw());
	// }
}
