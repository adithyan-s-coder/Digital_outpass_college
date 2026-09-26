package com.example.data.models

enum class UserRole(val displayName: String) {
    STUDENT("Student"),
    STAFF_ADVISOR("Staff Advisor"),
    HOD("Head of Dept (HOD)"),
    SECURITY_OFFICER("Security Officer"),
    ADMIN("Administrator")
}

enum class OutpassType(val displayName: String, val maxHours: Int) {
    LOCAL("Local Outpass", 6),
    HOME("Home Outpass", 72),
    EMERGENCY("Emergency Outpass", 24),
    SPECIAL_EVENT("Special Event", 48)
}

enum class OutpassStatus(val displayName: String) {
    PENDING_STAFF("Pending Staff Approval"),
    PENDING_HOD("Pending HOD Sign-off"),
    APPROVED("Approved & Ready for Exit"),
    REJECTED("Rejected"),
    CHECKED_OUT("Outside Campus (Active)"),
    CHECKED_IN("Returned (Completed)"),
    EXPIRED("Expired")
}
