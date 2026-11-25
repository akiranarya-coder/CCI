package com.example.cci.core.constants

enum class UserRole(
    val displayName: String,
    val canAddTest: Boolean,
    val canDeleteTest: Boolean,
    val canChangeover: Boolean,
    val canLogMaintenance: Boolean,
    val canApproveMaintenance: Boolean,
    val canManageUsers: Boolean,
    val canEditSettings: Boolean
) {
    ADMIN(
        displayName = "Plant Administrator",
        canAddTest = true,
        canDeleteTest = true,
        canChangeover = true,
        canLogMaintenance = true,
        canApproveMaintenance = true,
        canManageUsers = true,
        canEditSettings = true
    ),
    SHIFT_IN_CHARGE(
        displayName = "Shift In-Charge",
        canAddTest = true,
        canDeleteTest = true,
        canChangeover = true,
        canLogMaintenance = true,
        canApproveMaintenance = true,
        canManageUsers = false,
        canEditSettings = true
    ),
    OPERATOR(
        displayName = "Lab Technician / Operator",
        canAddTest = true,
        canDeleteTest = false,
        canChangeover = false,
        canLogMaintenance = false,
        canApproveMaintenance = false,
        canManageUsers = false,
        canEditSettings = false
    ),
    MAINTENANCE(
        displayName = "Maintenance Engineer",
        canAddTest = false,
        canDeleteTest = false,
        canChangeover = false,
        canLogMaintenance = true,
        canApproveMaintenance = false,
        canManageUsers = false,
        canEditSettings = false
    ),
    MANAGEMENT(
        displayName = "Plant Management",
        canAddTest = false,
        canDeleteTest = false,
        canChangeover = false,
        canLogMaintenance = false,
        canApproveMaintenance = false,
        canManageUsers = false,
        canEditSettings = false
    );

    companion object {
        fun fromName(name: String): UserRole =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: OPERATOR
    }
}

object AppConstants {
    const val COMPANY_NAME = "Evonith Steel"
    const val DEPARTMENT_NAME = "Sinter Plant — Coke Crushing"
    const val LINE_01 = "Crusher Line-01"
    const val LINE_02 = "Crusher Line-02"

    val CRUSHER_LINES = listOf(LINE_01, LINE_02)
    val SHIFTS = listOf("A", "B", "C")
    val COKE_TYPES = listOf("Breeze Coke", "Nut Coke", "Imported Coke", "Mixed Coke")
    val COKE_SOURCES = listOf(
        "BF Stock House Bay-1",
        "BF Stock House Bay-2",
        "Coke Oven Wharf",
        "Yard Stockpile North"
    )
    val CHANGEOVER_REASONS = listOf(
        "Scheduled Monthly Changeover",
        "Roller Wear / Gap Exceeded",
        "Preventive Maintenance",
        "Mechanical Breakdown",
        "Chute / Screen Choking",
        "Operational Requirement"
    )
    val MAINTENANCE_TYPES = listOf(
        "Roller Turning",
        "Gap Calibration",
        "Bearing Greasing",
        "Screen Mesh Replacement",
        "Liner Plate Inspection",
        "Belt & Drive Check",
        "General Breakdown Repair"
    )
    val ROLLER_CONDITIONS = listOf(
        "Good / Uniform Surface",
        "Good after turning",
        "Minor Grooving",
        "Moderate Wear — Monitor",
        "Heavy Wear — Schedule Turning"
    )
}
