package org.sudhir512kj.jobscheduler.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CronService {
    
    // Basic cron validation pattern (simplified)
    private static final Pattern CRON_PATTERN = Pattern.compile(
        "^\\s*($|#|\\w+\\s*=|(\\?|\\*|(?:[0-5]?\\d)(?:(?:-|\\/|\\,)(?:[0-5]?\\d))?(?:,(?:[0-5]?\\d)(?:(?:-|\\/|\\,)(?:[0-5]?\\d))?)*)\\s+(\\?|\\*|(?:[0-5]?\\d)(?:(?:-|\\/|\\,)(?:[0-5]?\\d))?(?:,(?:[0-5]?\\d)(?:(?:-|\\/|\\,)(?:[0-5]?\\d))?)*)\\s+(\\?|\\*|(?:[01]?\\d|2[0-3])(?:(?:-|\\/|\\,)(?:[01]?\\d|2[0-3]))?(?:,(?:[01]?\\d|2[0-3])(?:(?:-|\\/|\\,)(?:[01]?\\d|2[0-3]))?)*)\\s+(\\?|\\*|(?:0?[1-9]|[12]\\d|3[01])(?:(?:-|\\/|\\,)(?:0?[1-9]|[12]\\d|3[01]))?(?:,(?:0?[1-9]|[12]\\d|3[01])(?:(?:-|\\/|\\,)(?:0?[1-9]|[12]\\d|3[01]))?)*)\\s+(\\?|\\*|(?:[1-9]|1[012])(?:(?:-|\\/|\\,)(?:[1-9]|1[012]))?(?:L|W)?(?:,(?:[1-9]|1[012])(?:(?:-|\\/|\\,)(?:[1-9]|1[012]))?(?:L|W)?)*|\\?|\\*|(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)(?:(?:-)(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))?(?:,(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)(?:(?:-)(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))?)*)\\s+(\\?|\\*|(?:[0-6])(?:(?:-|\\/|\\,|#)(?:[0-6]))?(?:L)?(?:,(?:[0-6])(?:(?:-|\\/|\\,|#)(?:[0-6]))?(?:L)?)*|\\?|\\*|(?:MON|TUE|WED|THU|FRI|SAT|SUN)(?:(?:-)(?:MON|TUE|WED|THU|FRI|SAT|SUN))?(?:,(?:MON|TUE|WED|THU|FRI|SAT|SUN)(?:(?:-)(?:MON|TUE|WED|THU|FRI|SAT|SUN))?)*)(|\\s)+(\\?|\\*|(?:|\\d{4})(?:(?:-|\\/|\\,)(?:|\\d{4}))?(?:,(?:|\\d{4})(?:(?:-|\\/|\\,)(?:|\\d{4}))?)*))$"
    );
    
    public boolean isValidCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Basic validation - in production, use a proper cron library like Quartz
            String[] parts = cronExpression.trim().split("\\s+");
            return parts.length >= 5 && parts.length <= 7;
        } catch (Exception e) {
            log.error("Invalid cron expression: {}", cronExpression, e);
            return false;
        }
    }
    
    public LocalDateTime getNextExecution(String cronExpression, LocalDateTime from) {
        if (!isValidCronExpression(cronExpression)) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression);
        }
        
        try {
            // Simplified cron parsing - in production, use Quartz CronExpression
            return parseAndCalculateNext(cronExpression, from);
        } catch (Exception e) {
            log.error("Error calculating next execution for cron: {}", cronExpression, e);
            throw new RuntimeException("Failed to calculate next execution time", e);
        }
    }
    
    private LocalDateTime parseAndCalculateNext(String cronExpression, LocalDateTime from) {
        // Simplified implementation - in production, use proper cron library
        String[] parts = cronExpression.trim().split("\\s+");
        
        // Handle common patterns
        if (cronExpression.equals("0 0 * * *")) { // Daily at midnight
            return from.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        } else if (cronExpression.equals("0 * * * *")) { // Every hour
            return from.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        } else if (cronExpression.equals("*/5 * * * *")) { // Every 5 minutes
            LocalDateTime next = from.plusMinutes(5);
            int minute = (next.getMinute() / 5) * 5;
            return next.withMinute(minute).withSecond(0).withNano(0);
        } else if (cronExpression.equals("0 0 0 * * MON")) { // Every Monday at midnight
            LocalDateTime next = from.plusDays(1);
            while (next.getDayOfWeek().getValue() != 1) { // Monday = 1
                next = next.plusDays(1);
            }
            return next.withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        
        // Default: add 1 hour (fallback)
        return from.plusHours(1);
    }
    
    public LocalDateTime getNextExecutionInTimeZone(String cronExpression, LocalDateTime from, String timeZone) {
        ZoneId zoneId = ZoneId.of(timeZone);
        ZonedDateTime zonedFrom = from.atZone(zoneId);
        
        LocalDateTime nextUtc = getNextExecution(cronExpression, zonedFrom.toLocalDateTime());
        ZonedDateTime zonedNext = nextUtc.atZone(zoneId);
        
        return zonedNext.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
    }
}