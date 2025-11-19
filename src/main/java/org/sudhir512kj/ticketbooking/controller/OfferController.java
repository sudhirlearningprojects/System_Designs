package org.sudhir512kj.ticketbooking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.ticketbooking.dto.*;
import org.sudhir512kj.ticketbooking.model.Offer;
import org.sudhir512kj.ticketbooking.service.OfferService;

import java.util.List;

@RestController
@RequestMapping("/api/offers")
public class OfferController {
    
    @Autowired
    private OfferService offerService;
    
    // Public Offers
    @GetMapping("/active")
    public ResponseEntity<List<OfferResponse>> getActiveOffers(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category) {
        List<OfferResponse> offers = offerService.getActiveOffers(city, category);
        return ResponseEntity.ok(offers);
    }
    
    @GetMapping("/featured")
    public ResponseEntity<List<OfferResponse>> getFeaturedOffers() {
        List<OfferResponse> offers = offerService.getFeaturedOffers();
        return ResponseEntity.ok(offers);
    }
    
    @PostMapping("/validate")
    public ResponseEntity<OfferValidationResponse> validateOffer(@RequestBody ValidateOfferRequest request) {
        OfferValidationResponse response = offerService.validateOffer(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/apply")
    public ResponseEntity<OfferApplicationResponse> applyOffer(@RequestBody ApplyOfferRequest request) {
        OfferApplicationResponse response = offerService.applyOffer(request);
        return ResponseEntity.ok(response);
    }
    
    // Admin Operations
    @GetMapping
    public ResponseEntity<Page<Offer>> getAllOffers(Pageable pageable) {
        Page<Offer> offers = offerService.getAllOffers(pageable);
        return ResponseEntity.ok(offers);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OfferDetailResponse> getOffer(@PathVariable Long id) {
        OfferDetailResponse offer = offerService.getOfferDetails(id);
        return ResponseEntity.ok(offer);
    }
    
    @PostMapping
    public ResponseEntity<Offer> createOffer(@RequestBody CreateOfferRequest request) {
        Offer offer = offerService.createOffer(request);
        return ResponseEntity.ok(offer);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Offer> updateOffer(@PathVariable Long id, @RequestBody UpdateOfferRequest request) {
        Offer offer = offerService.updateOffer(id, request);
        return ResponseEntity.ok(offer);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffer(@PathVariable Long id) {
        offerService.deleteOffer(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{id}/analytics")
    public ResponseEntity<OfferAnalyticsResponse> getOfferAnalytics(@PathVariable Long id) {
        OfferAnalyticsResponse analytics = offerService.getOfferAnalytics(id);
        return ResponseEntity.ok(analytics);
    }
}