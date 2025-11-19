package org.sudhir512kj.ticketbooking.dto;

import org.sudhir512kj.ticketbooking.model.ShowSeat;
import java.util.List;

public class SeatLayoutResponse {
    private Long showId;
    private String venueName;
    private Integer totalSeats;
    private Integer availableSeats;
    private List<ShowSeat> seats;
    
    public SeatLayoutResponse() {}
    
    public Long getShowId() { return showId; }
    public void setShowId(Long showId) { this.showId = showId; }
    
    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }
    
    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
    
    public Integer getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }
    
    public List<ShowSeat> getSeats() { return seats; }
    public void setSeats(List<ShowSeat> seats) { this.seats = seats; }
}