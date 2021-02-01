package eu.solven.cleanthat.rules.cases;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import eu.solven.cleanthat.rules.CreateTempFilesUsingNio;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * cases inspired from https://jsparrow.github.io/rules/create-temp-files-using-java-nio.html#code-changes
 *
 * @author Sébastien Collard
 */

public class CreateTempFilesUsingNioCases {

	public CreateTempFilesUsingNioCases() {
		// TODO Auto-generated constructor stub
	}

	public String getId() {
		return "CreateTempFilesUsingNio";
	}

	public IClassTransformer getTransformer() {
		return new CreateTempFilesUsingNio();
	}

	public static class CasePrefixSuffix {
		public String getTitle() {
			return "File.createTempFile(\"myFile\", \".tmp\")";
		}

		public Object pre() throws IOException {
			return File.createTempFile("myFile", ".tmp");
		}

		public Object post() throws IOException {
			return Files.createTempFile("myFile", ".tmp").toFile();
		}
	}

	public static class CaseDirectoryCreation {
		public String getTitle() {
			return "File.createTempFile(\"myFile\", \".tmp\", new File(\"/tmp/test/\"))";
		}

		public Object pre() throws IOException {
			return File.createTempFile("myFile", ".tmp", new File("/tmp/test/"));
		}

		public Object post() throws IOException {
			return Files.createTempFile(Paths.get("/tmp/test/"), "myFile", ".tmp").toFile();
		}
	}

	public static class CaseWithDirectory {
		public String getTitle() {
			return "File.createTempFile(\"myFile\", \".tmp\", directory)";
		}

		public Object pre(File directory) throws IOException {
			return File.createTempFile("myFile", ".tmp", directory);
		}

		public Object post(File directory) throws IOException {
			return Files.createTempFile(directory.toPath(), "myFile", ".tmp").toFile();
		}
	}

	public static class CaseNullDirectory {
		public String getTitle() {
			return "File.createTempFile(\"myFile\", \".tmp\", null)";
		}

		public Object pre() throws IOException {
			return File.createTempFile("myFile", ".tmp", null);
		}

		public Object post() throws IOException {
			return Files.createTempFile("myFile", ".tmp").toFile();
		}
	}

}
