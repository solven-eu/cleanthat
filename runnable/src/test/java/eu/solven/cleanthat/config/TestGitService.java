package eu.solven.cleanthat.config;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestGitService {
	@Test
	public void testGetSha1() {
		Assertions.assertThat(new GitService().getSha1()).isNotEmpty();
	}
}
