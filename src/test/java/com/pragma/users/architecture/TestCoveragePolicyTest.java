package com.pragma.users.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Structural test-coverage policy: guarantees that every business component ships its own unit-test
 * suite (by naming convention). This is NOT a line/branch coverage metric — that is JaCoCo's job.
 * ArchUnit answers "does a &lt;Name&gt;Test exist?"; JaCoCo answers "how much of it is exercised?".
 *
 * <p>Lives in its own class because it must import the test classes too, which the
 * {@code DoNotIncludeTests} option in {@link ArchitectureTest} deliberately excludes.</p>
 */
class TestCoveragePolicyTest {

    private static final String ROOT_PACKAGE = "com.pragma.users";

    @Test
    void everyUseCaseAndValidatorMustHaveAMatchingUnitTest() {
        // On the test classpath this picks up BOTH production and *Test classes.
        JavaClasses allClasses = new ClassFileImporter().importPackages(ROOT_PACKAGE);

        // Index of existing test class names, e.g. "UserUseCaseTest".
        Set<String> existingTestNames = allClasses.stream()
                .map(JavaClass::getSimpleName)
                .filter(name -> name.endsWith("Test"))
                .collect(Collectors.toSet());

        // Every use case and domain validator must have a <Name>Test — new business code
        // cannot slip in without its suite (TDD is mandatory per HU).
        classes()
                .that().resideInAnyPackage("..domain.usecase..", "..domain.validation..")
                .and().areNotInterfaces()
                .and().areNotNestedClasses()
                // Exclude the test classes themselves — they mirror the production package.
                .and().haveSimpleNameNotEndingWith("Test")
                // Exempt the archetype "Object*" scaffolding — it is placeholder code that each HU
                // replaces. Real business components (e.g. UserUseCase) are still enforced.
                .and().haveSimpleNameNotStartingWith("Object")
                .should(haveAMatchingTestClass(existingTestNames))
                .check(allClasses);
    }

    private static ArchCondition<JavaClass> haveAMatchingTestClass(Set<String> existingTestNames) {
        return new ArchCondition<>("have a matching <Name>Test unit test") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                String expectedTest = clazz.getSimpleName() + "Test";
                if (!existingTestNames.contains(expectedTest)) {
                    events.add(SimpleConditionEvent.violated(clazz,
                            clazz.getSimpleName() + " has no unit test (expected " + expectedTest + ")"));
                }
            }
        };
    }
}
