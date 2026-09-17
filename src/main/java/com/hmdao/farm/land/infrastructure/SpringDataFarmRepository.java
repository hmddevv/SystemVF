package com.hmdao.farm.land.infrastructure;

import com.hmdao.farm.land.application.port.out.FarmRepository;
import com.hmdao.farm.land.domain.Farm;
import org.springframework.data.repository.Repository;

/**
 * Adapter persistence: Spring Data sinh implementation cho output port {@link FarmRepository}.
 * Kế thừa {@link Repository} (không phải JpaRepository) để không lộ thêm method nào ngoài port.
 */
interface SpringDataFarmRepository extends Repository<Farm, Long>, FarmRepository {
}
