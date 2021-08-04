package eu.solven.cleanthat.language.scala.scalafix;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.google.common.io.ByteStreams;

import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.ISourceCodeProperties;
import eu.solven.cleanthat.language.SourceCodeProperties;

public class TestScalafixFormatter {

	@Test
	public void testProcedureSyntax() throws IOException {
		ISourceCodeProperties sourceCodeProperties = new SourceCodeProperties();
		ScalafixProperties scalaFmtProperties = new ScalafixProperties();
		final ScalafixFormatter formatter = new ScalafixFormatter(sourceCodeProperties, scalaFmtProperties);

		String before = new String(
				ByteStreams
						.toByteArray(new ClassPathResource("/scala/scalafix/ProcedureSyntax.scala").getInputStream()),
				StandardCharsets.UTF_8);

		{
			Assertions.assertThat(before.split("[\r\n]"))
					.hasSize(5)
					.containsExactly("// https://scalacenter.github.io/scalafix/docs/rules/ProcedureSyntax.html",
							"object Hello {",
							"  def main(args: Seq[String]) { println(\"Hello world!\") }",
							"  trait A { def doSomething }",
							"}");
		}

		String after = formatter.doFormat(before, LineEnding.KEEP);

		{
			Assertions.assertThat(after).isNotEqualTo(before).hasLineCount(5);
			Assertions.assertThat(after.split("[\r\n]"))
					.hasSize(5)
					.containsExactly("// https://scalacenter.github.io/scalafix/docs/rules/ProcedureSyntax.html",
							"object Hello {",
							"  def main(args: Seq[String]): Unit = { println(\"Hello world!\") }",
							"  trait A { def doSomething: Unit }",
							"}");
		}

	}
}
