package com.training.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${trainer.password:trainer123}") // Значение по умолчанию
    private String trainerPassword;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/", "/book/**", "/login", "/css/**", "/js/**", "/webjars/**").permitAll()
                .antMatchers("/admin/**").authenticated()
                .anyRequest().permitAll()
            .and()
            .formLogin()
                .loginPage("/login")
                .defaultSuccessUrl("/admin", true)
                .permitAll()
            .and()
            .logout()
                .logoutSuccessUrl("/")
                .permitAll()
            .and()
            .rememberMe()
                .key("uniqueAndSecretKey123")
                .tokenValiditySeconds(86400); // 24 часа
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
            .antMatchers(
                "/css/**",
                "/js/**",
                "/images/**",
                "/webjars/**"
            );
    }

    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        UserDetails trainer = User.builder()
            .username("trainer")
            .password(passwordEncoder().encode(trainerPassword))
            .roles("TRAINER")
            .build();

        return new InMemoryUserDetailsManager(trainer);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}