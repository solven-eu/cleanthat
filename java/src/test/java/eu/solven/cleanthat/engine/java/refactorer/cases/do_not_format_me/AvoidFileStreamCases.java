package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.AvoidFileStream;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

// see https://jsparrow.github.io/rules/enums-without-equals.html#properties
public class AvoidFileStreamCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new AvoidFileStream();
	}

	@CompareMethods
	public static class FileInputStreamOverFile {
		public FileInputStream pre(File fileName) throws FileNotFoundException {
			return new FileInputStream(fileName);
		}

		public InputStream post(File fileName) throws IOException {
			return Files.newInputStream(fileName.toPath());
		}
	}

	@CompareMethods
	public static class FileInputStreamOverString {
		public FileInputStream pre(String fileName) throws FileNotFoundException {
			return new FileInputStream(fileName);
		}

		public InputStream post(String fileName) throws IOException {
			return Files.newInputStream(Paths.get(fileName));
		}
	}

	@CompareMethods
	public static class FileInputStreamOverPath {
		public FileInputStream pre(Path path) throws FileNotFoundException {
			return new FileInputStream(path.toFile());
		}

		public InputStream post(Path path) throws IOException {
			return Files.newInputStream(path);
		}
	}

	@CompareMethods
	public static class FileOutputStreamOverFile {
		public FileOutputStream pre(File fileName) throws FileNotFoundException {
			return new FileOutputStream(fileName);
		}

		public OutputStream post(File fileName) throws IOException {
			return Files.newOutputStream(fileName.toPath());
		}
	}

	@CompareMethods
	public static class FileReaderOverFile {
		public FileReader pre(File fileName) throws FileNotFoundException {
			return new FileReader(fileName);
		}

		public BufferedReader post(File fileName) throws IOException {
			return Files.newBufferedReader(fileName.toPath());
		}
	}

	@CompareMethods
	public static class FileWriterOverFile {
		public FileWriter pre(File fileName) throws IOException {
			return new FileWriter(fileName);
		}

		public BufferedWriter post(File fileName) throws IOException {
			return Files.newBufferedWriter(fileName.toPath());
		}
	}
}
