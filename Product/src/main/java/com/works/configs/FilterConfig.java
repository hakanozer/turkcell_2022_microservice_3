package com.works.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.works.props.Account;
import com.works.props.AccountData;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Response;
import java.io.IOException;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class FilterConfig implements Filter {

    final DiscoveryClient discoveryClient;
    final RestTemplate restTemplate;
    final ObjectMapper objectMapper;
    final Tracer tracer;

    @Override
    public void init(javax.servlet.FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse res = (HttpServletResponse) servletResponse;

        Span span = tracer.currentSpan();
        String spanId = span.context().spanId();
        String parentId = span.context().parentId();
        res.setHeader("spanId", spanId);
        res.setHeader("parentId", parentId);

        String url = req.getRequestURI();
        if (url.equals("/errorx")) {
            filterChain.doFilter(req, res);
        }else {
            boolean status = req.getSession().getAttribute("account") == null;
            if ( status ) {
            String email = req.getHeader("email");
            String password = req.getHeader("password");
            if (email == null || password == null) {
                res.sendRedirect("/errorx");
            }else {


                    List<ServiceInstance> instances = discoveryClient.getInstances("account");
                    ServiceInstance instance = instances.get(0);
                    String baseUrl = instance.getUri().toString();

                    // login
                    baseUrl = baseUrl + "/account/login";
                    Account account = new Account(email, password);
                    String jsonObj = objectMapper.writeValueAsString(account);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity httpEntity = new HttpEntity(jsonObj, headers);
                    ResponseEntity<AccountData> data = restTemplate.postForEntity(baseUrl, httpEntity, AccountData.class);
                    int statusCode = data.getStatusCodeValue();
                    if (statusCode == 200) {
                        AccountData accountData = data.getBody();
                        System.out.println(accountData.getResult().getAid());
                        req.getSession().setAttribute("account", accountData);
                        filterChain.doFilter(req, res);
                    } else {
                        res.sendRedirect("/errorx");
                    }
                }
            }else {
                filterChain.doFilter(req, res);
            }
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
