package net.devstudy.resume.ms.auth.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "net.devstudy.resume")
class PackageBoundariesArchTest {

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
}
