package eu.solven.cleanthat.language.java.refactorer.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This is a marker interface for cases which must have one and only-one 'pre' and a 'post' method, both returning a
 * Class<?> with a loadeable source-code.
 * 
 * @author Benoit Lacelle
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CompareClasses {
	Class<?> pre();

	Class<?> post();
}
