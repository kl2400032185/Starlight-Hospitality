package com.starlight.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ROOM-SERVICE")
public interface RoomClient {

    @GetMapping("/api/rooms/{id}")
    RoomResponse getRoomById(@PathVariable("id") Long id);
}