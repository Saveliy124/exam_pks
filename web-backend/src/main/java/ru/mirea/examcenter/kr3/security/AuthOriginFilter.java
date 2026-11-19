package ru.mirea.examcenter.kr3.security;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.mirea.examcenter.kr3.config.AppProperties;
import ru.mirea.examcenter.kr3.dto.ApiError;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
@Component
public class AuthOriginFilter extends OncePerRequestFilter {
 private final AppProperties props; private final ObjectMapper json;
 public AuthOriginFilter(AppProperties props,ObjectMapper json) {this.props=props;this.json=json;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws IOException,ServletException {
  String origin=req.getHeader("Origin");
  if(req.getRequestURI().startsWith("/api/auth/") && !req.getMethod().equals("GET") && !req.getMethod().equals("OPTIONS") && origin!=null && !props.cors().asList().contains(origin)) {
   res.setStatus(403);res.setContentType("application/json");json.writeValue(res.getOutputStream(),new ApiError(Instant.now(),403,"ACCESS_DENIED","Источник запроса не разрешен",req.getRequestURI(),res.getHeader("X-Correlation-ID"),Map.of()));return;
  }
  chain.doFilter(req,res);
 }
}
