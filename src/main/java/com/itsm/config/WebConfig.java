package com.itsm.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Open-entity-manager-in-view lets Thymeleaf pages read lazy associations while rendering.
 * Spring Boot's built-in version is switched off ({@code spring.jpa.open-in-view=false}) because it
 * applies to every request: a live-update stream stays open for as long as the browser tab does,
 * and would hold a database connection the whole time, exhausting the pool after a few page loads.
 * This registers the same interceptor for everything except the streams.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final EntityManagerFactory entityManagerFactory;

    public WebConfig(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        OpenEntityManagerInViewInterceptor interceptor = new OpenEntityManagerInViewInterceptor();
        interceptor.setEntityManagerFactory(entityManagerFactory);
        registry.addWebRequestInterceptor(interceptor).excludePathPatterns("/live/**");
    }
}
