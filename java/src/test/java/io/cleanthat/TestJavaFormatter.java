package io.cleanthat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.FileCopyUtils;

import eu.solven.cleanthat.github.CleanThatRepositoryProperties;
import io.cleanthat.do_not_format_me.CleanClass;
import io.cleanthat.do_not_format_me.ManySpacesBetweenImportsSimpleClass;
import io.cormoran.cleanthat.formatter.eclipse.JavaFormatter;

public class TestJavaFormatter {

	// https://www.baeldung.com/spring-load-resource-as-string
	public static String asString(Resource resource) {
		try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
			return FileCopyUtils.copyToString(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Test
	public void testFormat_Clean() throws IOException {
		JavaFormatter formatter = new JavaFormatter();
		URL location = CleanClass.class.getProtectionDomain().getCodeSource().getLocation();

		String classAsString = asString(new UrlResource(location));

		CleanThatRepositoryProperties properties = new CleanThatRepositoryProperties();
		formatter.format(properties, classAsString);
	}

	@Test
	public void testFormat_ManySpacesMiddleImports() throws IOException {
		JavaFormatter formatter = new JavaFormatter();
		URL location = ManySpacesBetweenImportsSimpleClass.class.getProtectionDomain().getCodeSource().getLocation();

		String classAsString = asString(new UrlResource(location));

		CleanThatRepositoryProperties properties = new CleanThatRepositoryProperties();
		formatter.format(properties, classAsString);
	}
}
