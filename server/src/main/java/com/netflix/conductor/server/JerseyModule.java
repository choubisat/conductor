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
import java.util.Enumeration;
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
import javax.ws.rs.HeaderParam;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.Provides;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.adobe.asr.autoconfigure.ims.IMSAutoConfiguration;
//import com.adobe.asr.annotations.cors.CrossOriginResourceSharing;
import com.adobe.asr.autoconfigure.ims.annotation.IMSServiceAuthContext;
import com.adobe.asr.autoconfigure.ims.annotation.IMSServiceAuthentication;
import com.adobe.asr.autoconfigure.ims.annotation.IMSUserAuthContext;
import com.adobe.asr.autoconfigure.ims.annotation.IMSUserAuthentication;
import com.adobe.asr.autoconfigure.ims.dto.IMSTokenInfo;
import com.adobe.asr.commons.AsrHttpHeaders;
import com.adobe.asr.connector.ims.IMSConnector;
import com.adobe.asr.connector.ims.dto.ValidateIMSTokenResponse;
import com.adobe.asr.imsutils.dto.ImsTokenPayload;
import com.adobe.asr.imsutils.exception.ImsTokenUtilException;
import com.adobe.asr.validators.IMSTokenValidator;
import com.adobe.asr.imsutils.ImsTokenUtil;
import com.adobe.asr.autoconfigure.ims.dto.IMSTokenInfo;
import com.adobe.asr.connector.ims.dto.IMSToken;

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
		//filter("/api/admin" + "*").through(apiAuthFilter());

		Map<String, String> jerseyParams = new HashMap<>();
		jerseyParams.put("com.sun.jersey.config.feature.FilterForwardOn404", "true");
		jerseyParams.put("com.sun.jersey.config.property.WebPageContentRegex",
				"/(((webjars|api-docs|swagger-ui/docs|manage)/.*)|(favicon\\.ico))");
		jerseyParams.put(PackagesResourceConfig.PROPERTY_PACKAGES,
				"com.netflix.conductor.server.resources;io.swagger.jaxrs.json;io.swagger.jaxrs.listing");
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
		return new Filter() {

			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {
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
			public void destroy() {
			}

		};
	}

	public class AuthFilter implements Filter {

		@Autowired
		private IMSAutoConfiguration imsConfig;

		@Autowired
		private ImsTokenUtil imsTokenUtil;

		private final static String validClient = "redhawkadmin2";
		public static final String IMS_BEARER = "Bearer ";

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			logger.info("apiAuthFilter: doFilter: executing");
			HttpServletRequest httpReq = (HttpServletRequest) request;
			Boolean isAuth = authenticate(httpReq);
			if (isAuth) {
				chain.doFilter(request, response);
			} else {
				logger.info("Could not validate the request. Please check authorization headers.");
				HttpServletResponse httpResp = (HttpServletResponse) response;
				httpResp.setStatus(HttpServletResponse.SC_FORBIDDEN);
			}
		}

		@Override
		public void destroy() {
		}
		
		  private String sanitiseAuthToken(String accessToken) {
			    String cleanTokenStr = accessToken;
			    String bearerStr = IMS_BEARER;
		        if (accessToken != null && accessToken.toLowerCase().startsWith(bearerStr.toLowerCase())) {
		          cleanTokenStr = accessToken.substring(bearerStr.length());
		        }
			    return cleanTokenStr;
			  }

		private Boolean authenticate(HttpServletRequest request) {
			Boolean isAuth = false;
			try {
				String userAccessToken = request.getHeader(AsrHttpHeaders.USER_ACCESS_TOKEN);
				logger.info("["+AsrHttpHeaders.USER_ACCESS_TOKEN+"] = " + userAccessToken);
				ValidateIMSTokenResponse validateIMSTokenResponse = validateAndGetIMSTokenInfo(sanitiseAuthToken(userAccessToken));
				IMSToken userTokenInfo = validateIMSTokenResponse.getImsToken();
				//String serviceAccessToken = request.getHeader(AsrHttpHeaders.SERVICE_ACCESS_TOKEN);
				
				String clientId = request.getHeader(AsrHttpHeaders.IMS_CLIENT_ID);
				logger.info("["+AsrHttpHeaders.IMS_CLIENT_ID+"] = " + clientId);
				// String apiKey = request.getHeader(AsrHttpHeaders.API_KEY);
				//IMSTokenInfo serviceToken = validateImsServiceAuth(serviceAccessToken, clientId);
				if (validClient.equals(clientId))
					isAuth = true;
			} catch (Exception e) {
				isAuth = false;
			}
			return isAuth;
		}
		
	  private ValidateIMSTokenResponse validateAndGetIMSTokenInfo(String accessToken)
		      throws Exception {
		    ValidateIMSTokenResponse validateIMSTokenResponse = null;
		    try {
		    	logger.info("in validateAndGetIMSTokenInfo accessToken= {}",accessToken);
		    	IMSConnector ims = imsConfig.createIMSConnector();
		      validateIMSTokenResponse =
		    		  ims.validateAccessToken("\\Eureka\\IMS\\prod\\IMS_prod_eureka_service", accessToken);
		    } catch (Exception e) {
		    	logger.info("Exception validateAndGetIMSTokenInfo" + e);
		    	e.printStackTrace();
		      throw new Exception("Error while calling IMS validate AccessToken " + e);
		    }
		    if (validateIMSTokenResponse == null || !validateIMSTokenResponse.isTokenValid()) {
		      throw new Exception("Invalid IMS Access token");
		    }
		    return validateIMSTokenResponse;
		  }

		public IMSTokenInfo validateImsServiceAuth(String imsAccessToken, String imsClientId) throws Exception {

			// validate input headers values
			if (StringUtils.isBlank(imsAccessToken) || StringUtils.isBlank(imsClientId)) {
				logger.info("WorkflowAuth : IMS Service token or client id is missing");
				throw new Exception(AsrHttpHeaders.IMS_CLIENT_ID + " and " + AsrHttpHeaders.SERVICE_ACCESS_TOKEN
						+ " headers are required");
			}

			// Validating the accessToken format
			if (imsAccessToken != null && !IMSTokenValidator.validateToken(imsAccessToken)) {
				logger.info("WorkflowAuth : IMS Service token is in invalid format");
				throw new Exception("Invalid token in " + AsrHttpHeaders.SERVICE_ACCESS_TOKEN + " header.");
			}
			logger.info("WorkflowAuth : IMS Service token authentication in progress");

			// try to decode access token and validate client id and user id
			ImsTokenPayload imsTokenPayload = null;
			try {
				imsTokenPayload = imsTokenUtil.decodeTokenPayload(imsAccessToken);
			} catch (ImsTokenUtilException imsTokenUtilException) {
				logger.info("WorkflowAuth : IMS Service token decoding failure");
				throw new Exception(imsTokenUtilException.getMessage());
			}

			String imsClientIdInToken = imsTokenPayload.getClientId();
			String imsUserIdInToken = imsTokenPayload.getUserId();

			if (!imsClientId.equalsIgnoreCase(imsClientIdInToken)) {
				logger.info(
						"WorkflowAuth : Client id header and the client id in token don't match. [In Token - {}] [In Header - {}]",
						imsClientIdInToken, imsClientId);
				throw new Exception("Unauthorized");
			}

			// if
			// (!reviewServiceProps.getWhitelistedServiceClientIds().contains(imsClientIdInToken)
			// ||
			// !reviewServiceProps.getWhitelistedServiceUserIds().contains(imsUserIdInToken))
			// {
			// logger.info("WorkflowAuth : IMS Service User not whitelisted");
			// throw new Exception();
			// }

			IMSConnector ims = imsConfig.createIMSConnector();
			ValidateIMSTokenResponse validateIMSTokenResponse = ims.validateAccessToken(imsClientIdInToken,
					imsAccessToken);

			IMSTokenInfo tokenInfoDTO = null;
			if (validateIMSTokenResponse.isTokenValid()) {
				tokenInfoDTO = new IMSTokenInfo();
				tokenInfoDTO.setExpiresAtStr(validateIMSTokenResponse.getExpiresAtStr());
				tokenInfoDTO.setExpiresAtTime(validateIMSTokenResponse.getExpiresAtTime());
				tokenInfoDTO.setFailureReason(validateIMSTokenResponse.getFailureReason());
				tokenInfoDTO.setImsToken(validateIMSTokenResponse.getImsToken());
				tokenInfoDTO.setTokenValid(validateIMSTokenResponse.isTokenValid());
				tokenInfoDTO.setImsTokenStr(imsAccessToken);
			} else {
				logger.info("WorkflowAuth : Invalid service auth token");
				throw new Exception("Invalid Service Token");
			}
			return tokenInfoDTO;
		}

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
