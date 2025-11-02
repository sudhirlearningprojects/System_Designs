package org.sudhir512kj.parkinglot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sudhir512kj.parkinglot.model.Ticket;
import org.sudhir512kj.parkinglot.repository.TicketRepository;
import java.util.UUID;

@Service
public class TicketService {
    
    @Autowired
    private TicketRepository ticketRepository;
    
    public Ticket generateTicket(String licensePlate, String spotId) {
        String ticketId = UUID.randomUUID().toString();
        Ticket ticket = new Ticket(ticketId, licensePlate, spotId);
        return ticketRepository.save(ticket);
    }
    
    public Ticket getTicket(String ticketId) {
        return ticketRepository.findById(ticketId).orElse(null);
    }
    
    public void updateTicket(Ticket ticket) {
        ticketRepository.save(ticket);
    }
}