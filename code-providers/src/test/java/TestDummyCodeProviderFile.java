import org.assertj.core.api.Assertions;
import org.junit.Test;

import eu.solven.cleanthat.codeprovider.DummyCodeProviderFile;

public class TestDummyCodeProviderFile {
	@Test
	public void testMissingSlash() {
		Assertions.assertThatThrownBy(() -> new DummyCodeProviderFile("dir/file", null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testTrailingDoubleSlash() {
		Assertions.assertThatThrownBy(() -> new DummyCodeProviderFile("//dir/file", null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testOk() {
		new DummyCodeProviderFile("/dir/file", null);
	}
}
