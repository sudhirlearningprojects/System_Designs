package org.sudhir512kj.ticketbooking.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.sudhir512kj.ticketbooking.dto.ReviewResponse;

@Service
public class ReviewService {
    
    public Page<ReviewResponse> getEventReviews(Long eventId, Pageable pageable) {
        return Page.empty();
    }
}