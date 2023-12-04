package eu.solven.cleanthat.engine.java.refactorer;

import java.time.*;
import java.time.LocalDateTime;

public class NoWildcardImport_IsWildcardImported_Pre {
	public static boolean isLeapYear(LocalDate date) {
		return date.isLeapYear();
	}

	public static LocalDateTime isEmpty(LocalDate date) {
		return date.atStartOfDay();
	}
}
