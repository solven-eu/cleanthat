package blasd.apex.server.cube.snapshot;

import com.quartetfs.biz.pivot.cube.hierarchy.ILevelInfo;

public class TestApexCubeSnapshooterOnUpdatedCountries {

	public void testQueryOnUpdatePartitions(ILevelInfo countryLevel) {
		return new AApexCubeSnapshooter() {

				@Override
				protected String getTargetColumnName(ILevelInfo levelInfo) {
					if (levelInfo.equals(countryLevel)) {
						return COUNTRY;
					} else {
						return super.getTargetColumnName(levelInfo);
					}
				}

			};
	}
}
