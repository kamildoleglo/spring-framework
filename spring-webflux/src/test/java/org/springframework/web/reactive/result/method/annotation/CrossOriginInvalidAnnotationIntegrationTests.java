/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.util.Properties;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests with invalid {@code @CrossOrigin} annotation with allowCredentials specified without allowOrigins.
 *
 * @author Sebastien Deleuze
 */
class CrossOriginInvalidAnnotationIntegrationTests extends AbstractRequestMappingIntegrationTests {

	private final HttpHeaders headers = new HttpHeaders();


	@Override
	protected void startServer(HttpServer httpServer) throws Exception {
		super.startServer(httpServer);
		this.headers.setOrigin("https://site1.com");
	}

	@Override
	protected ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(WebConfig.class);
		Properties props = new Properties();
		props.setProperty("myOrigin", "https://site1.com");
		context.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("ps", props));
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.refresh();
		return context;
	}

	@Override
	protected RestTemplate initRestTemplate() {
		// JDK default HTTP client disallowed headers like Origin
		return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
	}


	@ParameterizedHttpServerTest
	void actualGetRequestWithoutAnnotation(HttpServer httpServer) throws Exception {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() -> startServer(httpServer));
	}


	@Configuration
	@EnableWebFlux
	@ComponentScan(resourcePattern = "**/CrossOriginInvalidAnnotationIntegrationTests*")
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class WebConfig {
	}

	@RestController
	@SuppressWarnings("unused")
	private static class InvalidController {

		@CrossOrigin(allowCredentials = "true")
		@GetMapping("/baz")
		public String baz() {
			return "baz";
		}
	}

}
