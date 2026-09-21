package com.hmdao.farm.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Nối tài liệu với test: mọi quy tắc nghiệp vụ được ghi trong {@code docs/architecture.md} phải
 * xuất hiện trong ít nhất một test, và ngược lại không test nào được viện dẫn một mã quy tắc
 * không tồn tại trong tài liệu.
 *
 * <p>Vì sao đáng có: bảng quy tắc là thứ chủ dự án đọc để đối chiếu với nghiệp vụ, còn test là
 * thứ chứng minh phần mềm làm đúng như vậy. Hai bên trôi khỏi nhau một cách âm thầm — thêm
 * một quy tắc mới vào tài liệu rồi quên viết test, hay xóa một test mà tài liệu vẫn hứa. Test
 * này biến sự trôi đó thành build đỏ.
 *
 * <p>Nó chỉ kiểm tra sự <b>có mặt</b> của mã quy tắc, không kiểm tra test ấy đúng hay không —
 * đó là việc của chính test kia. Đây là lưới an toàn, không phải thẩm phán.
 */
class BusinessRuleCoverageTest {

    private static final Path ARCHITECTURE = Path.of("docs", "architecture.md");
    private static final Path TESTS = Path.of("src", "test", "java");
    /**
     * Bắt cả hai cách viết: "BR-05a" trong tài liệu và thông điệp lỗi, và "br05a_" trong tên
     * test — tên method Java không chứa dấu gạch ngang được.
     */
    private static final Pattern RULE_CODE = Pattern.compile("(?i)\\b(BR|CARE)[-_]?(\\d{1,2}[a-z]?)");

    @Test
    void everyDocumentedRuleIsExercisedBySomeTest() {
        Set<String> documented = codesIn(read(ARCHITECTURE));
        Set<String> tested = codesIn(allTestSources());

        assertThat(documented)
                .as("Tài liệu phải có quy tắc để so — đọc được %s chứ?", ARCHITECTURE)
                .isNotEmpty();
        assertThat(tested)
                .as("""
                        Có quy tắc nghiệp vụ trong docs/architecture.md chưa được test nào nhắc tới.
                        Hoặc viết test cho nó, hoặc bỏ nó khỏi tài liệu nếu quy tắc đã không còn.""")
                .containsAll(documented);
    }

    @Test
    void noTestInventsARuleCodeThatTheDocumentationDoesNotDefine() {
        Set<String> documented = codesIn(read(ARCHITECTURE));

        assertThat(codesIn(allTestSources()))
                .as("""
                        Test đang viện dẫn một mã quy tắc không có trong docs/architecture.md.
                        Mã quy tắc là ngôn ngữ chung giữa tài liệu, API và test — đặt thêm mã mới
                        thì phải mô tả nó trong bảng quy tắc.""")
                .isSubsetOf(documented);
    }

    private static Set<String> codesIn(String text) {
        Matcher matcher = RULE_CODE.matcher(text);
        Set<String> codes = new TreeSet<>();
        while (matcher.find()) {
            codes.add("%s-%s".formatted(
                    matcher.group(1).toUpperCase(Locale.ROOT), matcher.group(2).toLowerCase(Locale.ROOT)));
        }
        return codes;
    }

    /** Gộp toàn bộ mã nguồn test thành một chuỗi: chỉ cần biết mã nào có mặt, không cần biết ở đâu. */
    private static String allTestSources() {
        try (Stream<Path> files = Files.walk(TESTS)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            return javaFiles.stream().map(BusinessRuleCoverageTest::read).collect(Collectors.joining("\n"));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
