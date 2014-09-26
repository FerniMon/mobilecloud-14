package org.magnum.mobilecloud.video.auth;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.config.annotation.builders.InMemoryClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Class with the purpose of configuring all the needed structures for OAuth2
 * security.
 * 
 *
 */
@Configuration
public class OAuth2SecurityConfig {
	
	// This first section of the configuration just makes sure that Spring Security picks
	// up the UserDetailsService that we create below. 
	@Configuration
	@EnableWebSecurity
	protected static class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
		
		@Autowired
		private UserDetailsService userDetailsService;
		
		@Autowired
		protected void registerAuthentication(
				final AuthenticationManagerBuilder auth) throws Exception {
			auth.userDetailsService(userDetailsService);
		}
	}

	/**
	 * This class serves as configurator of the OAuth2 authorizaton server
	 * according to our needs
	 * 
	 *
	 */
	@Configuration
	@EnableAuthorizationServer
	@Order(Ordered.LOWEST_PRECEDENCE - 100)
	protected static class OAuth2AuthSrvrConfig extends
			AuthorizationServerConfigurerAdapter {

		// Delegate the processing of Authentication requests to the framework
		@Autowired
		private AuthenticationManager authenticationManager;
		
		private ClientAndUserDetailsService combinedService_;
		
		public OAuth2AuthSrvrConfig() throws Exception {
			
			// Create the service with all the credentials for the clients
			ClientDetailsService clientsSvc = new InMemoryClientDetailsServiceBuilder()
					.withClient("mobile").authorizedGrantTypes("password")
					.authorities("ROLE_CLIENT", "ROLE_TRUSTED_CLIENT")
					.scopes("read", "write").resourceIds("video")
					.accessTokenValiditySeconds(3600).and().build();
			
			
			// Create the service with all the credentials for the users
			UserDetailsService usersSvc =  new InMemoryUserDetailsManager(
					Arrays.asList(
							User.create("admin", "pass", "ADMIN", "USER"),
							User.create("user0", "pass", "USER")));
			
			combinedService_ = new ClientAndUserDetailsService(clientsSvc, usersSvc);
		}
		
		/**
		 * Return the list of trusted client information to anyone who asks for it.
		 */
		@Bean
		public ClientDetailsService clientDetailsService() throws Exception {
			return combinedService_;
		}

		/**
		 * Return all of our user information to anyone in the framework who requests it.
		 */
		@Bean
		public UserDetailsService userDetailsService() {
			return combinedService_;
		}
		
		/**
		 * This method tells our AuthorizationServerConfigurerAdapter to use the delegated AuthenticationManager
		 * to process authentication requests.
		 */
		@Override
		public void configure(AuthorizationServerEndpointsConfigurer endpoints)
				throws Exception {
			endpoints.authenticationManager(authenticationManager);
		}

		/**
		 * This method tells the AuthorizationServerConfigurerAdapter to use our self-defined client details service to
		 * authenticate clients with.
		 */
		@Override
		public void configure(ClientDetailsServiceConfigurer clients)
				throws Exception {
			clients.withClientDetails(clientDetailsService());
		}
	}
	
	@Configuration
	@EnableResourceServer
	protected static class ResourceServerConfig extends ResourceServerConfigurerAdapter {
		
		@Override
		public void configure(HttpSecurity http) throws Exception {
			// Avoid CSRF attacks
			http.csrf().disable();
			
			http
			.authorizeRequests()
				.antMatchers("/oauth/token").anonymous();
			
			http
			.authorizeRequests()
				.antMatchers(HttpMethod.GET, "/**")
				.access("#oauth2.hasScope('read')");
			
			// Require all other requests to have "write" scope
			
			http
			.authorizeRequests()
				.antMatchers("/**")
				.access("#oauth2.hasScope('write')");	
		}
		
	}

}
