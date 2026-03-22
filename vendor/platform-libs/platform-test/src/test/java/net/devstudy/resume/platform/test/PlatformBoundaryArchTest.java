package net.devstudy.resume.platform.test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "net.devstudy.resume", importOptions = ImportOption.DoNotIncludeTests.class)
class PlatformBoundaryArchTest {

    @ArchTest
    static final ArchRule platform_infra_packages_must_not_depend_on_auth_profile_search = noClasses()
            .that()
            .resideInAnyPackage(
                    "net.devstudy.resume.shared.middleware..",
                    "net.devstudy.resume.shared.observability..",
                    "net.devstudy.resume.shared.validation..",
                    "net.devstudy.resume.shared.config..",
                    "net.devstudy.resume.shared.component..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "net.devstudy.resume.auth..",
                    "net.devstudy.resume.profile..",
                    "net.devstudy.resume.search..");
}
