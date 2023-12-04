package eu.solven.cleanthat.engine.java.refactorer;

import java.time.LocalDate;

public class NoWildcardImport_IsNotImported_Post {
	public static boolean isEmpty(LocalDate date) {
		return date.isLeapYear();
	}
}
