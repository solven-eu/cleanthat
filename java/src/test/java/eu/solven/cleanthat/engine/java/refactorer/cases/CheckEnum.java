package eu.solven.cleanthat.engine.java.refactorer.cases;

import java.math.RoundingMode;

public class CheckEnum {
	public boolean pre(RoundingMode roundingMode) {
		return roundingMode.equals(RoundingMode.UP);
	}
}
