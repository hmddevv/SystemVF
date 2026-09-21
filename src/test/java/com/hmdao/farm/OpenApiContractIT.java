package com.hmdao.farm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmdao.farm.support.IntegrationTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Chốt hợp đồng API thành một file được version hoá.
 *
 * <p>Đổi tên một trường, bỏ một mã lỗi, đổi kiểu một tham số — tất cả đều là thay đổi phá vỡ
 * phía gọi, nhưng nếu không có gì ghi lại thì chúng lặng lẽ trôi qua code review. Test này ghi
 * đặc tả ra {@code docs/openapi.json}: thay đổi cố ý thì hiện thành diff trong commit và được
 * duyệt như mọi thay đổi khác; thay đổi vô tình thì build đỏ ngay.
 *
 * <p>Quan trọng nhất là trước M6 — frontend React sẽ sinh client từ chính file này.
 */
@IntegrationTest
class OpenApiContractIT {

    private static final Path CONTRACT = Path.of("docs", "openapi.json");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void theCommittedContractMatchesWhatTheApplicationActuallyServes() throws Exception {
        String served = canonicalJson(mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        if (!Files.exists(CONTRACT)) {
            write(served);
            throw new AssertionError(
                    "Chưa có docs/openapi.json — đã sinh ra từ ứng dụng đang chạy. Kiểm tra rồi commit file này.");
        }

        String committed = Files.readString(CONTRACT);
        if (!committed.equals(served)) {
            write(served);
            assertThat(served)
                    .as("""
                            Hợp đồng API đã thay đổi. docs/openapi.json vừa được ghi đè bằng đặc tả mới:
                            xem `git diff docs/openapi.json`. Đúng ý thì commit kèm thay đổi code;
                            không đúng ý thì đó là một thay đổi phá vỡ phía gọi bị lọt vào.""")
                    .isEqualTo(committed);
        }
    }

    /**
     * Sắp xếp khóa trước khi ghi. springdoc trả các mã lỗi theo thứ tự của HashMap nên hai lần
     * chạy liên tiếp cho ra hai file khác nhau ở cùng một nội dung — không chuẩn hóa thì diff
     * đầy nhiễu và test này đỏ ngẫu nhiên. Thứ tự phần tử trong mảng giữ nguyên, vì ở đó thứ
     * tự là một phần của ý nghĩa.
     */
    private String canonicalJson(String raw) {
        return json.writerWithDefaultPrettyPrinter().writeValueAsString(withSortedKeys(json.readTree(raw))) + "\n";
    }

    private JsonNode withSortedKeys(JsonNode node) {
        if (node.isObject()) {
            List<Map.Entry<String, JsonNode>> properties = new ArrayList<>(node.properties());
            properties.sort(Map.Entry.comparingByKey());
            ObjectNode sorted = json.createObjectNode();
            properties.forEach(property -> sorted.set(property.getKey(), withSortedKeys(property.getValue())));
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode sorted = json.createArrayNode();
            node.values().forEach(child -> sorted.add(withSortedKeys(child)));
            return sorted;
        }
        return node;
    }

    private static void write(String content) throws IOException {
        Files.createDirectories(CONTRACT.getParent());
        Files.writeString(CONTRACT, content);
    }
}
