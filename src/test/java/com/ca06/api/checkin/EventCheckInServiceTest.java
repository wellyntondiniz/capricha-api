package com.ca06.api.checkin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.ca06.api.auth.AuthenticatedUser;
import com.ca06.api.event.Event;
import com.ca06.api.event.EventRepository;
import com.ca06.api.exception.ApiException;
import com.ca06.api.time.AppTime;
import com.ca06.api.user.AppUser;
import com.ca06.api.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class EventCheckInServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventCheckInRepository checkInRepository;

    @Mock
    private AppTime appTime;

    private EventCheckInService service;
    private Event event;
    private AppUser user;
    private AuthenticatedUser authenticatedUser;
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 13, 19, 0);

    @BeforeEach
    void setUp() {
        service = new EventCheckInService(eventRepository, userRepository, checkInRepository, appTime);
        event = new Event(
                "Evento de teste",
                "Descrição",
                now.minusHours(1),
                now.plusHours(2),
                "Auditório",
                now.minusHours(2),
                now.plusHours(2));
        ReflectionTestUtils.setField(event, "id", 15L);
        ReflectionTestUtils.setField(event, "checkInToken", "token-seguro");
        user = new AppUser("Participante", "participante@test.local", "hash", "PARTICIPANT");
        ReflectionTestUtils.setField(user, "id", 42L);
        authenticatedUser = new AuthenticatedUser(42L, user.getName(), user.getRole());
        when(eventRepository.findById(15L)).thenReturn(Optional.of(event));
    }

    @Test
    void createsCheckInUsingAuthenticatedUserAndServerTime() {
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(appTime.now()).thenReturn(now);
        when(checkInRepository.existsByUserIdAndEventId(42L, 15L)).thenReturn(false);
        when(checkInRepository.saveAndFlush(any(EventCheckIn.class))).thenAnswer(invocation -> {
            EventCheckIn checkIn = invocation.getArgument(0);
            ReflectionTestUtils.setField(checkIn, "id", 182L);
            return checkIn;
        });

        CheckInResponse response = service.checkIn(
                15L,
                new CheckInRequest("token-seguro"),
                authenticatedUser);

        assertEquals(182L, response.id());
        assertEquals(15L, response.eventId());
        assertEquals(now, response.checkedInAt());
        verify(checkInRepository).saveAndFlush(any(EventCheckIn.class));
    }

    @Test
    void rejectsInvalidQrTokenBeforeSaving() {
        ApiException exception = assertThrows(ApiException.class, () -> service.checkIn(
                15L,
                new CheckInRequest("token-diferente"),
                authenticatedUser));

        assertEquals("INVALID_EVENT_TOKEN", exception.getCode());
        verify(checkInRepository, never()).saveAndFlush(any(EventCheckIn.class));
    }

    @Test
    void rejectsDuplicateCheckIn() {
        when(appTime.now()).thenReturn(now);
        when(checkInRepository.existsByUserIdAndEventId(42L, 15L)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> service.checkIn(
                15L,
                new CheckInRequest("token-seguro"),
                authenticatedUser));

        assertEquals("ALREADY_CHECKED_IN", exception.getCode());
        verify(checkInRepository, never()).saveAndFlush(any(EventCheckIn.class));
    }
}
