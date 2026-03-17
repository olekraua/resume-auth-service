package net.devstudy.resume.ms.auth.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.devstudy.resume.auth.internal.entity.AuthOutboxEvent;
import net.devstudy.resume.auth.internal.outbox.AuthOutboxWriter;
import net.devstudy.resume.auth.internal.repository.storage.AuthOutboxRepository;
import net.devstudy.resume.notification.api.event.RestoreAccessMailRequestedEvent;

class AuthRestoreAccessMailAsyncConsumerContractTest {

    private static final String AUTH_ASYNCAPI_RESOURCE = "contracts/auth/asyncapi.yaml";
    private static final String EVENT_NAME = "RestoreAccessMailRequested";
    private static final String OUTBOX_HEADERS_TRAIT = "OutboxHeaders";
    private static final String SOURCE_SERVICE = "resume-auth-service";
    private static final Pattern SIMPLE_EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void restore_access_mail_outbox_payload_and_headers_should_match_asyncapi_contract() throws Exception {
        AsyncApiContract contract = AsyncApiContract.load(AUTH_ASYNCAPI_RESOURCE);
        AsyncMessageContract message = contract.message(EVENT_NAME);
        AsyncObjectSchema payloadSchema = message.payloadSchema(contract);
        AsyncObjectSchema headersSchema = message.headersSchema(contract, OUTBOX_HEADERS_TRAIT);

        AuthOutboxRepository outboxRepository = mock(AuthOutboxRepository.class);
        when(outboxRepository.save(any(AuthOutboxEvent.class))).thenAnswer(invocation -> {
            AuthOutboxEvent saved = invocation.getArgument(0, AuthOutboxEvent.class);
            saved.setId(101L);
            return saved;
        });
        AuthOutboxWriter authOutboxWriter = new AuthOutboxWriter(outboxRepository, objectMapper);

        RestoreAccessMailRequestedEvent restoreEvent = new RestoreAccessMailRequestedEvent(
                "user@example.com",
                "User",
                "https://app.local/restore/token"
        );

        authOutboxWriter.enqueueRestoreAccessMail(restoreEvent);

        ArgumentCaptor<AuthOutboxEvent> savedEventCaptor = ArgumentCaptor.forClass(AuthOutboxEvent.class);
        verify(outboxRepository).save(savedEventCaptor.capture());
        AuthOutboxEvent saved = savedEventCaptor.getValue();
        saved.setId(101L);

        Map<String, Object> payload = objectMapper.readValue(
                saved.getPayload(),
                new TypeReference<Map<String, Object>>() {
                }
        );
        payloadSchema.assertValidObject(payload);

        Map<String, Object> headers = buildRelayHeaders(saved);
        headersSchema.assertValidObject(headers);
    }

    private Map<String, Object> buildRelayHeaders(AuthOutboxEvent event) {
        assertThat(event.getId()).as("event id").isNotNull();
        assertThat(event.getEventType()).as("event type").isNotNull();
        assertThat(event.getCreatedAt()).as("createdAt").isNotNull();
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("x-event-id", event.getId().toString());
        headers.put("x-event-type", event.getEventType().name());
        headers.put("x-source-service", SOURCE_SERVICE);
        headers.put("x-occurred-at", event.getCreatedAt().toString());
        return headers;
    }

    private static final class AsyncApiContract {

        private final Map<String, Object> root;

        private AsyncApiContract(Map<String, Object> root) {
            this.root = root;
        }

        static AsyncApiContract load(String resourcePath) {
            try (InputStream inputStream = AsyncApiContract.class.getClassLoader()
                    .getResourceAsStream(resourcePath)) {
                assertThat(inputStream)
                        .as("AsyncAPI resource %s", resourcePath)
                        .isNotNull();
                String yaml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> parsed = new Yaml().load(yaml);
                assertThat(parsed).isNotNull();
                return new AsyncApiContract(parsed);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to read AsyncAPI contract: " + resourcePath, ex);
            }
        }

        AsyncMessageContract message(String messageName) {
            Map<String, Object> messages = asMap(components().get("messages"), "components.messages");
            Map<String, Object> message = asMap(messages.get(messageName), "message " + messageName);
            return new AsyncMessageContract(messageName, message);
        }

        AsyncObjectSchema schemaByRef(String ref) {
            String schemaName = refToComponentName(ref, "schemas");
            Map<String, Object> schemas = asMap(components().get("schemas"), "components.schemas");
            Map<String, Object> schema = asMap(schemas.get(schemaName), "schema " + schemaName);
            return AsyncObjectSchema.from(schemaName, schema);
        }

