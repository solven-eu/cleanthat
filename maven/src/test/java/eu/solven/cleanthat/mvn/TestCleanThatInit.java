package eu.solven.cleanthat.mvn;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestCleanThatInit {
	@Test
	public void testConfigUrlIsNull() {
		CleanThatInitMojo mojo = new CleanThatInitMojo();
		Assertions.assertThat(mojo.getConfigUrl()).isNull();
	}

	@Test
	public void testConfigUrlIsAnything() {
		CleanThatInitMojo mojo = new CleanThatInitMojo();
		mojo.setConfigUrl("anything");
		Assertions.assertThat(mojo.getConfigUrl()).isEqualTo("anything");
	}

	@Test
	public void testConfigUrlHasPlaceholder() {
		CleanThatInitMojo mojo = new CleanThatInitMojo();
		mojo.setConfigUrl("${not_replaced}");
		Assertions.assertThatThrownBy(() -> mojo.getConfigUrl()).isInstanceOf(IllegalStateException.class);
	}
}
