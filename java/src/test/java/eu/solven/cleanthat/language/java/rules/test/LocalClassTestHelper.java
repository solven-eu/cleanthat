package eu.solven.cleanthat.language.java.rules.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.springframework.core.io.ClassPathResource;

public class LocalClassTestHelper {

	public static Path getProjectTestSourceCode() throws IOException {
		Path srcMainResource = new ClassPathResource("logback-test.xml").getFile().getParentFile().toPath();
		Assert.assertEquals("test-classes", srcMainResource.getName(srcMainResource.getNameCount() - 1).toString());
		Assert.assertEquals("target", srcMainResource.getName(srcMainResource.getNameCount() - 2).toString());
		Assert.assertEquals("java", srcMainResource.getName(srcMainResource.getNameCount() - 3).toString());
		return srcMainResource.resolve("./../../src/test/java").toAbsolutePath();
	}

	public static Path localClassAsPath(Class<?> classToLoad) throws IOException {
		Path srcMainJava = getProjectTestSourceCode();
		// https://stackoverflow.com/questions/3190301/obtaining-java-source-code-from-class-name
		String path = classToLoad.getName().replaceAll("\\.", "/") + ".java";

		Path pathToDirty = srcMainJava.resolve(path);
		return pathToDirty;
	}

	public static String loadClassAsString(Class<?> classToLoad) throws IOException {
		Path pathToDirty = localClassAsPath(classToLoad);

		String dirtyCode = Files.readString(pathToDirty);
		return dirtyCode;
	}

}
