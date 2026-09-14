package com.ca06.api.config;

import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.ca06.api.event.Event;
import com.ca06.api.event.EventRepository;
import com.ca06.api.time.AppTime;
import com.ca06.api.user.AppUser;
import com.ca06.api.user.UserRepository;

@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppTime appTime;

    public DemoDataSeeder(
            UserRepository userRepository,
            EventRepository eventRepository,
            PasswordEncoder passwordEncoder,
            AppTime appTime) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.passwordEncoder = passwordEncoder;
        this.appTime = appTime;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(new AppUser(
                    "Participante Demo",
                    "participante@capricha.local",
                    passwordEncoder.encode("demo123"),
                    "PARTICIPANT"));
            userRepository.save(new AppUser(
                    "Organizador Demo",
                    "organizador@capricha.local",
                    passwordEncoder.encode("demo123"),
                    "ORGANIZER"));
        }

        if (eventRepository.count() == 0) {
            LocalDateTime now = appTime.now();
            eventRepository.save(new Event(
                    "Semana Acadêmica de Engenharia",
                    "Evento acadêmico, comunidade e boas conversas.",
                    now.minusMinutes(30),
                    now.plusHours(3),
                    "Auditório Principal",
                    now.minusHours(2),
                    now.plusHours(2)));
        }
    }
}
