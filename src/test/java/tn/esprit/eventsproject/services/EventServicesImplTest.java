package tn.esprit.eventsproject.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.eventsproject.entities.Event;
import tn.esprit.eventsproject.entities.Logistics;
import tn.esprit.eventsproject.entities.Participant;
import tn.esprit.eventsproject.entities.Tache;
import tn.esprit.eventsproject.repositories.EventRepository;
import tn.esprit.eventsproject.repositories.LogisticsRepository;
import tn.esprit.eventsproject.repositories.ParticipantRepository;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServicesImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private LogisticsRepository logisticsRepository;

    @InjectMocks
    private EventServicesImpl eventServices;

    @Test
    void addParticipant_savesParticipant() {
        Participant participant = new Participant();
        when(participantRepository.save(participant)).thenReturn(participant);

        Participant result = eventServices.addParticipant(participant);

        verify(participantRepository).save(participant);
        assertSame(participant, result);
    }

    @Test
    void addAffectEvenParticipant_addsEventToParticipantWhenNoExistingEvents() {
        Participant participant = new Participant();
        participant.setIdPart(1);
        Event event = new Event();
        when(participantRepository.findById(1)).thenReturn(Optional.of(participant));
        when(eventRepository.save(event)).thenReturn(event);

        Event savedEvent = eventServices.addAffectEvenParticipant(event, 1);

        assertNotNull(participant.getEvents());
        assertTrue(participant.getEvents().contains(event));
        verify(eventRepository).save(event);
        assertSame(event, savedEvent);
    }

    @Test
    void addAffectEvenParticipant_addsEventForEachLinkedParticipant() {
        Participant persistedParticipant = new Participant();
        persistedParticipant.setIdPart(7);
        Event event = new Event();
        Participant linkedParticipant = new Participant();
        linkedParticipant.setIdPart(7);
        event.setParticipants(new HashSet<>(Collections.singletonList(linkedParticipant)));

        when(participantRepository.findById(7)).thenReturn(Optional.of(persistedParticipant));
        when(eventRepository.save(event)).thenReturn(event);

        Event savedEvent = eventServices.addAffectEvenParticipant(event);

        assertNotNull(persistedParticipant.getEvents());
        assertTrue(persistedParticipant.getEvents().contains(event));
        verify(eventRepository).save(event);
        assertSame(event, savedEvent);
    }

    @Test
    void addAffectLog_initializesLogisticSetWhenMissing() {
        Logistics logistics = new Logistics();
        Event event = new Event();
        when(eventRepository.findByDescription("Tech Day")).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);
        when(logisticsRepository.save(logistics)).thenReturn(logistics);

        Logistics result = eventServices.addAffectLog(logistics, "Tech Day");

        assertNotNull(event.getLogistics());
        assertTrue(event.getLogistics().contains(logistics));
        verify(eventRepository).save(event);
        verify(logisticsRepository).save(logistics);
        assertSame(logistics, result);
    }

    @Test
    void addAffectLog_addsLogisticToExistingSet() {
        Logistics logistics = new Logistics();
        Logistics existing = new Logistics();
        Event event = new Event();
        event.setLogistics(new HashSet<>(Collections.singleton(existing)));

        when(eventRepository.findByDescription("Tech Day")).thenReturn(event);
        when(logisticsRepository.save(logistics)).thenReturn(logistics);

        Logistics result = eventServices.addAffectLog(logistics, "Tech Day");

        assertEquals(2, event.getLogistics().size());
        assertTrue(event.getLogistics().contains(logistics));
        verify(eventRepository, never()).save(event);
        verify(logisticsRepository).save(logistics);
        assertSame(logistics, result);
    }

    @Test
    void getLogisticsDates_returnsNullWhenAnEventHasNoLogistics() {
        Event event = new Event();
        event.setLogistics(new HashSet<>());
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(5);
        when(eventRepository.findByDateDebutBetween(start, end)).thenReturn(Collections.singletonList(event));

        List<Logistics> result = eventServices.getLogisticsDates(start, end);

        assertNull(result);
    }

    @Test
    void getLogisticsDates_returnsOnlyReservedLogistics() {
        Logistics reserved = new Logistics();
        reserved.setReserve(true);
        Logistics notReserved = new Logistics();
        notReserved.setReserve(false);
        Event event = new Event();
        event.setLogistics(new HashSet<>(Arrays.asList(reserved, notReserved)));
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(5);
        when(eventRepository.findByDateDebutBetween(start, end)).thenReturn(Collections.singletonList(event));

        List<Logistics> result = eventServices.getLogisticsDates(start, end);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(reserved));
        assertFalse(result.contains(notReserved));
    }

    @Test
    void calculCout_updatesEventCostWithReservedLogistics() {
        Logistics reserved = new Logistics();
        reserved.setReserve(true);
        reserved.setPrixUnit(15f);
        reserved.setQuantite(3);
        Logistics notReserved = new Logistics();
        notReserved.setReserve(false);
        Event event = new Event();
        event.setLogistics(new HashSet<>(Arrays.asList(reserved, notReserved)));

        when(eventRepository.findByParticipants_NomAndParticipants_PrenomAndParticipants_Tache("Tounsi", "Ahmed", Tache.ORGANISATEUR))
                .thenReturn(Collections.singletonList(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        eventServices.calculCout();

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        Event saved = captor.getValue();
        assertEquals(45f, saved.getCout());
    }
}
