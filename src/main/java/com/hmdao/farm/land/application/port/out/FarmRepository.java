package com.hmdao.farm.land.application.port.out;

import com.hmdao.farm.land.domain.Farm;
import java.util.List;
import java.util.Optional;

/**
 * Output port lưu trữ nông trại. Mọi truy vấn đọc đều lọc theo chủ sở hữu (BR-11).
 */
public interface FarmRepository {

    Farm save(Farm farm);

    Optional<Farm> findByIdAndOwnerId(Long id, Long ownerId);

    List<Farm> findAllByOwnerIdOrderByNameAsc(Long ownerId);

    void delete(Farm farm);
}
