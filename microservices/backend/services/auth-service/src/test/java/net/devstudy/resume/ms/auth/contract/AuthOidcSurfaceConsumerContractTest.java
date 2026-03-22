package net.devstudy.resume.ms.auth.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.yaml.snakeyaml.Yaml;

import net.devstudy.resume.web.controller.SessionApiController;
import net.devstudy.resume.web.controller.api.AccountApiController;
import net.devstudy.resume.web.controller.api.AuthApiController;
import net.devstudy.resume.web.controller.api.CsrfApiController;
import net.devstudy.resume.web.controller.api.PublicAuthApiController;

class AuthOidcSurfaceConsumerContractTest {

    private static final String AUTH_OPENAPI_RESOURCE = "contracts/auth/openapi.yaml";

    @Test
    void key_auth_rest_contract_should_match_expected_shape() {
        OpenApiContract contract = OpenApiContract.load(AUTH_OPENAPI_RESOURCE);

        contract.operation("/api/auth/register", HttpMethod.POST)
                .assertRequestJsonSchemaRef("#/components/schemas/RegistrationRequest")
                .assertResponseJsonSchemaRef("201", "#/components/schemas/SessionResponse")
                .assertStatusPresent("400")
                .assertStatusPresent("403")
                .assertStatusPresent("409");

        contract.operation("/api/auth/login", HttpMethod.POST)
                .assertRequestJsonSchemaRef("#/components/schemas/LoginRequest")
                .assertResponseJsonSchemaRef("200", "#/components/schemas/SessionResponse")
                .assertStatusPresent("400")
                .assertStatusPresent("401")
                .assertStatusPresent("429");

        contract.operation("/api/auth/logout-all", HttpMethod.POST)
                .assertStatusPresent("204")
                .assertStatusPresent("401");

        contract.operation("/api/auth/logout", HttpMethod.POST)
                .assertStatusPresent("204");

        contract.operation("/api/account/password", HttpMethod.POST)
                .assertRequestJsonSchemaRef("#/components/schemas/ChangePasswordRequest")
                .assertStatusPresent("204")
                .assertStatusPresent("400")
                .assertStatusPresent("401");

        contract.operation("/api/account/login", HttpMethod.POST)
                .assertRequestJsonSchemaRef("#/components/schemas/ChangeLoginRequest")
                .assertResponseJsonSchemaRef("200", "#/components/schemas/ChangeLoginResponse")
                .assertStatusPresent("400")
                .assertStatusPresent("401")
                .assertStatusPresent("409");

        contract.operation("/api/account/remove", HttpMethod.DELETE)
                .assertStatusPresent("204")
                .assertStatusPresent("401");

        contract.operation("/api/me", HttpMethod.GET)
                .assertResponseJsonSchemaRef("200", "#/components/schemas/SessionResponse");

        contract.operation("/api/csrf", HttpMethod.GET)
                .assertResponseJsonSchemaRef("200", "#/components/schemas/CsrfResponse");
    }

    @Test
    void key_auth_rest_controller_mappings_should_match_contract_endpoints() {
        Set<String> mappings = collectMappings(
                PublicAuthApiController.class,
                AuthApiController.class,
                AccountApiController.class,
                SessionApiController.class,
                CsrfApiController.class
        );

        assertThat(mappings).contains(
                "POST /api/auth/register",
                "POST /api/auth/login",
                "POST /api/auth/logout",
                "POST /api/auth/logout-all",
                "POST /api/account/password",
                "POST /api/account/login",
                "DELETE /api/account/remove",
                "GET /api/me",
                "GET /api/csrf"
        );
    }

