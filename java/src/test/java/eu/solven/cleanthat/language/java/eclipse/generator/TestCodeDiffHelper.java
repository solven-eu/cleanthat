package eu.solven.cleanthat.language.java.eclipse.generator;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestCodeDiffHelper {

	@Test
	public void testDeltaDiff() {
		CodeDiffHelper helper = new CodeDiffHelper();
		Assertions.assertThat(helper.deltaDiff("abcd", "efgh")).isEqualTo(4);

		Assertions.assertThat(helper.deltaDiff("abcd", "_abcd_")).isEqualTo(2);
		Assertions.assertThat(helper.deltaDiff("        b.add(-234);", "        b.add( -234);")).isEqualTo(1);

		Assertions.assertThat(helper.deltaDiff("aaa12bbb23ccc", "aaa  bbb  ccc")).isEqualTo(4);
	}
}
