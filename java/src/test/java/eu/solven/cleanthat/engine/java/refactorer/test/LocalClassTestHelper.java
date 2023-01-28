/*
 * Copyright 2023 Solven
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
