package net.devstudy.resume.web.controller.api;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/csrf")
public class CsrfApiController {

    @GetMapping
    public CsrfResponse csrf(CsrfToken token) {
        if (token == null) {
            return new CsrfResponse("", "", "");
        }
        return new CsrfResponse(token.getHeaderName(), token.getParameterName(), token.getToken());
    }

    public record CsrfResponse(
            String headerName,
            String parameterName,
            String token
    ) {
    }
}
