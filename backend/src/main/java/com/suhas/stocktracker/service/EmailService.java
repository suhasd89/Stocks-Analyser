package com.suhas.stocktracker.service;

public interface EmailService {
    void sendEmail(String[] recipients, String from, String senderName, String subject, String htmlBody);
}
