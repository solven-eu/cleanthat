package eu.solven.cleanthat.engine.java.refactorer;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class NoWildcardImport_IsImported_Post {
	public static boolean isLeapYear(LocalDate date) {
		return date.isLeapYear();
	}

	public static LocalDateTime isEmpty(LocalDate date) {
		return date.atStartOfDay();
	}
}
