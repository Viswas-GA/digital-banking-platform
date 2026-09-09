package com.banking.transaction.service;

import com.banking.transaction.client.AccountServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTransferProcessor {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTransferProcessor.class);

    private final ScheduledTransferService scheduledTransferService;

    public ScheduledTransferProcessor(ScheduledTransferService scheduledTransferService) {
        this.scheduledTransferService = scheduledTransferService;
    }

    @Scheduled(cron = "0 * * * * *")
    public void processDueTransfers() {
        log.debug("Checking for due scheduled transfers");
        scheduledTransferService.processDueTransfers();
    }
}
