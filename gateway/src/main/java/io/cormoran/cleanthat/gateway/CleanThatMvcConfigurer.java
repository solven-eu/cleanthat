package io.cormoran.cleanthat.gateway;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Defines how are delivered static resources, including icons, the VueJS app
 *
 * @author Benoit Lacelle
 *
 */
// https://stackoverflow.com/questions/24156490/how-to-set-content-length-in-spring-mvc-rest-for-json
@Configuration
public class CleanThatMvcConfigurer implements WebMvcConfigurer {

	// public static final int DEFAULT_CACHEPERIOD_MINUTE = 10;

	/**
	 * Controllers always produce application/json response even other type is asked (eg: xml/application)
	 *
	 * @param configurer
	 *            content negotiation configurer
	 */
	// @Override
	// public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
	// configurer.ignoreAcceptHeader(true).defaultContentType(MediaType.APPLICATION_JSON);
	// }

	// @Override
	// public void addCorsMappings(CorsRegistry registry) {
	// registry.addMapping("/**")
	// .allowedMethods(AgileaCorsConfiguration.getAllowedMethods())
	// .allowedHeaders(AgileaCorsConfiguration.getAlloweHeaders())
	// .exposedHeaders(AgileaCorsConfiguration.getExposedHeaders())
	// .allowedOrigins(AgileaCorsConfiguration.getAllowedOrigins());
	// }

	// protected void setCachePeriod(ResourceHandlerRegistration resourceHandler) {
	// resourceHandler.setCachePeriod(Ints.saturatedCast(TimeUnit.MINUTES.toSeconds(DEFAULT_CACHEPERIOD_MINUTE)));
	// }
	//
	// protected void extraConfigureResourceHandlerRegistration(ResourceHandlerRegistration resourceHandler) {
	// resourceHandler
	// // https://github.com/webjars/webjars-locator/issues/100
	// .resourceChain(true)
	// // resolve HTML files when requesting without extension
	// .addResolver(new HtmlRessourceResolver())
	// // http://www.baeldung.com/spring-mvc-static-resources
	// .addResolver(new EncodedResourceResolver())
	// .addResolver(new PathResourceResolver());
	// }

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// Resources controlled by Spring Security has
		// "Cache-Control: must-revalidate". -> We override it with some value

		ResourceHandlerRegistration resourceHandler = registry.addResourceHandler("/**")
				.addResourceLocations(
						// Static resources written in the gateway
						"classpath:/public/"
				// ,
				// // Frontend, managed by `web` project
				// "classpath:/react/"
				);

		// setCachePeriod(resourceHandler);
		// extraConfigureResourceHandlerRegistration(resourceHandler);
	}

}
