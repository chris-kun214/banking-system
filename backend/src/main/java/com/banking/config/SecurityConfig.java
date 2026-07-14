package com.banking.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	// 逗号分隔的允许来源，本地默认放行 Vite dev server；生产环境通过环境变量覆盖成实际前端域名
	@Value("${app.cors.allowed-origins:http://localhost:5173}")
	private String allowedOrigins;

	/**
	 * 配置密码加密器
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * 配置认证管理器
	 */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	/**
	 * 配置 CORS，允许前端（本地 Vite dev server / 生产前端域名）跨域调用
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	/**
	 * 配置安全过滤器链
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// 启用上面配置的 CORS
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))

				// 禁用 CSRF（因为使用 JWT）
				.csrf(AbstractHttpConfigurer::disable)

				// 配置异常处理
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(jwtAuthenticationEntryPoint))

				// 配置会话管理（无状态）
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// 配置授权规则
				.authorizeHttpRequests(auth -> auth
						// 公开端点
						.requestMatchers(
								"/api/auth/**",
								"/api/public/**",
								"/error",
								"/swagger-ui/**",
								"/v3/api-docs/**",
								"/actuator/health")
						.permitAll()

						// 内部端点：不走用户 JWT，由 InternalApiKeyFilter 用共享密钥单独鉴权
						.requestMatchers("/api/internal/**").permitAll()

						// 管理员端点
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						// 其余 actuator 端点（metrics/info 等）可能暴露内部细节，仅管理员可访问
						.requestMatchers("/actuator/**").hasRole("ADMIN")

						// 其他所有请求需要认证
						.anyRequest().authenticated())

				// 添加 JWT 过滤器
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
