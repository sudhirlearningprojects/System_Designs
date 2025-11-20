package org.sudhir512kj.whatsapp.util;

import org.sudhir512kj.whatsapp.constants.WhatsAppConstants;

import java.util.regex.Pattern;

public final class ValidationUtils {
    
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    private ValidationUtils() {
        // Utility class
    }
    
    public static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && PHONE_PATTERN.matcher(phoneNumber).matches();
    }
    
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    public static boolean isValidGroupSize(int participantCount) {
        return participantCount > 0 && participantCount <= WhatsAppConstants.MAX_GROUP_PARTICIPANTS;
    }
    
    public static boolean isValidMessageContent(String content) {
        return content != null && !content.trim().isEmpty() && content.length() <= 4096;
    }
    
    public static boolean isValidUserName(String name) {
        return name != null && !name.trim().isEmpty() && name.length() <= 100;
    }
}