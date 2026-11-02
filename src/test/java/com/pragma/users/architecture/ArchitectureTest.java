package com.pragma.users.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import jakarta.persistence.Entity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Enforces the hexagonal (ports & adapters) boundaries and naming conventions of this service.
 * Dependencies must point inward only: infrastructure -> application -> domain.
 * Production code only (tests are excluded from the analysis).
 */
@AnalyzeClasses(
        packages = "com.pragma.users",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // --- RULE 1: Hexagonal layering ---
    // The domain is the core; the application orchestrates it; infrastructure adapts the outside world.
    // Access flows inward only, so no inner layer may reach an outer one.
    @ArchTest
    static final ArchRule hexagonal_layers_are_respected = layeredArchitecture()
            .consideringAllDependencies()
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Infrastructure").definedBy("..infrastructure..")

            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure");

    // --- RULE 2: Domain purity ---
    // The domain must stay framework-free so the business logic is testable and portable.
    @ArchTest
    static final ArchRule domain_must_not_depend_on_frameworks =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "org.hibernate..",
                            "..infrastructure..",
                            "..application..")
                    .because("the domain is the pure core of the hexagon: no framework, no outer layer");

    // --- RULE 3: Application isolation ---
    // Orchestration may know the domain, never the concrete adapters.
    @ArchTest
    static final ArchRule application_must_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("the application layer depends on the domain only, never on infrastructure");

    // --- RULE 4: Controllers must not reach persistence directly ---
    // Web adapters go through the handler -> service port, never straight to the output adapters.
    @ArchTest
    static final ArchRule controllers_must_not_access_persistence_directly =
            noClasses()
                    .that().resideInAPackage("..infrastructure.input..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure.out..")
                    .because("controllers must pass through the application/domain layer, "
                            + "never query persistence directly");

    // --- RULE 5: @Service belongs only to handlers ---
    // Per our convention only *Handler carries @Service; everything else is wired in BeanConfiguration.
    @ArchTest
    static final ArchRule service_annotation_only_on_handlers =
            classes()
                    .that().areAnnotatedWith(Service.class)
                    .should().haveSimpleNameEndingWith("Handler")
                    .andShould().resideInAPackage("..application.handler..")
                    .because("only *Handler classes are Spring @Service beans");

    // --- RULE 6: No field injection ---
    // Constructor injection via @RequiredArgsConstructor keeps dependencies explicit and testable.
    @ArchTest
    static final ArchRule no_field_injection =
            fields()
                    .should().notBeAnnotatedWith(Autowired.class)
                    .because("use constructor injection, never field injection");

    // --- RULE 7: Naming conventions (class suffixes) ---
    @ArchTest
    static final ArchRule rest_controllers_are_named_and_placed_properly =
            classes()
                    .that().areAnnotatedWith(RestController.class)
                    .should().haveSimpleNameEndingWith("RestController")
                    .andShould().resideInAPackage("..infrastructure.input.rest..");

    @ArchTest
    static final ArchRule jpa_entities_are_named_and_placed_properly =
            classes()
                    .that().areAnnotatedWith(Entity.class)
                    .should().haveSimpleNameEndingWith("Entity")
                    .andShould().resideInAPackage("..infrastructure.out.jpa.entity..")
                    .because("persistence entities live in the output adapter, not in the domain model");

    @ArchTest
    static final ArchRule use_cases_are_named_properly =
            classes()
                    .that().resideInAPackage("..domain.usecase..")
                    .should().haveSimpleNameEndingWith("UseCase");

    @ArchTest
    static final ArchRule jpa_adapters_are_named_properly =
            classes()
                    .that().resideInAPackage("..infrastructure.out.jpa.adapter..")
                    .should().haveSimpleNameEndingWith("JpaAdapter");

    // --- RULE 8: Port and repository interfaces are I-prefixed ---
    // Driving (api) and driven (spi) ports, plus repositories, follow the I<Name> convention.
    @ArchTest
    static final ArchRule port_and_repository_interfaces_are_i_prefixed =
            classes()
                    .that().areInterfaces()
                    .and().resideInAnyPackage(
                            "..domain.api..",
                            "..domain.spi..",
                            "..infrastructure.out.jpa.repository..")
                    .should().haveSimpleNameStartingWith("I")
                    .because("port and repository interfaces follow the I<Name> convention");
}
