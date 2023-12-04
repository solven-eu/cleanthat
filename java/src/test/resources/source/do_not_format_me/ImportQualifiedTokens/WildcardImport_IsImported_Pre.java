package eu.solven.cleanthat.engine.java.refactorer;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class NoWildcardImport_IsImported_Pre {
	public static boolean isLeapYear(java.time.LocalDate date) {
		return date.isLeapYear();
	}

	public static LocalDateTime isEmpty(LocalDate date) {
		return date.atStartOfDay();
	}
}
