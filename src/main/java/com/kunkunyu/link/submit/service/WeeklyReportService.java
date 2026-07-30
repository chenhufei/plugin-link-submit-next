package com.kunkunyu.link.submit.service;

public interface WeeklyReportService {

    String generateWeeklyReport();

    void sendWeeklyReport(String email);
}
