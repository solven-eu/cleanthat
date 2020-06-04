/*
 * Copyright © 2019 MiTrust (cto@m-itrust.com). Unauthorized copying of this file, via any medium is strictly prohibited. Proprietary and confidential
 */
package io.cormoran.cleanthat.gateway;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import com.fasterxml.jackson.databind.ObjectMapper;

import cormoran.pepper.time.PepperDateHelper;
import eu.solven.cleanthat.gateway.CleanThatSpringConfig;
import io.cormoran.cleanthat.gateway.TestRunDatasharingSpringConfigAutonomy.RunMiTrustSpringConfigComplement;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { CleanThatSpringConfig.class,
		RunMiTrustSpringConfigComplement.class })
public class TestRunDatasharingSpringConfigAutonomy {

	protected static final Logger LOGGER = LoggerFactory.getLogger(TestRunDatasharingSpringConfigAutonomy.class);

	@Configuration
	public static class RunMiTrustSpringConfigComplement {
		// Required by CorsConfigurer as one of SpringMVC or SpringSecurity is not configured
		@Bean
		public HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
			return new HandlerMappingIntrospector();
		}

		@Bean
		public ObjectMapper objectMapper() {
			return new ObjectMapper();
		}
	}

	@Autowired
	ApplicationContext appContext;

	@Test
	public void testAutonomy() {
		LOGGER.debug("Started on {}", PepperDateHelper.now());
	}

}
