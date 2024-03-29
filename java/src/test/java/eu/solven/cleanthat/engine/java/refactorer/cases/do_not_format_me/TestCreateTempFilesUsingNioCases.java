package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.CreateTempFilesUsingNio;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

/**
 * cases inspired from https://jsparrow.github.io/rules/create-temp-files-using-java-nio.html#code-changes
 *
 * @author Sébastien Collard
 */

public class TestCreateTempFilesUsingNioCases extends AJavaparserRefactorerCases {

	@Override
	public IJavaparserAstMutator getTransformer() {
		return new CreateTempFilesUsingNio();
	}

	@CompareMethods
	public static class CasePrefixSuffix {
		public Object pre() throws IOException {
			return File.createTempFile("myFile", ".tmp");
		}

		public Object post() throws IOException {
			return Files.createTempFile("myFile", ".tmp").toFile();
		}
	}

	@CompareMethods
	public static class CaseDirectoryCreation {
		public Object pre() throws IOException {
			return File.createTempFile("myFile", ".tmp", new File("/tmp/test/"));
		}

		public Object post() throws IOException {
			return Files.createTempFile(Paths.get("/tmp/test/"), "myFile", ".tmp").toFile();
		}
	}

	// TODO Add test case where directory is explicitly null
	@CompareMethods
	public static class CaseWithDirectory {
		public Object pre(File directory) throws IOException {
			return File.createTempFile("myFile", ".tmp", directory);
		}

		public Object post(File directory) throws IOException {
			return (directory == null ? Files.createTempFile("myFile", ".tmp") : Files.createTempFile(directory.toPath(), "myFile", ".tmp")).toFile();
		}
	}

	@CompareMethods
	public static class CaseNullDirectory {
		public Object pre() throws IOException {
			return File.createTempFile("myFile", ".tmp", null);
		}

		public Object post() throws IOException {
			return Files.createTempFile("myFile", ".tmp").toFile();
		}
	}

}
