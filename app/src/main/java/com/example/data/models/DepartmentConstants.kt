package com.example.data.models

object DepartmentConstants {
    const val DEFAULT_DEPARTMENT = "Computer Science"

    // Computer Science based departments
    const val DEPT_COMPUTER_SCIENCE = "Computer Science"
    const val DEPT_AI_AND_DATA_SCIENCE = "Artificial Intelligence and Data Science"
    const val DEPT_COMPUTER_APPLICATION = "Computer Application"
    const val DEPT_ARTIFICIAL_INTELLIGENCE = "Artificial Intelligence"
    const val DEPT_INFORMATION_TECH = "Information Tech"

    // Other Engineering departments
    const val DEPT_ELECTRONICS_COMM = "Electronics & Comm"
    const val DEPT_MECHANICAL_ENG = "Mechanical Eng"
    const val DEPT_ELECTRICAL_ENG = "Electrical Eng"
    const val DEPT_CIVIL_ENG = "Civil Eng"

    /**
     * All official academic departments available across the campus portal.
     * Includes dedicated Computer Science based departments:
     * - Artificial Intelligence and Data Science
     * - Computer Application
     * - Artificial Intelligence
     * - Computer Science
     * - Information Tech
     */
    val ALL_DEPARTMENTS: List<String> = listOf(
        DEPT_COMPUTER_SCIENCE,
        DEPT_AI_AND_DATA_SCIENCE,
        DEPT_COMPUTER_APPLICATION,
        DEPT_ARTIFICIAL_INTELLIGENCE,
        DEPT_INFORMATION_TECH,
        DEPT_ELECTRONICS_COMM,
        DEPT_MECHANICAL_ENG,
        DEPT_ELECTRICAL_ENG,
        DEPT_CIVIL_ENG
    )

    /**
     * Check if a department is Computer Science based
     */
    fun isComputerScienceBased(department: String): Boolean {
        val clean = department.trim().uppercase()
        return clean.contains("COMPUTER") ||
                clean.contains("ARTIFICIAL INTELLIGENCE") ||
                clean.contains("DATA SCIENCE") ||
                clean.contains("INFORMATION TECH")
    }
}
