package com.example.data.model

enum class ProjectStatus(val label: String) {
    ACTIVE("Active"),
    COMPLETED("Completed"),
    ARCHIVED("Archived")
}

enum class ProjectType(val label: String) {
    RESIDENTIAL_ROOFTOP("Residential Rooftop"),
    COMMERCIAL_ROOFTOP("Commercial Rooftop"),
    INDUSTRIAL("Industrial Solar"),
    GROUND_MOUNTED("Ground-Mounted"),
    HYBRID_STORAGE("Hybrid + Battery"),
    OFF_GRID("Off-Grid")
}

enum class DashboardProjectFilter(val label: String) {
    ACTIVE("Active"),
    COMPLETED("Completed"),
    ALL("All Projects")
}

enum class DashboardTimeFilter(val label: String) {
    CURRENT_MONTH("This Month"),
    ALL_TIME("All Time")
}

enum class DashboardSectionId(val title: String, val subtitle: String) {
    STAGE_OVERVIEW("Project Overview by Stage", "Distribution of projects across pipeline stages"),
    PROJECTS_ATTENTION("Projects Needing Attention", "Active projects past due or awaiting progression"),
    RECENT_PROJECTS("Recently Updated Projects", "Latest customer solar projects modified"),
    RECENT_ACTIVITY("Recent Activity", "Latest local audit log events")
}

enum class CustomerFilterOption(val label: String) {
    ACTIVE("Active"),
    ARCHIVED("Archived"),
    ALL("All")
}

enum class CustomerSortOption(val label: String) {
    RECENTLY_UPDATED("Recently Updated"),
    NAME_ASC("Name (A–Z)"),
    CAPACITY_DESC("Highest kW Capacity"),
    NEWEST("Newest First")
}

object DefaultStages {
    val STAGES: List<String> = listOf(
        "Work Order Signing",
        "Document Collection",
        "MNRE Registration",
        "Feasibility Assessment",
        "System Delivery",
        "Industrial Works",
        "Electrical Works",
        "Completion Report Submission",
        "Net Metering",
        "Application Installation",
        "Second Loan Disbursement",
        "Subsidy Processing"
    )

    fun encodeStages(stages: List<String>): String =
        stages.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")

    fun decodeStages(raw: String): List<String> {
        val parsed = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return if (parsed.isEmpty()) STAGES else parsed
    }
}

data class DashboardPreferences(
    val sectionOrder: List<DashboardSectionId> = DashboardSectionId.entries,
    val hiddenSections: Set<DashboardSectionId> = emptySet(),
    val projectFilter: DashboardProjectFilter = DashboardProjectFilter.ACTIVE,
    val timeFilter: DashboardTimeFilter = DashboardTimeFilter.ALL_TIME
)

data class DatabaseStats(
    val customerCount: Int = 0,
    val projectCount: Int = 0,
    val checklistCount: Int = 0,
    val photoCount: Int = 0,
    val activityCount: Int = 0,
    val databaseSizeBytes: Long = 0L
)
