package com.hmdao.farm.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.hmdao.farm.shared.domain.BaseEntity;
import jakarta.persistence.Entity;

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

    /**
     * Cạnh giữa các module là quyết định kiến trúc, không phải chuyện tự phát. {@code reminder}
     * và {@code analytics} được đọc {@code cultivation.domain} (loại việc, trạng thái lứa trồng)
     * vì đó là ngôn ngữ nghiệp vụ chung; chiều ngược lại thì không. Luật beFreeOfCycles ở trên
     * chỉ bắt được chu trình sau khi nó đã hình thành — luật này chặn ngay cạnh đầu tiên.
     */
    @ArchTest
    static final ArchRule cultivationNeverDependsOnItsReaders = noClasses()
            .that().resideInAPackage("com.hmdao.farm.cultivation..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.hmdao.farm.analytics..", "com.hmdao.farm.reminder..");

    /**
     * Dependency injection qua constructor, không qua field. Field injection giấu phụ thuộc đi:
     * không nhìn constructor là không biết một lớp cần gì, và không tạo được nó trong unit test
     * nếu thiếu cả một container.
     */
    @ArchTest
    static final ArchRule noFieldInjection = noFields()
            .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired");

    /**
     * Mọi entity phải kế thừa {@link BaseEntity} — nơi duy nhất khai báo {@code @Version} cùng
     * dấu thời gian. Thiếu nó thì hai người cùng sửa một bản ghi, người lưu sau ghi đè âm thầm
     * lên thay đổi của người lưu trước mà không ai biết.
     */
    @ArchTest
    static final ArchRule everyEntityCarriesAVersion = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().beAssignableTo(BaseEntity.class);

    /** Nhật ký chạy qua logger, không qua System.out — production không ai đọc stdout. */
    @ArchTest
    static final ArchRule noConsolePrinting = noClasses()
            .should().callMethod(System.class, "currentTimeMillis")
            .orShould().accessField(System.class, "out")
            .orShould().accessField(System.class, "err")
            .because("dùng Logger, và dùng Clock để lấy thời gian (múi giờ nghiệp vụ là Asia/Ho_Chi_Minh)");

    /**
     * "Hôm nay" của nghiệp vụ luôn đến từ {@code Clock}: chỉ như vậy test mới điều khiển được
     * thời gian, và múi giờ mới chắc chắn là Asia/Ho_Chi_Minh chứ không phải múi giờ của máy chủ.
     *
     * <p>Dấu thời gian kỹ thuật trong {@link BaseEntity} ({@code Instant.now()}) không thuộc diện
     * này: nó ghi "bản ghi được lưu lúc nào", không tham gia vào quyết định nghiệp vụ nào.
     */
    @ArchTest
    static final ArchRule businessTimeAlwaysComesFromTheClock = noClasses()
            .should().callMethod(java.time.LocalDate.class, "now")
            .orShould().callMethod(java.time.LocalDateTime.class, "now");

    /** Kiểu ngày tháng cũ không còn chỗ trong code mới: không múi giờ, không bất biến. */
    @ArchTest
    static final ArchRule noLegacyDateTypes = noClasses()
            .should().dependOnClassesThat().haveFullyQualifiedName("java.util.Date")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("java.util.Calendar")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("java.sql.Date");

    /** Tiền là BigDecimal. double làm tròn nhị phân, cộng dồn chi phí cả vụ sẽ lệch. */
    @ArchTest
    static final ArchRule moneyIsNeverADouble = noFields()
            .that().haveNameMatching(".*([Cc]ost|[Rr]evenue|[Pp]rofit|[Pp]rice|[Aa]mount).*")
            .should().haveRawType(double.class)
            .orShould().haveRawType(Double.class)
            .because("tiền dùng BigDecimal; double chỉ dành cho đại lượng vật lý như diện tích, sản lượng");
}
