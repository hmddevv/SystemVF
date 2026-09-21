package com.hmdao.farm.land.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmdao.farm.land.application.dto.FarmView;
import com.hmdao.farm.land.application.port.in.FarmQueryUseCase;
import com.hmdao.farm.land.application.port.in.ManageFarmUseCase;
import com.hmdao.farm.shared.domain.ResourceConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Slice test tầng web cho Epic A: mã HTTP, validation và hình dạng response của nông trại. */
@WebMvcTest(FarmController.class)
class FarmControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ManageFarmUseCase manageFarm;

    @MockitoBean
    FarmQueryUseCase farmQuery;

    @Test
    void creatingAFarmReturns201WithLocationAndBody() throws Exception {
        when(manageFarm.create(any())).thenReturn(new FarmView(3L, "Nông trại Ea Kar", "Đắk Lắk", 0, 0));

        mvc.perform(post("/api/v1/farms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Nông trại Ea Kar", "location": "Đắk Lắk"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/farms/3"))
                .andExpect(jsonPath("$.name").value("Nông trại Ea Kar"))
                .andExpect(jsonPath("$.plotCount").value(0));
    }

    @Test
    void aBlankNameIsRejectedBeforeReachingTheUseCase() throws Exception {
        mvc.perform(post("/api/v1/farms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:farm:problem:validation"))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')]").exists());

        verifyNoInteractions(manageFarm);
    }

    @Test
    void theListCarriesPlotCountAndTotalAreaSoTheFrontendNeedsNoSecondCall() throws Exception {
        when(farmQuery.listMine()).thenReturn(List.of(
                new FarmView(1L, "Nông trại Ea Kar", "Đắk Lắk", 3, 45_000),
                new FarmView(2L, "Vườn nhà", null, 0, 0)));

        mvc.perform(get("/api/v1/farms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].plotCount").value(3))
                .andExpect(jsonPath("$[0].totalAreaM2").value(45_000.0));
    }

    @Test
    void br10_deletingAFarmThatStillHasPlotsIs409() throws Exception {
        doThrow(new ResourceConflictException("BR-10", "Nông trại còn 3 lô đất."))
                .when(manageFarm).delete(eq(1L));

        mvc.perform(delete("/api/v1/farms/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("BR-10"))
                .andExpect(jsonPath("$.detail").value("Nông trại còn 3 lô đất."));
    }

    @Test
    void deletingAnEmptyFarmReturns204WithNoBody() throws Exception {
        mvc.perform(delete("/api/v1/farms/1"))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());

        verify(manageFarm).delete(1L);
    }

    /**
     * Hai người cùng sửa một nông trại: người lưu sau không được phép ghi đè âm thầm lên thay
     * đổi của người lưu trước. {@code @Version} phát hiện, tầng web dịch thành 409 kèm lời
     * hướng dẫn tải lại — đây là đường duy nhất để kiểm chứng phần dịch đó.
     */
    @Test
    void aConcurrentUpdateBecomes409WithAnInstructionToReload() throws Exception {
        when(manageFarm.update(eq(1L), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException("Farm", 1L));

        mvc.perform(put("/api/v1/farms/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Nông trại Ea Kar"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:farm:problem:concurrent-update"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("tải lại")));
    }

    /** Ràng buộc database bị chạm do tranh chấp: vẫn là 409, không phải 500. */
    @Test
    void aDatabaseConstraintViolationBecomes409RatherThanAServerError() throws Exception {
        when(manageFarm.create(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        mvc.perform(post("/api/v1/farms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Nông trại Ea Kar"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:farm:problem:data-integrity"));
    }

    @Test
    void aNonNumericIdIsATypeMismatchAndSaysWhatShapeWasExpected() throws Exception {
        mvc.perform(get("/api/v1/farms/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Tham số không hợp lệ"))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("Cần một giá trị số")));
    }
}
