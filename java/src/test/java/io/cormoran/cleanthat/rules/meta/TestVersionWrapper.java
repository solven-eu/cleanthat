package io.cormoran.cleanthat.rules.meta;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import eu.solven.cleanthat.language.java.IJdkVersionConstants;
import eu.solven.cleanthat.language.java.rules.meta.VersionWrapper;

public class TestVersionWrapper {
	@Test
	public void testCompareVersions() {
		Assertions.assertThat(new VersionWrapper(IJdkVersionConstants.JDK_7))
				.isGreaterThan(new VersionWrapper(IJdkVersionConstants.JDK_1))
				.isGreaterThan(new VersionWrapper(IJdkVersionConstants.JDK_6))
				.isLessThan(new VersionWrapper(IJdkVersionConstants.JDK_11));
	}
}
