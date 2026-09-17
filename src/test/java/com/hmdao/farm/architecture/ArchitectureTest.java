package com.hmdao.farm.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Biến quy tắc phụ thuộc trong docs/architecture.md thành test: vi phạm thì build thất bại.
 */
@AnalyzeClasses(packages = "com.hmdao.farm", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** Domain là lõi: chỉ được biết đặc tả jakarta.persistence, không biết Spring, Hibernate hay tầng khác. */
    @ArchTest
    static final ArchRule domainDependsOnNothingOutward = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "org.hibernate..",
                    "..application..", "..infrastructure..", "..web..");

    /** Application không biết HTTP hay cơ chế lưu trữ cụ thể — chỉ biết port. */
    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infrastructure..", "..web..", "org.springframework.web..", "org.springframework.data..",
                    "jakarta.servlet..");

    /** Controller chỉ gọi vào lõi qua input port, không chạm service implementation hay repository. */
    @ArchTest
    static final ArchRule webUsesOnlyInputPorts = noClasses()
            .that().resideInAPackage("..web..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application.service..", "..application.port.out..", "..infrastructure..");

    /** Service implementation ẩn sau input port: không public để không ai phụ thuộc trực tiếp. */
    @ArchTest
    static final ArchRule servicesAreHiddenBehindPorts = classes()
            .that().resideInAPackage("..application.service..")
            .should().notBePublic();

    @ArchTest
    static final ArchRule adaptersAreHiddenBehindPorts = classes()
            .that().resideInAPackage("..infrastructure..")
            .should().notBePublic();

    /** Không có chu trình phụ thuộc giữa các module nghiệp vụ (Acyclic Dependencies Principle). */
    @ArchTest
    static final ArchRule modulesAreFreeOfCycles = slices()
            .matching("com.hmdao.farm.(*)..")
            .should().beFreeOfCycles();
}
