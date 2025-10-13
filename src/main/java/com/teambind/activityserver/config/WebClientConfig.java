package com.teambind.activityserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
	
	@Value("${article.server.url:https://teambind.co.kr:9300}")
	private String articleServerUrl;
	
	@Bean
	public WebClient webClient() {
		return WebClient.builder()
				.baseUrl(articleServerUrl)
				.build();
	}
}
