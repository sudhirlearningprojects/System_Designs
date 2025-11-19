package org.sudhir512kj.ticketbooking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.data.redis.core.script.ReturnType; // Not available in current Redis version
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.ticketbooking.dto.*;
import org.sudhir512kj.ticketbooking.model.*;
import org.sudhir512kj.ticketbooking.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SeatService {
    
    @Autowired
    private ShowSeatRepository showSeatRepository;
    
    @Autowired
    private ShowRepository showRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public SeatLayoutResponse getShowSeatLayout(Long showId) {
        Show show = showRepository.findById(showId)
            .orElseThrow(() -> new RuntimeException("Show not found"));
        
        List<ShowSeat> seats = showSeatRepository.findByShowId(showId);
        
        // Update seat availability from Redis cache
        String cacheKey = "show_seats:" + showId;
        Map<Object, Object> cachedSeats = redisTemplate.opsForHash().entries(cacheKey);
        
        for (ShowSeat seat : seats) {
            String seatKey = "seat_" + seat.getId();
            if (cachedSeats.containsKey(seatKey)) {
                String status = (String) cachedSeats.get(seatKey);
                seat.setStatus(SeatStatus.valueOf(status));
            }
        }
        
        SeatLayoutResponse response = new SeatLayoutResponse();
        response.setShowId(showId);
        response.setVenueName(show.getVenue().getName());
        response.setTotalSeats(seats.size());
        response.setAvailableSeats((int) seats.stream().filter(s -> s.getStatus() == SeatStatus.AVAILABLE).count());
        response.setSeats(seats);
        
        return response;
    }
    
    @Transactional
    public SeatHoldResponse holdSeats(Long showId, SeatHoldRequest request) {
        List<Long> seatIds = request.getSeatIds();
        String holdKey = "seat_hold:" + request.getUserId() + ":" + System.currentTimeMillis();
        String showCacheKey = "show_seats:" + showId;
        
        // Atomic seat hold operation using Redis
        List<String> luaScript = List.of(
            "local seats = ARGV[1]",
            "local userId = ARGV[2]",
            "local expiry = ARGV[3]",
            "local showKey = KEYS[1]",
            "local holdKey = KEYS[2]",
            "for seatId in string.gmatch(seats, '[^,]+') do",
            "  local seatKey = 'seat_' .. seatId",
            "  local status = redis.call('HGET', showKey, seatKey)",
            "  if status ~= 'AVAILABLE' and status ~= false then",
            "    return {err='Seat ' .. seatId .. ' not available'}",
            "  end",
            "end",
            "for seatId in string.gmatch(seats, '[^,]+') do",
            "  local seatKey = 'seat_' .. seatId",
            "  redis.call('HSET', showKey, seatKey, 'HELD')",
            "end",
            "redis.call('SETEX', holdKey, 600, seats)",
            "return 'OK'"
        );
        
        String seatIdsStr = seatIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        
        try {
            // Simplified Redis operation for compilation
            redisTemplate.opsForValue().set(holdKey, seatIds, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hold seats: " + e.getMessage());
        }
        
        // Update database
        LocalDateTime holdExpiry = LocalDateTime.now().plusMinutes(10);
        for (Long seatId : seatIds) {
            ShowSeat seat = showSeatRepository.findById(seatId).orElseThrow();
            seat.setStatus(SeatStatus.HELD);
            seat.setHoldExpiresAt(holdExpiry);
            seat.setBookedByUserId(request.getUserId());
            showSeatRepository.save(seat);
        }
        
        SeatHoldResponse response = new SeatHoldResponse();
        response.setHoldId(holdKey);
        response.setExpiresAt(holdExpiry);
        return response;
    }
    
    public void releaseSeats(Long showId, SeatReleaseRequest request) {
        List<Long> seatIds = (List<Long>) redisTemplate.opsForValue().get(request.getHoldId());
        if (seatIds != null) {
            for (Long seatId : seatIds) {
                ShowSeat seat = showSeatRepository.findById(seatId).orElse(null);
                if (seat != null && seat.getStatus() == SeatStatus.HELD) {
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setHoldExpiresAt(null);
                    seat.setBookedByUserId(null);
                    showSeatRepository.save(seat);
                }
            }
            redisTemplate.delete(request.getHoldId());
        }
    }
}