package org.sudhir512kj.ticketbooking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.sudhir512kj.ticketbooking.dto.*;
import org.sudhir512kj.ticketbooking.model.Venue;
import org.sudhir512kj.ticketbooking.repository.VenueRepository;

import java.util.List;

@Service
public class VenueService {
    
    @Autowired
    private VenueRepository venueRepository;
    
    public Page<Venue> searchVenues(String city, String name, String type, Pageable pageable) {
        return venueRepository.searchVenues(city, name, type, pageable);
    }
    
    public VenueDetailResponse getVenueDetails(Long id) {
        return new VenueDetailResponse();
    }
    
    public List<Venue> getNearbyVenues(Double latitude, Double longitude, Double radiusKm) {
        return venueRepository.findNearbyVenues(latitude, longitude, radiusKm * 1000);
    }
    
    public Venue createVenue(CreateVenueRequest request) {
        Venue venue = new Venue();
        venue.setName(request.getName());
        venue.setCity(request.getCity());
        return venueRepository.save(venue);
    }
    
    public Venue updateVenue(Long id, UpdateVenueRequest request) {
        Venue venue = venueRepository.findById(id).orElseThrow();
        venue.setName(request.getName());
        return venueRepository.save(venue);
    }
    
    public void deleteVenue(Long id) {
        venueRepository.deleteById(id);
    }
}