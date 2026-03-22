package net.devstudy.resume.ms.auth.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.yaml.snakeyaml.Yaml;

import net.devstudy.resume.auth.internal.client.HttpProfileInternalClient;
import net.devstudy.resume.profile.api.dto.internal.ProfileAuthResponse;
import net.devstudy.resume.profile.api.dto.internal.ProfileIdentifierLookupRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileLookupResponse;
import net.devstudy.resume.profile.api.dto.internal.ProfileRegistrationRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileUidUpdateRequest;

class ProfileInternalApiConsumerContractTest {

    private static final String PROFILE_OPENAPI_RESOURCE = "contracts/profile/openapi.yaml";
    private static final String PROFILE_BASE_URL = "http://profile-service.local";

    private MockRestServiceServer server;
    private HttpProfileInternalClient client;
    private OpenApiContract contract;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROFILE_BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpProfileInternalClient(builder.build());
        contract = OpenApiContract.load(PROFILE_OPENAPI_RESOURCE);
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    void register_should_match_contract_method_path_status_and_body() {
        ContractOperation operation = contract.operation("/internal/profiles", HttpMethod.POST);
        operation.assertRequestJsonSchemaRef("#/components/schemas/ProfileRegistrationRequest");
        operation.assertResponseJsonSchemaRef("200", "#/components/schemas/ProfileAuthResponse");
        operation.assertStatusPresent("400");
        operation.assertStatusPresent("409");

        server.expect(requestTo(PROFILE_BASE_URL + "/internal/profiles"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.uid").value("john-doe"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.password").value("secret123"))
                .andRespond(withSuccess("""
                        {
                          "id": 101,
                          "uid": "john-doe",
                          "passwordHash": "{bcrypt}hash",
                          "firstName": "John",
                          "lastName": "Doe",
                          "email": "john@example.com",
                          "phone": "+12025550123"
                        }
                        """, MediaType.APPLICATION_JSON));

        ProfileAuthResponse response = client.register(
                new ProfileRegistrationRequest("john-doe", "John", "Doe", "secret123"));

        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.uid()).isEqualTo("john-doe");
    }

    @Test
    void lookup_should_match_contract_method_path_status_and_body() {
        ContractOperation operation = contract.operation("/internal/profiles/lookup", HttpMethod.POST);
        operation.assertRequestJsonSchemaRef("#/components/schemas/ProfileIdentifierLookupRequest");
        operation.assertResponseJsonSchemaRef("200", "#/components/schemas/ProfileLookupResponse");
        operation.assertStatusPresent("400");
        operation.assertStatusPresent("404");

        server.expect(requestTo(PROFILE_BASE_URL + "/internal/profiles/lookup"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.identifier").value("john-doe"))
                .andRespond(withSuccess("""
                        {
                          "id": 101,
                          "uid": "john-doe",
                          "email": "john@example.com",
                          "phone": "+12025550123",
                          "firstName": "John",
                          "lastName": "Doe"
                        }
                        """, MediaType.APPLICATION_JSON));

        ProfileLookupResponse response = client.lookup(new ProfileIdentifierLookupRequest("john-doe"));

        assertThat(response).isNotNull();
        assertThat(response.uid()).isEqualTo("john-doe");
        assertThat(response.email()).isEqualTo("john@example.com");
    }

    @Test
    void lookup_not_found_should_follow_contract_status() {
        ContractOperation operation = contract.operation("/internal/profiles/lookup", HttpMethod.POST);
        operation.assertStatusPresent("404");

        server.expect(requestTo(PROFILE_BASE_URL + "/internal/profiles/lookup"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        ProfileLookupResponse response = client.lookup(new ProfileIdentifierLookupRequest("unknown"));
        assertThat(response).isNull();
    }

    @Test
    void update_uid_should_match_contract_method_path_status_and_body() {
        ContractOperation operation = contract.operation("/internal/profiles/{id}/uid", HttpMethod.PUT);
        operation.assertRequestJsonSchemaRef("#/components/schemas/ProfileUidUpdateRequest");
        operation.assertStatusPresent("204");
        operation.assertStatusPresent("400");
        operation.assertStatusPresent("409");

        server.expect(requestTo(PROFILE_BASE_URL + "/internal/profiles/42/uid"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.uid").value("new-uid"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.updateUid(42L, new ProfileUidUpdateRequest("new-uid"));
    }

    @Test
    void uid_exists_should_match_contract_method_path_status_and_body() {
        ContractOperation operation = contract.operation("/internal/profiles/exists/uid/{uid}", HttpMethod.GET);
        operation.assertResponseJsonBoolean("200");

        server.expect(requestTo(PROFILE_BASE_URL + "/internal/profiles/exists/uid/john-doe"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("true", MediaType.APPLICATION_JSON));

        assertThat(client.uidExists("john-doe")).isTrue();
    }

    @Test
    void remove_profile_should_match_contract_method_path_status() {
        ContractOperation operation = contract.operation("/internal/profiles/{id}", HttpMethod.DELETE);
        operation.assertStatusPresent("204");
        operation.assertStatusPresent("400");

        server.expect(requestTo(PROFILE_BASE_URL + "/internal/profiles/42"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.removeProfile(42L);
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

        void assertStatusPresent(String statusCode) {
            Map<String, Object> responses = responses();
            assertThat(responses)
                    .as("responses for %s %s", method, path)
                    .containsKey(statusCode);
        }

        void assertRequestJsonSchemaRef(String expectedSchemaRef) {
            Map<String, Object> requestBody = asMap(operation.get("requestBody"), "requestBody");
            Map<String, Object> content = asMap(requestBody.get("content"), "requestBody.content");
            Map<String, Object> jsonContent = asMap(content.get("application/json"), "application/json");
            Map<String, Object> schema = asMap(jsonContent.get("schema"), "request schema");
            assertThat(schema.get("$ref"))
                    .as("request schema ref for %s %s", method, path)
                    .isEqualTo(expectedSchemaRef);
        }

        void assertResponseJsonSchemaRef(String statusCode, String expectedSchemaRef) {
            Map<String, Object> response = asMap(responses().get(statusCode), "response " + statusCode);
            Map<String, Object> content = asMap(response.get("content"), "response content");
            Map<String, Object> jsonContent = asMap(content.get("application/json"), "application/json");
            Map<String, Object> schema = asMap(jsonContent.get("schema"), "response schema");
            assertThat(schema.get("$ref"))
                    .as("response schema ref for %s %s status %s", method, path, statusCode)
                    .isEqualTo(expectedSchemaRef);
        }

        void assertResponseJsonBoolean(String statusCode) {
            Map<String, Object> response = asMap(responses().get(statusCode), "response " + statusCode);
            Map<String, Object> content = asMap(response.get("content"), "response content");
            Map<String, Object> jsonContent = asMap(content.get("application/json"), "application/json");
            Map<String, Object> schema = asMap(jsonContent.get("schema"), "response schema");
            assertThat(schema.get("type"))
                    .as("response type for %s %s status %s", method, path, statusCode)
                    .isEqualTo("boolean");
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
