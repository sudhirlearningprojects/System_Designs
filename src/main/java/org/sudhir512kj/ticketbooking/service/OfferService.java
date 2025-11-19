package org.sudhir512kj.ticketbooking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.sudhir512kj.ticketbooking.dto.*;
import org.sudhir512kj.ticketbooking.model.Offer;
import org.sudhir512kj.ticketbooking.repository.OfferRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OfferService {
    
    @Autowired
    private OfferRepository offerRepository;
    
    public List<OfferResponse> getActiveOffers(String city, String category) {
        List<Offer> offers = offerRepository.findActiveOffers(LocalDateTime.now());
        return offers.stream().map(this::convertToResponse).collect(Collectors.toList());
    }
    
    public List<OfferResponse> getFeaturedOffers() {
        List<Offer> offers = offerRepository.findActiveOffers(LocalDateTime.now());
        return offers.stream().limit(5).map(this::convertToResponse).collect(Collectors.toList());
    }
    
    public OfferValidationResponse validateOffer(ValidateOfferRequest request) {
        OfferValidationResponse response = new OfferValidationResponse();
        response.setIsValid(true);
        response.setMessage("Offer is valid");
        return response;
    }
    
    public OfferApplicationResponse applyOffer(ApplyOfferRequest request) {
        OfferApplicationResponse response = new OfferApplicationResponse();
        return response;
    }
    
    public Page<Offer> getAllOffers(Pageable pageable) {
        return offerRepository.findAll(pageable);
    }
    
    public OfferDetailResponse getOfferDetails(Long id) {
        return new OfferDetailResponse();
    }
    
    public Offer createOffer(CreateOfferRequest request) {
        Offer offer = new Offer();
        offer.setTitle(request.getTitle());
        offer.setOfferCode(request.getOfferCode());
        return offerRepository.save(offer);
    }
    
    public Offer updateOffer(Long id, UpdateOfferRequest request) {
        Offer offer = offerRepository.findById(id).orElseThrow();
        offer.setTitle(request.getTitle());
        return offerRepository.save(offer);
    }
    
    public void deleteOffer(Long id) {
        offerRepository.deleteById(id);
    }
    
    public OfferAnalyticsResponse getOfferAnalytics(Long id) {
        return new OfferAnalyticsResponse();
    }
    
    private OfferResponse convertToResponse(Offer offer) {
        OfferResponse response = new OfferResponse();
        response.setId(offer.getId());
        response.setTitle(offer.getTitle());
        response.setOfferCode(offer.getOfferCode());
        response.setDiscountType(offer.getDiscountType());
        response.setDiscountValue(offer.getDiscountValue());
        return response;
    }
}