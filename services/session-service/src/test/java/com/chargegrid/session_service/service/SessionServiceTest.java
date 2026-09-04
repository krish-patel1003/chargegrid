package com.chargegrid.session_service.service;

import com.chargegrid.session_service.domain.*; import com.chargegrid.session_service.lock.InMemoryReservationLock; import com.chargegrid.session_service.repository.*; import org.junit.jupiter.api.*; import org.mockito.*; import org.springframework.amqp.core.AmqpTemplate;
import java.math.BigDecimal; import java.time.*; import java.util.*; import static org.junit.jupiter.api.Assertions.*; import static org.mockito.ArgumentMatchers.*; import static org.mockito.Mockito.*;

class SessionServiceTest {
 @Mock ReservationRepository reservations; @Mock ChargingSessionRepository sessions; @Mock MeterReadingRepository readings; @Mock AmqpTemplate events; SessionService service; Clock clock;
 @BeforeEach void setup(){MockitoAnnotations.openMocks(this);clock=Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"),ZoneOffset.UTC);service=new SessionService(reservations,sessions,readings,new InMemoryReservationLock(),events,clock);}
 @Test void expiresAtTenMinutes(){Reservation r=new Reservation("u","st","c",Instant.parse("2025-12-31T23:50:00Z"),Instant.parse("2026-01-01T00:00:00Z")); when(reservations.findByIdAndOwnerId(r.getId(),"u")).thenReturn(Optional.of(r)); assertSame(r,service.reservation(r.getId(),"u")); assertEquals(Reservation.Status.EXPIRED,r.getStatus()); verify(reservations).save(r);}
 @Test void isolatesOwners(){UUID id=UUID.randomUUID();when(reservations.findByIdAndOwnerId(id,"other")).thenReturn(Optional.empty());assertThrows(SessionService.NotFoundException.class,()->service.reservation(id,"other"));}
 @Test void accumulatesEnergy(){Reservation r=new Reservation("u","st","c",clock.instant(),clock.instant().plusSeconds(600));ChargingSession s=new ChargingSession(r,clock.instant());when(sessions.findByIdAndOwnerId(s.getId(),"u")).thenReturn(Optional.of(s));when(sessions.save(any())).thenAnswer(i->i.getArgument(0));assertEquals(new BigDecimal("3.5"),service.meter(s.getId(),"u",new BigDecimal("3.5")).getEnergyKwh());assertEquals(new BigDecimal("4.5"),service.meter(s.getId(),"u",new BigDecimal("1.0")).getEnergyKwh());}
 @Test void completionIsIdempotent(){Reservation r=new Reservation("u","st","c",clock.instant(),clock.instant().plusSeconds(600));ChargingSession s=new ChargingSession(r,clock.instant());when(sessions.findByIdAndOwnerId(s.getId(),"u")).thenReturn(Optional.of(s));when(sessions.save(any())).thenAnswer(i->i.getArgument(0));service.complete(s.getId(),"u");service.complete(s.getId(),"u");verify(events,times(1)).convertAndSend(any());}
}
