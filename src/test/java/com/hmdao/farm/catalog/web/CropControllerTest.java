package com.hmdao.farm.catalog.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmdao.farm.catalog.application.dto.CropView;
import com.hmdao.farm.catalog.application.port.in.CropQueryUseCase;
import com.hmdao.farm.catalog.application.port.in.ManageCropUseCase;
import com.hmdao.farm.shared.domain.BusinessRuleViolationException;
import com.hmdao.farm.shared.domain.ResourceConflictException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Slice test tầng web cho danh mục cây trồng — nơi quy tắc niên vụ (BR-05a) lộ ra ngoài API. */
@WebMvcTest(CropController.class)
class CropControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ManageCropUseCase manageCrop;

    @MockitoBean
    CropQueryUseCase cropQuery;

    @Test
    void addingAPerennialCropReturns201() throws Exception {
        when(manageCrop.create(any()))
                .thenReturn(new CropView(8L, "Sầu riêng", "Musang King", "Sầu riêng (Musang King)", true, 10));

        mvc.perform(post("/api/v1/crops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Sầu riêng", "variety": "Musang King", "perennial": true,
                                 "seasonStartMonth": 10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/crops/8"))
                .andExpect(jsonPath("$.displayName").value("Sầu riêng (Musang King)"))
                .andExpect(jsonPath("$.seasonStartMonth").value(10));
    }

    @Test
    void aMonthOutsideOneToTwelveNeverReachesTheUseCase() throws Exception {
        mvc.perform(post("/api/v1/crops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Sầu riêng", "perennial": true, "seasonStartMonth": 13}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'seasonStartMonth')].message")
                        .value(org.hamcrest.Matchers.hasItem("Tháng bắt đầu niên vụ từ 1 đến 12")));

        verifyNoInteractions(manageCrop);
    }

    @Test
    void br05a_aPerennialWithoutASeasonStartMonthIs422NotABadRequest() throws Exception {
        // Ràng buộc "lâu năm thì phải có tháng" là quy tắc nghiệp vụ của domain, không phải
        // ràng buộc cú pháp của một trường -> 422, kèm mã quy tắc để client biết hỏi ai.
        when(manageCrop.create(any())).thenThrow(new BusinessRuleViolationException(
                "BR-05a", "Cây lâu năm phải có tháng bắt đầu niên vụ."));

        mvc.perform(post("/api/v1/crops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Sầu riêng", "perennial": true}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.rule").value("BR-05a"));
    }

    @Test
    void aMalformedJsonBodySaysSoInsteadOfLeakingTheParserMessage() throws Exception {
        mvc.perform(post("/api/v1/crops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Sầu riêng\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Thân request không phải JSON hợp lệ."));
    }

    @Test
    void aWrongTypeInTheBodyNamesTheFieldAndTheExpectedShape() throws Exception {
        mvc.perform(post("/api/v1/crops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ngô", "perennial": false, "seasonStartMonth": "tháng mười"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("seasonStartMonth")));
    }

    @Test
    void theCatalogListsEveryCropIncludingAnnualsWithNoSeasonMonth() throws Exception {
        when(cropQuery.listAll()).thenReturn(List.of(
                new CropView(1L, "Cà phê", "Robusta", "Cà phê (Robusta)", true, 2),
                new CropView(7L, "Ngô", "LVN10", "Ngô (LVN10)", false, null)));

        mvc.perform(get("/api/v1/crops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].seasonStartMonth").doesNotExist());
    }

    @Test
    void br10_deletingACropThatIsInUseIs409() throws Exception {
        doThrow(new ResourceConflictException("BR-10", "Cây trồng đang được dùng bởi 4 lứa trồng."))
                .when(manageCrop).delete(eq(1L));

        mvc.perform(delete("/api/v1/crops/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("BR-10"));
    }
}
