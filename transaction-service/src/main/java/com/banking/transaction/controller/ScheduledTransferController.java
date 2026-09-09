package com.banking.transaction.controller;

import com.banking.transaction.dto.ScheduleTransferRequest;
import com.banking.transaction.dto.ScheduledTransferResponse;
import com.banking.transaction.service.ScheduledTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scheduled-transfers")
@Tag(name = "Scheduled Transfers", description = "Schedule future money transfers")
public class ScheduledTransferController {

    private final ScheduledTransferService scheduledTransferService;

    public ScheduledTransferController(ScheduledTransferService scheduledTransferService) {
        this.scheduledTransferService = scheduledTransferService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Schedule a transfer", description = "Schedule a transfer for a future date/time")
    public ScheduledTransferResponse schedule(@Valid @RequestBody ScheduleTransferRequest request) {
        return scheduledTransferService.schedule(request);
    }

    @GetMapping
    @Operation(summary = "List scheduled transfers")
    public List<ScheduledTransferResponse> listMine() {
        return scheduledTransferService.listMine();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel scheduled transfer", description = "Cancel a pending scheduled transfer")
    public ScheduledTransferResponse cancel(@PathVariable UUID id) {
        return scheduledTransferService.cancel(id);
    }
}
