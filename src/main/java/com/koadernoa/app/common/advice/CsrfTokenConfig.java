package com.koadernoa.app.common.advice;


import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.nio.file.Paths;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CsrfToken;

@Configuration
public class CsrfTokenConfig implements WebMvcConfigurer {
	
	@Value("${koadernoa.uploads.dir:uploads}")
    private String baseDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void postHandle(HttpServletRequest request, HttpServletResponse response,
                                   Object handler, ModelAndView modelAndView) throws Exception {

                CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

                if (token != null && modelAndView != null) {
                    modelAndView.addObject("_csrf", token);
                }
            }
        });
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Paths.get(baseDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + root.toString() + "/")
                .setCachePeriod(3600);
    }
}