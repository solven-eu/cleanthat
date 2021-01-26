package eu.solven.cleanthat.java.mutators;

import java.util.stream.IntStream;

import com.github.javaparser.StaticJavaParser;

public class StaticJavaParserThreadSafetyIssue {
	public static void main(String[] args) {
		IntStream.range(0, 16).parallel().forEach(i -> {
			while (true) {
				StaticJavaParser.parse("package eu.solven.cleanthat.java.mutators;\n" + "\n"
						+ "import java.util.stream.IntStream;\n"
						+ "\n"
						+ "import com.github.javaparser.StaticJavaParser;\n"
						+ "\n"
						+ "public class StaticJavaParserThreadSafetyIssue {\n"
						+ "	public static void main(String[] args) {\n"
						+ "		IntStream.range(0, 16).parallel().forEach(i -> {\n"
						+ "			StaticJavaParser.parse(\"\");\n"
						+ "		});\n"
						+ "	}\n"
						+ "}\n"
						+ "");
			}
		});
	}
}