        AsyncObjectSchema messageTraitHeaders(String traitName) {
            Map<String, Object> traits = asMap(components().get("messageTraits"), "components.messageTraits");
            Map<String, Object> trait = asMap(traits.get(traitName), "message trait " + traitName);
            Map<String, Object> headers = asMap(trait.get("headers"), "trait headers");
            return AsyncObjectSchema.from(traitName + ".headers", headers);
        }

        private Map<String, Object> components() {
            return asMap(root.get("components"), "components");
        }
    }

    private static final class AsyncMessageContract {

        private final String messageName;
        private final Map<String, Object> message;

        private AsyncMessageContract(String messageName, Map<String, Object> message) {
            this.messageName = messageName;
            this.message = message;
        }

        AsyncObjectSchema payloadSchema(AsyncApiContract contract) {
            Map<String, Object> payload = asMap(message.get("payload"), "payload for " + messageName);
            String ref = asString(payload.get("$ref"), "payload.$ref");
            return contract.schemaByRef(ref);
        }

        AsyncObjectSchema headersSchema(AsyncApiContract contract, String expectedTraitName) {
            List<Object> traits = asList(message.get("traits"), "traits for " + messageName);
            assertThat(traits).isNotEmpty();
            Map<String, Object> trait = asMap(traits.get(0), "first trait");
            String ref = asString(trait.get("$ref"), "trait.$ref");
            String traitName = refToComponentName(ref, "messageTraits");
            assertThat(traitName).as("message trait name").isEqualTo(expectedTraitName);
            return contract.messageTraitHeaders(traitName);
        }
    }

    private static final class AsyncObjectSchema {

        private final String name;
        private final Set<String> required;
        private final Map<String, Object> properties;

        private AsyncObjectSchema(String name, Set<String> required, Map<String, Object> properties) {
            this.name = name;
            this.required = required;
            this.properties = properties;
        }

        static AsyncObjectSchema from(String name, Map<String, Object> schema) {
            assertThat(schema.get("type")).as("schema type for %s", name).isEqualTo("object");
            List<Object> requiredRaw = asList(schema.get("required"), "required fields of " + name);
            Set<String> required = requiredRaw.stream()
                    .map(field -> asString(field, "required field"))
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
            Map<String, Object> properties = asMap(schema.get("properties"), "properties of " + name);
            return new AsyncObjectSchema(name, required, properties);
        }

        void assertValidObject(Map<String, Object> value) {
            assertThat(value).as("value for schema %s", name).isNotNull();
            assertThat(value.keySet()).as("required fields for schema %s", name).containsAll(required);
            for (String requiredField : required) {
                assertThat(value.get(requiredField))
                        .as("required value '%s' for schema %s", requiredField, name)
                        .isNotNull();
            }
            for (Map.Entry<String, Object> entry : value.entrySet()) {
                Map<String, Object> propertySchema = asMap(properties.get(entry.getKey()),
                        "property schema " + entry.getKey());
                validateProperty(entry.getKey(), entry.getValue(), propertySchema);
            }
        }

        private void validateProperty(String fieldName, Object fieldValue, Map<String, Object> schema) {
            assertThat(schema.get("type"))
                    .as("type for '%s' in schema %s", fieldName, name)
                    .isEqualTo("string");
            assertThat(fieldValue).as("value type for '%s' in schema %s", fieldName, name).isInstanceOf(String.class);
            String stringValue = (String) fieldValue;
            assertThat(stringValue).as("value for '%s' in schema %s", fieldName, name).isNotBlank();

            Object enumRaw = schema.get("enum");
            if (enumRaw != null) {
                List<Object> enumValues = asList(enumRaw, "enum for " + fieldName);
                assertThat(enumValues).contains(stringValue);
            }

            Object formatRaw = schema.get("format");
            if (formatRaw != null) {
                String format = asString(formatRaw, "format");
                if ("date-time".equals(format)) {
                    Instant.parse(stringValue);
                } else if ("email".equals(format)) {
                    assertThat(SIMPLE_EMAIL_PATTERN.matcher(stringValue).matches())
                            .as("email format for '%s' in schema %s", fieldName, name)
                            .isTrue();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value, String location) {
        assertThat(value).as(location).isInstanceOf(Map.class);
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object value, String location) {
        assertThat(value).as(location).isInstanceOf(List.class);
        return (List<Object>) value;
    }

    private static String asString(Object value, String location) {
        assertThat(value).as(location).isInstanceOf(String.class);
        return (String) value;
    }

    private static String refToComponentName(String ref, String componentSection) {
        String prefix = "#/components/" + componentSection + "/";
        assertThat(ref).as("reference prefix").startsWith(prefix);
        return ref.substring(prefix.length());
    }
}
