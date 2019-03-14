/**
 * Copyright 2016 Netflix, Inc.
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
package com.netflix.conductor.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.Provides;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

//import com.adobe.asr.annotations.cors.CrossOriginResourceSharing;
import com.adobe.asr.autoconfigure.ims.annotation.IMSServiceAuthContext;
import com.adobe.asr.autoconfigure.ims.annotation.IMSServiceAuthentication;
import com.adobe.asr.autoconfigure.ims.annotation.IMSUserAuthContext;
import com.adobe.asr.autoconfigure.ims.annotation.IMSUserAuthentication;
import com.adobe.asr.autoconfigure.ims.dto.IMSTokenInfo;
import com.adobe.asr.commons.AsrHttpHeaders;

/**
 * 
 * @author Viren
 *
 */
public final class JerseyModule extends JerseyServletModule {

	private static Logger logger = LoggerFactory.getLogger(JerseyModule.class);
	
    @Override
    protected void configureServlets() {
    	filter("/*").through(apiOriginFilter());
    	filter("/*").through(apiAuthFilter());
        
        Map<String, String> jerseyParams = new HashMap<>();	
		jerseyParams.put("com.sun.jersey.config.feature.FilterForwardOn404", "true");
		jerseyParams.put("com.sun.jersey.config.property.WebPageContentRegex", "/(((webjars|api-docs|swagger-ui/docs|manage)/.*)|(favicon\\.ico))");
		jerseyParams.put(PackagesResourceConfig.PROPERTY_PACKAGES, "com.netflix.conductor.server.resources;io.swagger.jaxrs.json;io.swagger.jaxrs.listing");
		jerseyParams.put(ResourceConfig.FEATURE_DISABLE_WADL, "false");
		serve("/api/*").with(GuiceContainer.class, jerseyParams);
    }
    
	@Provides
	@Singleton
	JacksonJsonProvider jacksonJsonProvider(ObjectMapper mapper) {
	    return new JacksonJsonProvider(mapper);
	}
	
	@Provides
    @Singleton
    public Filter apiOriginFilter() {
        return new Filter(){

			@Override
			public void init(FilterConfig filterConfig) throws ServletException {}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
				logger.info("apiOriginFilter: doFilter: executing");
		        HttpServletResponse res = (HttpServletResponse) response;
		        if (!res.containsHeader("Access-Control-Allow-Origin")) {
		            res.setHeader("Access-Control-Allow-Origin", "*");
		        }
		        res.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
		        res.addHeader("Access-Control-Allow-Headers", "Content-Type, api_key, Authorization");
		        
		        chain.doFilter(request, response);
		    }
			@Override
			public void destroy() {}
        	
        };
    }
	
	public class AuthFilter implements Filter{

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
			logger.info("apiAuthFilter: doFilter: executing");
			HttpServletRequest httpReq = (HttpServletRequest) request;
			String authHeader = httpReq.getHeader("Authorization");
			String apiKey = httpReq.getHeader(AsrHttpHeaders.API_KEY);
	        if ((authHeader != null && authHeader != "") 
	        		&& (apiKey != null && apiKey != "")){
	        	chain.doFilter(request, response);
	        }
	        else
	        {
	        	logger.info("No Authorization header. ");
	        	HttpServletResponse httpResp = (HttpServletResponse)response;
	        	httpResp.setStatus(HttpServletResponse.SC_FORBIDDEN);
	        }
	    }
		@Override
		public void destroy() {}
    	
    };
	@Provides
    @Singleton
    public AuthFilter apiAuthFilter() {
        return new AuthFilter();
    }
    @Override
    public boolean equals(Object obj) {
        return obj != null && getClass().equals(obj.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }


}
