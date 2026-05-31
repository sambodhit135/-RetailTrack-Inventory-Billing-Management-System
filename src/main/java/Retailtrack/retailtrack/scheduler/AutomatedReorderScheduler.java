package Retailtrack.retailtrack.scheduler;

import Retailtrack.retailtrack.service.ReorderEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background cron job scheduler to trigger automated reorder request drafts daily.
 */
@Slf4j
@Component
public class AutomatedReorderScheduler {

    private final ReorderEngineService reorderEngineService;

    public AutomatedReorderScheduler(ReorderEngineService reorderEngineService) {
        this.reorderEngineService = reorderEngineService;
    }

    /**
     * Automatically triggers the inventory reorder drafting daily at midnight.
     * For developer testing, a fixedRate of 60 seconds is provided.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    //@Scheduled(fixedRate = 60000)
    public void runAutomatedReorderTask() {
        log.info("Scheduler: Starting background automated reorder task checks.");
        try {
            reorderEngineService.triggerAutomatedReorderRequests();
            log.info("Scheduler: Background automated reorder task checks completed successfully.");
        } catch (Exception e) {
            log.error("Scheduler: Critical error executing automated reorder scheduler task", e);
        }
    }
}
