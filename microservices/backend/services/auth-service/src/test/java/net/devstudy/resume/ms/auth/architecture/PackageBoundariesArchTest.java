package net.devstudy.resume.ms.auth.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "net.devstudy.resume")
class PackageBoundariesArchTest {

    private static final String LOCAL_MS_AUTH_PACKAGE = "net.devstudy.resume.ms.auth.";

    @ArchTest
    static final ArchRule local_code_should_not_depend_on_unrelated_service_packages = noClasses()
            .that()
            .resideInAnyPackage(
                    "net.devstudy.resume.ms.auth..",
                    "net.devstudy.resume.ms.auth.adapters.web..")
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
                    "net.devstudy.resume.ms.auth.api.dto..",
                    "net.devstudy.resume.ms.auth.api.model..",
                    "net.devstudy.resume.ms.auth.application.port.in.security..",
                    "net.devstudy.resume.ms.auth.application.port.in.service..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "net.devstudy.resume.ms.auth.adapters.web..",
                    "net.devstudy.resume.ms.auth.application.service.impl..",
                    "net.devstudy.resume.ms.auth.config..");

    @ArchTest
    static final ArchRule domain_data_packages_should_not_depend_on_transport_packages = noClasses()
            .that()
            .resideInAnyPackage(
                    LOCAL_MS_AUTH_PACKAGE + "domain.entity..",
                    LOCAL_MS_AUTH_PACKAGE + "domain.model..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    LOCAL_MS_AUTH_PACKAGE + "adapters.web..",
                    LOCAL_MS_AUTH_PACKAGE + "adapters.profile..",
                    LOCAL_MS_AUTH_PACKAGE + "config..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule persistence_and_outbox_packages_should_not_depend_on_transport_packages = noClasses()
            .that()
            .resideInAnyPackage(
                    LOCAL_MS_AUTH_PACKAGE + "adapters.persistence..",
                    LOCAL_MS_AUTH_PACKAGE + "adapters.outbox..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    LOCAL_MS_AUTH_PACKAGE + "adapters.web..")
            .allowEmptyShould(true);
}
