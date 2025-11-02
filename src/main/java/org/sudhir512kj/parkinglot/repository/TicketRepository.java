package org.sudhir512kj.parkinglot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.parkinglot.model.Ticket;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {
}