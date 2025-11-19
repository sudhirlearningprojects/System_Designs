package org.sudhir512kj.ticketbooking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sudhir512kj.ticketbooking.dto.FoodBeverageResponse;
import org.sudhir512kj.ticketbooking.repository.FoodBeverageRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FoodBeverageService {
    
    @Autowired
    private FoodBeverageRepository foodBeverageRepository;
    
    public List<FoodBeverageResponse> getVenueMenu(Long venueId) {
        return foodBeverageRepository.findByVenueIdAndIsAvailableTrue(venueId)
                .stream()
                .map(item -> {
                    FoodBeverageResponse response = new FoodBeverageResponse();
                    response.setId(item.getId());
                    response.setName(item.getName());
                    response.setPrice(item.getPrice());
                    return response;
                })
                .collect(Collectors.toList());
    }
}