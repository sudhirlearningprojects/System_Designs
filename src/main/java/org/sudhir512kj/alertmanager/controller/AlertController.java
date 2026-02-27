package org.sudhir512kj.alertmanager.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.alertmanager.dto.TicketEventRequest;
import org.sudhir512kj.alertmanager.service.AlertService;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {
    private final AlertService alertService;

    @PostMapping("/webhook")
    public ResponseEntity<String> receiveTicketEvent(@RequestBody TicketEventRequest request) {
        alertService.processTicketEvent(request);
        return ResponseEntity.ok("Event processed");
    }
}
