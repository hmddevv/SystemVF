package com.hmdao.farm.reminder.application.port.out;

import com.hmdao.farm.reminder.application.dto.LastActivityDate;
import com.hmdao.farm.reminder.application.dto.LastHarvestDate;
import com.hmdao.farm.reminder.application.dto.PlantingSnapshot;
import java.util.Collection;
import java.util.List;

/**
 * Ba method này là toàn bộ số truy vấn của một lần lấy danh sách nhắc việc, bất kể nông trại
 * có bao nhiêu lứa trồng và bao nhiêu luật đang bật.
 */
public interface CareContextQueryPort {

    /** Chỉ lứa đang canh tác; lứa đã cưa bỏ không cần nhắc gì nữa (BR-18). */
    List<PlantingSnapshot> findActivePlantings(Long ownerId, Long farmId);

    List<LastActivityDate> lastActivityDates(Collection<Long> plantingIds);

    List<LastHarvestDate> lastHarvestDates(Collection<Long> plantingIds);

    /**
     * Nông trại được chỉ đích danh có thuộc người gọi không (BR-11). Không kiểm thì lọc theo
     * nông trại của người khác trả về danh sách rỗng — không phân biệt được với "không có
     * việc gì tới hạn".
     */
    boolean farmBelongsToOwner(Long ownerId, Long farmId);
}
