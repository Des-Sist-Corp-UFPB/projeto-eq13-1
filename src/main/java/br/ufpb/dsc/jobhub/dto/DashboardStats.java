package br.ufpb.dsc.jobhub.dto;

public record DashboardStats(
        long totalJobs,
        long publishedJobs,
        long pendingJobs,
        long archivedJobs,
        long totalApplications,
        long totalUsers,
        long remoteJobs,
        long hybridPbJobs,
        long presencialPbJobs,
        long jobsLast7Days,
        long applicationsLast7Days,
        long totalViews
) {
}
