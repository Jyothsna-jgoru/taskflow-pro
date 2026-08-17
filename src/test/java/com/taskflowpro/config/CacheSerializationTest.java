package com.taskflowpro.config;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.taskflowpro.dto.DashboardDtos.DashboardResponse;
import com.taskflowpro.entity.TaskStatus;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

class CacheSerializationTest {
  @Test
  void dashboardRecordsRoundTripThroughRedisJsonSerializer() {
    var mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    var serializer =
        GenericJackson2JsonRedisSerializer.builder()
            .objectMapper(mapper)
            .defaultTyping(true)
            .build();
    var dashboard =
        new DashboardResponse(
            4,
            new EnumMap<>(Map.of(TaskStatus.TODO, 3L, TaskStatus.DONE, 1L)),
            1,
            2,
            25,
            List.of(),
            List.of(),
            List.of());

    Object restored = serializer.deserialize(serializer.serialize(dashboard));

    assertInstanceOf(DashboardResponse.class, restored);
    assertEquals(4, ((DashboardResponse) restored).totalTasks());
    Map<?, ?> restoredStatuses = ((DashboardResponse) restored).tasksByStatus();
    assertEquals(1L, ((Number) restoredStatuses.get(TaskStatus.DONE)).longValue());
  }
}
