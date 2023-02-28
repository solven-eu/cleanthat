package eu.solven.cleanthat.engine.java.refactorer;

import java.util.Map;

public class TestEclipseStylesheetGenerator_OverBigFiles {

	public String testRoaringBitmap(Map<String, ?> map) {
		return map.entrySet().stream().map(e -> e.getKey()).findAny().get();
	}
}
