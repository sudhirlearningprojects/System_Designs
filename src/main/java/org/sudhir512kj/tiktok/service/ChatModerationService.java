package org.sudhir512kj.tiktok.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatModerationService {
    private final Set<String> bannedWords = loadBannedWords();
    private final Pattern urlPattern = Pattern.compile("https?://\\S+");
    
    public boolean containsInappropriateContent(String content) {
        // Layer 1: Banned words filter
        String lowerContent = content.toLowerCase();
        for (String word : bannedWords) {
            if (lowerContent.contains(word)) {
                log.warn("Banned word detected: {}", word);
                return true;
            }
        }
        
        // Layer 2: URL spam detection
        Matcher matcher = urlPattern.matcher(content);
        if (matcher.find()) {
            log.warn("URL detected in chat: {}", content);
            return true;
        }
        
        // Layer 3: Repetitive character detection
        if (hasRepetitiveCharacters(content, 5)) {
            log.warn("Repetitive characters detected: {}", content);
            return true;
        }
        
        return false;
    }
    
    private boolean hasRepetitiveCharacters(String text, int threshold) {
        if (text.length() < threshold) return false;
        
        for (int i = 0; i <= text.length() - threshold; i++) {
            char c = text.charAt(i);
            boolean allSame = true;
            for (int j = 1; j < threshold; j++) {
                if (text.charAt(i + j) != c) {
                    allSame = false;
                    break;
                }
            }
            if (allSame) return true;
        }
        return false;
    }
    
    private Set<String> loadBannedWords() {
        Set<String> words = new HashSet<>();
        words.add("spam");
        words.add("scam");
        // Add more banned words
        return words;
    }
}
