/*
 * Copyright 2023 Solven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.spotless.language;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.google.common.collect.ImmutableList;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.spotless.AFormatterStepFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;
import java.util.List;

/**
 * Configure Spotless engine for XML files
 * 
 * @author Benoit Lacelle
 *
 */
public class XmlFormatterStepFactory extends AFormatterStepFactory {
	public XmlFormatterStepFactory(XmlFormatterFactory xmlFactory,
			ICodeProvider codeProvider,
			SpotlessFormatterProperties spotlessProperties) {
		super(xmlFactory, codeProvider, spotlessProperties);
	}

	@Override
	public String licenseHeaderDelimiter() {
		return null;
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	public FormatterStep makeSpecializedStep(SpotlessStepProperties s, Provisioner provisioner) {
		String stepName = s.getId();
		throw new IllegalArgumentException("Unknown step: " + stepName);
	}

	// This is useful to demonstrate all available configuration
	public static List<SpotlessStepProperties> exampleSteps() {
		SpotlessStepProperties eclipseWtp = new SpotlessStepProperties();
		eclipseWtp.setId("eclipseWtp");
		eclipseWtp.putProperty(KEY_TYPE, eclipseWtp);

		return ImmutableList.<SpotlessStepProperties>builder().add(eclipseWtp).build();
	}

}
