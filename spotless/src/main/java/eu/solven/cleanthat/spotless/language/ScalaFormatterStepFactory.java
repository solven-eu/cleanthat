/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
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
import com.diffplug.spotless.scala.ScalaFmtStep;
import com.google.common.collect.ImmutableList;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.spotless.AFormatterStepFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepParametersProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Configure Spotless engine for '.scala' files
 * 
 * @author Benoit Lacelle
 *
 */
public class ScalaFormatterStepFactory extends AFormatterStepFactory {
	private static final String LICENSE_HEADER_DELIMITER = "package ";

	public ScalaFormatterStepFactory(ScalaFormatterFactory scalaFactory,
			ICodeProvider codeProvider,
			SpotlessFormatterProperties formatterProperties) {
		super(scalaFactory, codeProvider, formatterProperties);
	}

	@Override
	public String licenseHeaderDelimiter() {
		return LICENSE_HEADER_DELIMITER;
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	public FormatterStep makeSpecializedStep(SpotlessStepProperties s, Provisioner provisioner) {
		String stepId = s.getId();
		SpotlessStepParametersProperties parameters = s.getParameters();

		switch (stepId) {
		case "scalafmt": {
			String scalafmtVersion = parameters.getCustomProperty("version", String.class);
			if (scalafmtVersion == null) {
				scalafmtVersion = ScalaFmtStep.defaultVersion();
			}

			String scalaMajorVersion = parameters.getCustomProperty("scala_major_version", String.class);
			if (scalaMajorVersion == null) {
				scalaMajorVersion = ScalaFmtStep.defaultScalaMajorVersion();
			}

			String stylesheetFilePath = parameters.getCustomProperty(KEY_FILE, String.class);
			File stylesheetFile = null;
			if (stylesheetFilePath != null) {
				try {
					stylesheetFile = locateFile(stylesheetFilePath);
				} catch (IOException e) {
					throw new UncheckedIOException("Issue processing eclipse.file: " + stylesheetFile, e);
				}
			}
			return ScalaFmtStep.create(scalafmtVersion, scalaMajorVersion, provisioner, stylesheetFile);
		}
		default: {
			throw new IllegalArgumentException("Unknown step: " + stepId);
		}
		}
	}

	// This is useful to demonstrate all available configuration
	public static List<SpotlessStepProperties> exampleSteps() {
		SpotlessStepProperties scalafmt = SpotlessStepProperties.builder().id("scalafmt").build();
		SpotlessStepParametersProperties scalafmtParameters = new SpotlessStepParametersProperties();
		scalafmtParameters.putProperty("version", ScalaFmtStep.defaultVersion());
		scalafmtParameters.putProperty("scalaMajorVersion", ScalaFmtStep.defaultScalaMajorVersion());
		// https://scalameta.org/scalafmt/docs/configuration.html
		scalafmtParameters.putProperty(KEY_FILE, "repository:/.cleanthat/.scalafmt.conf");
		scalafmt.setParameters(scalafmtParameters);

		return ImmutableList.<SpotlessStepProperties>builder().add(scalafmt).build();
	}

}
