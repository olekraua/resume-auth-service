package net.devstudy.resume.ms.auth.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "net.devstudy.resume")
class PackageBoundariesArchTest {

    private static final String LOCAL_MS_AUTH_PACKAGE = "net.devstudy.resume.ms.auth.";
    private static final String LOCAL_LEGACY_AUTH_PACKAGE = "net.devstudy.resume.auth.";

    @ArchTest
    static final ArchRule local_code_should_not_depend_on_unrelated_service_packages = noClasses()
            .that()
            .resideInAnyPackage(
                    "net.devstudy.resume.ms.auth..",
                    "net.devstudy.resume.auth..",
                    "net.devstudy.resume.web..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "net.devstudy.resume.file..",
                    "net.devstudy.resume.media..",
                    "net.devstudy.resume.search..",
                    "net.devstudy.resume.staticdata..",
                    "net.devstudy.resume.messaging..");

    @ArchTest
    static final ArchRule auth_public_api_should_not_depend_on_internal_or_web = noClasses()
            .that()
            .resideInAnyPackage(
                    "net.devstudy.resume.auth.api.dto..",
                    "net.devstudy.resume.auth.api.model..",
                    "net.devstudy.resume.auth.api.security..",
                    "net.devstudy.resume.auth.api.service..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "net.devstudy.resume.auth.internal..",
                    "net.devstudy.resume.web..",
                    "net.devstudy.resume.ms.auth..");

    @ArchTest
    static final ArchRule domain_data_packages_should_not_depend_on_transport_packages = noClasses()
            .that()
            .resideInAnyPackage(
                    LOCAL_MS_AUTH_PACKAGE + "model..",
                    LOCAL_MS_AUTH_PACKAGE + "entity..",
                    LOCAL_MS_AUTH_PACKAGE + "event..",
                    LOCAL_MS_AUTH_PACKAGE + "dto..",
                    LOCAL_LEGACY_AUTH_PACKAGE + "model..",
                    LOCAL_LEGACY_AUTH_PACKAGE + "entity..",
                    LOCAL_LEGACY_AUTH_PACKAGE + "event..",
                    LOCAL_LEGACY_AUTH_PACKAGE + "dto..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    LOCAL_MS_AUTH_PACKAGE + "web..",
                    LOCAL_MS_AUTH_PACKAGE + "controller..",
                    LOCAL_MS_AUTH_PACKAGE + "messaging..",
                    LOCAL_MS_AUTH_PACKAGE + "mqtt..",
                    LOCAL_MS_AUTH_PACKAGE + "ws..",
                    LOCAL_LEGACY_AUTH_PACKAGE + "web..",
                    "net.devstudy.resume.web..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule persistence_and_outbox_packages_should_not_depend_on_transport_packages = noClasses()
            .that()
            .resideInAnyPackage(
                    LOCAL_MS_AUTH_PACKAGE + "repository..",
                    LOCAL_MS_AUTH_PACKAGE + "outbox..",
                    LOCAL_LEGACY_AUTH_PACKAGE + "repository..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    LOCAL_MS_AUTH_PACKAGE + "web..",
                    LOCAL_MS_AUTH_PACKAGE + "controller..",
                    LOCAL_MS_AUTH_PACKAGE + "mqtt..",
                    LOCAL_MS_AUTH_PACKAGE + "ws..",
                    LOCAL_LEGACY_AUTH_PACKAGE + "web..",
                    "net.devstudy.resume.web..")
            .allowEmptyShould(true);
}
