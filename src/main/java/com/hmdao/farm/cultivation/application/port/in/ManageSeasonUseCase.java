package com.hmdao.farm.cultivation.application.port.in;

public interface ManageSeasonUseCase {

    /** Chỉ xóa được niên vụ rỗng — còn hoạt động hoặc thu hoạch thì trả 409 (BR-10). */
    void delete(Long seasonId);
}
