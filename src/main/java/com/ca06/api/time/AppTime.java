package com.ca06.api.time;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class AppTime {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    public LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }
}
