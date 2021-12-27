package eu.solven.cleanthat.language.java.eclipse;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * The configuration of what is not related to a language.
 * 
 * @author Benoit Lacelle
 *
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@SuppressWarnings("PMD.ImmutableField")
@Data
public class EclipseJavaFormatterProcessorProperties {
	public static final String URL_DEFAULT_GOOGLE = "classpath:/eclipse/eclipse-java-google-style.xml";
	public static final String URL_DEFAULT_SPRING = "classpath:/eclipse/spring-eclipse-code-formatter.xml";
	public static final String URL_DEFAULT_PEPPER = "classpath:/eclipse/pepper-eclipse-code-formatter.xml";

	// see eu.solven.cleanthat.language.CleanthatUrlLoader.loadUrl(ICodeProvider, String)
	private String url = URL_DEFAULT_GOOGLE;

}
