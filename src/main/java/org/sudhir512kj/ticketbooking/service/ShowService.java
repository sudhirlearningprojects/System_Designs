package org.sudhir512kj.ticketbooking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sudhir512kj.ticketbooking.dto.*;
import org.sudhir512kj.ticketbooking.model.*;
import org.sudhir512kj.ticketbooking.repository.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShowService {
    
    @Autowired
    private ShowRepository showRepository;
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private VenueRepository venueRepository;
    
    @Autowired
    private ShowSeatRepository showSeatRepository;
    
    public List<ShowResponse> getEventShows(Long eventId, LocalDate date, String city) {
        List<Show> shows;
        if (date != null && city != null) {
            shows = showRepository.findByEventIdAndDateAndCity(eventId, date, city);
        } else if (date != null) {
            shows = showRepository.findByEventIdAndDate(eventId, date);
        } else if (city != null) {
            shows = showRepository.findByEventIdAndCity(eventId, city);
        } else {
            shows = showRepository.findByEventId(eventId);
        }
        
        return shows.stream()
                .filter(show -> show.getIsActive())
                .map(this::convertToShowResponse)
                .collect(Collectors.toList());
    }
    
    public ShowDetailResponse getShowDetails(Long showId) {
        Show show = showRepository.findById(showId).orElse(null);
        if (show == null) return null;
        
        ShowDetailResponse response = new ShowDetailResponse();
        response.setId(show.getId());
        response.setShowDate(show.getShowDate());
        response.setEventName(show.getEvent().getName());
        response.setVenueName(show.getVenue().getName());
        return response;
    }
    
    public ShowResponse createShow(CreateShowRequest request) {
        Event event = eventRepository.findById(request.getEventId()).orElseThrow();
        Venue venue = venueRepository.findById(request.getVenueId()).orElseThrow();
        
        Show show = new Show(event, venue, request.getShowDate());
        show = showRepository.save(show);
        
        return convertToShowResponse(show);
    }
    
    public ShowResponse updateShow(Long id, UpdateShowRequest request) {
        Show show = showRepository.findById(id).orElseThrow();
        show.setShowDate(request.getShowDate());
        show = showRepository.save(show);
        
        return convertToShowResponse(show);
    }
    
    public void deleteShow(Long id) {
        showRepository.deleteById(id);
    }
    
    private ShowResponse convertToShowResponse(Show show) {
        ShowResponse response = new ShowResponse();
        response.setId(show.getId());
        response.setShowDate(show.getShowDate());
        response.setEventName(show.getEvent().getName());
        response.setVenueName(show.getVenue().getName());
        
        // Calculate available seats
        long availableSeats = showSeatRepository.countByShowIdAndStatus(show.getId(), SeatStatus.AVAILABLE);
        response.setAvailableSeats((int) availableSeats);
        
        return response;
    }
}