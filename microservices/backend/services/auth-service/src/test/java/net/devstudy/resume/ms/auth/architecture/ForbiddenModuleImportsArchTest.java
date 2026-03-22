package net.devstudy.resume.ms.auth.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;

@AnalyzeClasses(packages = "net.devstudy.resume")
class ForbiddenModuleImportsArchTest {

    private static final List<String> FORBIDDEN_JAR_MARKERS = List.of(
            "/resume-web-",
            "/resume-search-",
            "/resume-auth-",
            "/resume-profile-",
            "/resume-notification-"
    );

    private static final DescribedPredicate<JavaClass> FORBIDDEN_EXTERNAL_MODULE_CLASS =
            new DescribedPredicate<>("class from forbidden module jars") {
                @Override
                public boolean test(JavaClass javaClass) {
                    return javaClass.getSource()
                            .map(source -> source.getUri().toString())
                            .filter(uri -> uri.startsWith("jar:file:"))
                            .map(uri -> FORBIDDEN_JAR_MARKERS.stream().anyMatch(uri::contains))
                            .orElse(false);
                }
            };

    @ArchTest
    static final ArchRule local_code_should_not_depend_on_forbidden_modules = noClasses()
            .that()
            .resideInAnyPackage(
                    "net.devstudy.resume.ms.auth..",
                    "net.devstudy.resume.auth..",
                    "net.devstudy.resume.web..",
                    "net.devstudy.resume.profile..",
                    "net.devstudy.resume.notification.."
            )
            .should()
            .dependOnClassesThat(FORBIDDEN_EXTERNAL_MODULE_CLASS);
}
