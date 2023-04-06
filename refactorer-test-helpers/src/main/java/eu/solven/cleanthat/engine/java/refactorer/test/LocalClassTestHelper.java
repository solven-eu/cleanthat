/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.engine.java.refactorer.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.springframework.core.io.ClassPathResource;

/**
 * Helps loading local .java file
 * 
 * @author Benoit Lacelle
 *
 */
public class LocalClassTestHelper {
	protected LocalClassTestHelper() {
		// hidden
	}

	@SuppressWarnings("PMD.MagicNumber")
	public static Path getSrcMainResourceFolder() throws IOException {
		var someResourceInSrcTestResources = new ClassPathResource("/logback-test.xml").getFile();
		var srcMainResource = someResourceInSrcTestResources.getParentFile().toPath();
		var nameCount = srcMainResource.getNameCount();
		Assert.assertEquals("Check 'test-classes' directory",
				"test-classes",
				srcMainResource.getName(nameCount - 1).toString());
		Assert.assertEquals("Check 'target' directory", "target", srcMainResource.getName(nameCount - 2).toString());
		// Assert.assertEquals("Check 'java' directory", "java", srcMainResource.getName(nameCount - 3).toString());
		return srcMainResource;
	}

	public static Path getProjectTestSourceCode() throws IOException {
		var srcMainResource = getSrcMainResourceFolder();
		return srcMainResource.resolve("./../../src/test/java").toAbsolutePath();
	}

	public static Path localClassAsPath(Class<?> classToLoad) throws IOException {
		var srcMainJava = getProjectTestSourceCode();
		// https://stackoverflow.com/questions/3190301/obtaining-java-source-code-from-class-name
		var path = classToLoad.getName().replace(".", "/") + ".java";

		var pathToDirty = srcMainJava.resolve(path);
		return pathToDirty;
	}

	public static String loadClassAsString(Class<?> classToLoad) throws IOException {
		var pathToDirty = localClassAsPath(classToLoad);

		var dirtyCode = Files.readString(pathToDirty);
		return dirtyCode;
	}

}