    @Test
    void key_oidc_contract_should_match_expected_shape() {
        OpenApiContract contract = OpenApiContract.load(AUTH_OPENAPI_RESOURCE);

        contract.operation("/.well-known/openid-configuration", HttpMethod.GET)
                .assertResponseJsonSchemaRef("200", "#/components/schemas/OpenIdProviderConfiguration");

        contract.operation("/oauth2/authorize", HttpMethod.GET)
                .assertStatusPresent("302")
                .assertStatusPresent("400")
                .assertStatusPresent("401");

        contract.operation("/oauth2/token", HttpMethod.POST)
                .assertRequestContentTypePresent("application/x-www-form-urlencoded")
                .assertRequestContentSchemaRef("application/x-www-form-urlencoded",
                        "#/components/schemas/TokenRequestForm")
                .assertResponseJsonSchemaRef("200", "#/components/schemas/OidcTokenResponse")
                .assertStatusPresent("400")
                .assertStatusPresent("401");

        contract.operation("/oauth2/jwks", HttpMethod.GET)
                .assertResponseJsonSchemaRef("200", "#/components/schemas/JwkSet");

        contract.operation("/connect/logout", HttpMethod.GET)
                .assertStatusPresent("302");
    }

    @Test
    void oidc_default_surface_should_match_contract_endpoints() {
        AuthorizationServerSettings settings = AuthorizationServerSettings.builder()
                .issuer("https://auth.local")
                .build();

        OpenApiContract contract = OpenApiContract.load(AUTH_OPENAPI_RESOURCE);
        contract.operation("/.well-known/openid-configuration", HttpMethod.GET);
        contract.operation(settings.getAuthorizationEndpoint(), HttpMethod.GET);
        contract.operation(settings.getTokenEndpoint(), HttpMethod.POST);
        contract.operation(settings.getJwkSetEndpoint(), HttpMethod.GET);
        contract.operation(settings.getOidcLogoutEndpoint(), HttpMethod.GET);
    }

    private static Set<String> collectMappings(Class<?>... controllerTypes) {
        Set<String> mappings = new LinkedHashSet<>();
        for (Class<?> controllerType : controllerTypes) {
            for (String basePath : requestMappingPaths(controllerType.getAnnotation(RequestMapping.class))) {
                for (Method method : controllerType.getDeclaredMethods()) {
                    addMappings(mappings, basePath, method);
                }
            }
        }
        return mappings;
    }

    private static void addMappings(Set<String> mappings, String basePath, Method method) {
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        if (getMapping != null) {
            addHttpMappings(mappings, HttpMethod.GET, basePath, mappingPaths(getMapping.path(), getMapping.value()));
        }
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        if (postMapping != null) {
            addHttpMappings(mappings, HttpMethod.POST, basePath, mappingPaths(postMapping.path(), postMapping.value()));
        }
        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        if (putMapping != null) {
            addHttpMappings(mappings, HttpMethod.PUT, basePath, mappingPaths(putMapping.path(), putMapping.value()));
        }
        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        if (deleteMapping != null) {
            addHttpMappings(mappings,
                    HttpMethod.DELETE,
                    basePath,
                    mappingPaths(deleteMapping.path(), deleteMapping.value()));
        }
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if (requestMapping != null && requestMapping.method().length > 0) {
            String[] methodPaths = mappingPaths(requestMapping.path(), requestMapping.value());
            for (RequestMethod requestMethod : requestMapping.method()) {
                addHttpMappings(mappings, HttpMethod.valueOf(requestMethod.name()), basePath, methodPaths);
            }
        }
    }

    private static void addHttpMappings(Set<String> mappings, HttpMethod method, String basePath, String[] methodPaths) {
        for (String methodPath : methodPaths) {
            mappings.add(method.name() + " " + normalizePath(basePath, methodPath));
        }
    }

    private static String[] requestMappingPaths(RequestMapping requestMapping) {
        if (requestMapping == null) {
            return new String[]{""};
        }
        return mappingPaths(requestMapping.path(), requestMapping.value());
    }

    private static String[] mappingPaths(String[] path, String[] value) {
        if (path != null && path.length > 0) {
            return path;
        }
        if (value != null && value.length > 0) {
            return value;
        }
        return new String[]{""};
    }

