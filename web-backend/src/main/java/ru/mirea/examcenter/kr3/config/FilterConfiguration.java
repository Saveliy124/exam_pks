package ru.mirea.examcenter.kr3.config;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import ru.mirea.examcenter.kr3.security.*;
@Configuration
public class FilterConfiguration {
 @Bean FilterRegistrationBean<JwtAuthenticationFilter> jwtRegistration(JwtAuthenticationFilter f) {var b=new FilterRegistrationBean<>(f);b.setEnabled(false);return b;}
 @Bean FilterRegistrationBean<PasswordChangeRequiredFilter> passwordRegistration(PasswordChangeRequiredFilter f) {var b=new FilterRegistrationBean<>(f);b.setEnabled(false);return b;}
}