    private static String normalizePath(String basePath, String methodPath) {
        String base = basePath == null ? "" : basePath.trim();
        String method = methodPath == null ? "" : methodPath.trim();
        if (base.isEmpty()) {
            return ensureLeadingSlash(method);
        }
        if (method.isEmpty()) {
            return ensureLeadingSlash(base);
        }
        String merged = ensureNoTrailingSlash(base) + "/" + ensureNoLeadingSlash(method);
        return ensureLeadingSlash(merged);
    }

    private static String ensureLeadingSlash(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String ensureNoLeadingSlash(String path) {
        if (path == null) {
            return "";
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private static String ensureNoTrailingSlash(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private static final class OpenApiContract {

        private final Map<String, Object> root;

        private OpenApiContract(Map<String, Object> root) {
            this.root = root;
        }

        static OpenApiContract load(String resourcePath) {
            try (InputStream inputStream = OpenApiContract.class.getClassLoader()
                    .getResourceAsStream(resourcePath)) {
                assertThat(inputStream)
                        .as("OpenAPI contract resource %s", resourcePath)
                        .isNotNull();
                String yaml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> parsed = new Yaml().load(yaml);
                assertThat(parsed).isNotNull();
                return new OpenApiContract(parsed);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to read OpenAPI contract: " + resourcePath, ex);
            }
        }

        ContractOperation operation(String path, HttpMethod method) {
            Map<String, Object> paths = asMap(root.get("paths"), "paths");
            Map<String, Object> pathItem = asMap(paths.get(path), "path " + path);
            String methodKey = method.name().toLowerCase(Locale.ROOT);
            Map<String, Object> operation = asMap(pathItem.get(methodKey), methodKey + " " + path);
            return new ContractOperation(path, method, operation);
        }
    }

    private static final class ContractOperation {

        private final String path;
        private final HttpMethod method;
        private final Map<String, Object> operation;

        private ContractOperation(String path, HttpMethod method, Map<String, Object> operation) {
            this.path = path;
            this.method = method;
            this.operation = operation;
        }

        ContractOperation assertStatusPresent(String statusCode) {
            assertThat(responses())
                    .as("responses for %s %s", method, path)
                    .containsKey(statusCode);
            return this;
        }

        ContractOperation assertRequestJsonSchemaRef(String expectedSchemaRef) {
            return assertRequestContentSchemaRef("application/json", expectedSchemaRef);
        }

        ContractOperation assertRequestContentTypePresent(String contentType) {
            Map<String, Object> requestBody = asMap(operation.get("requestBody"), "requestBody");
            Map<String, Object> content = asMap(requestBody.get("content"), "requestBody.content");
            assertThat(content)
                    .as("request content types for %s %s", method, path)
                    .containsKey(contentType);
            return this;
        }

        ContractOperation assertRequestContentSchemaRef(String contentType, String expectedSchemaRef) {
            Map<String, Object> requestBody = asMap(operation.get("requestBody"), "requestBody");
            Map<String, Object> content = asMap(requestBody.get("content"), "requestBody.content");
            Map<String, Object> typedContent = asMap(content.get(contentType), contentType);
            Map<String, Object> schema = asMap(typedContent.get("schema"), "request schema");
            assertThat(schema.get("$ref"))
                    .as("request schema ref for %s %s", method, path)
                    .isEqualTo(expectedSchemaRef);
            return this;
        }

        ContractOperation assertResponseJsonSchemaRef(String statusCode, String expectedSchemaRef) {
            Map<String, Object> response = asMap(responses().get(statusCode), "response " + statusCode);
            Map<String, Object> content = asMap(response.get("content"), "response content");
            Map<String, Object> jsonContent = asMap(content.get("application/json"), "application/json");
            Map<String, Object> schema = asMap(jsonContent.get("schema"), "response schema");
            assertThat(schema.get("$ref"))
                    .as("response schema ref for %s %s status %s", method, path, statusCode)
                    .isEqualTo(expectedSchemaRef);
            return this;
        }

        private Map<String, Object> responses() {
            return asMap(operation.get("responses"), "responses");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value, String location) {
        assertThat(value)
                .as(location)
                .isInstanceOf(Map.class);
        return (Map<String, Object>) value;
    }
}
