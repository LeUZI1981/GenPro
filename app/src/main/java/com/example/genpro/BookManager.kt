package com.example.genpro

import kotlin.random.Random

object BookManager {
    
    // List of knowledge skills for libraries
    private val knowledgeSkills = listOf(
        "Wiedza o Naturze", "Wiedza o Magii", "Wiedza o Wojnie", "Wiedza o Alchemy",
        "Wiedza o Historia", "Wiedza o Prawie", "Wiedza o Astrologia", "Wiedza o Religia",
        "Wiedza o Rzemiosłach", "Wiedza o Handlu", "Wiedza o Zwierzetach", "Wiedza o Botanice",
        "Wiedza o Minerałach", "Wiedza o Architekturze", "Wiedza o Medycynie", "Wiedza o Astronomii"
    )

    fun generateBookForSkill(skill: String): Book {
        val basePrice = Random.nextInt(50, 150)
        return Book(
            name = "${skill.replace("Wiedza o ", "")} - Księga",
            skill = skill,
            price = basePrice,
            quality = Random.nextInt(60, 101)
        )
    }

    fun generateLibraryBooks(settlementType: SettlementType, week: Int): LibraryInventory {
        val bookCount = when (settlementType) {
            SettlementType.CITY -> 5
            SettlementType.TOWN -> 3
            SettlementType.VILLAGE -> 0
        }

        if (bookCount == 0) return LibraryInventory()

        val borrowingBooks = knowledgeSkills
            .shuffled()
            .take(bookCount)
            .map { generateBookForSkill(it) }

        val buyingBooks = knowledgeSkills
            .shuffled()
            .take(bookCount)
            .map { generateBookForSkill(it) }

        return LibraryInventory(
            borrowingBooks = borrowingBooks,
            buyingBooks = buyingBooks,
            lastRefreshWeek = week
        )
    }

    fun performBookLearning(
        heroData: String,
        skill: String,
        learningHours: Double,
        intelligence: Int
    ): Pair<String, Int> {
        // Only process if learning time >= 3 hours
        if (learningHours < 3.0) {
            return addStudyPoints(heroData, skill, 1)
        }

        // Perform K100 test on Intelligence
        val testResult = Random.nextInt(1, 101)
        val pointsToAdd = when {
            testResult > intelligence -> 1  // Test failed
            testResult <= intelligence && testResult + 30 >= intelligence -> 2  // Test passed
            testResult + 30 < intelligence -> 3  // Test passed significantly
            else -> 1
        }

        return addStudyPoints(heroData, skill, pointsToAdd)
    }

    private fun addStudyPoints(heroData: String, skill: String, points: Int): Pair<String, Int> {
        val lines = heroData.split("\n")
        val skillLineIndex = lines.indexOfFirst { 
            it.trim().startsWith("$skill:") && !it.contains("PN=")
        }

        return if (skillLineIndex >= 0) {
            val skillLine = lines[skillLineIndex]
            val parts = skillLine.split(":".toRegex(), limit = 3)
            if (parts.size >= 2) {
                val skillNamePart = parts[0]
                val skillDataPart = parts[1]
                val restPart = if (parts.size > 2) ":" + parts[2] else ""

                // Parse current rank and PN
                val rankMatch = Regex("Rz\\s*(\\d+)").find(skillDataPart)
                val pnMatch = Regex("PN\\s*(\\d+)").find(skillDataPart)

                val currentRank = rankMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val currentPN = pnMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val newPN = currentPN + points

                // Check if rank should increase (every 10 PN = 1 rank increase)
                val rankIncrease = newPN / 10
                val newRank = currentRank + rankIncrease
                val remainingPN = newPN % 10

                val updatedSkillData = if (newRank > currentRank) {
                    skillDataPart.replace(Regex("Rz\\s*\\d+"), "Rz $newRank").replace(Regex("PN\\s*\\d+"), "PN $remainingPN")
                } else {
                    skillDataPart.replace(Regex("PN\\s*\\d+"), "PN $newPN")
                }

                val updatedLines = lines.toMutableList()
                updatedLines[skillLineIndex] = "$skillNamePart:$updatedSkillData$restPart"
                Pair(updatedLines.joinToString("\n"), rankIncrease)
            } else {
                Pair(heroData, 0)
            }
        } else {
            // Skill doesn't exist, add it with rank 0, PN = points
            val newSkillLine = "$skill: Rz 0 PN $points Uczeń"
            Pair(heroData + "\n" + newSkillLine, 0)
        }
    }

    fun canBorrowBook(heroData: String, skill: String): Boolean {
        // Check if hero already has this skill at rank "Uczeń" or higher
        val lines = heroData.split("\n")
        val skillLine = lines.find { 
            it.trim().startsWith("$skill:") && !it.contains("PN=")
        }

        if (skillLine == null) return true  // Can borrow if skill doesn't exist yet

        val rankMatch = Regex("Rz\\s*(\\d+)").find(skillLine)
        val currentRank = rankMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

        // "Uczeń" is rank 1 in the system (0 = no skill, 1 = Uczeń, 2+ = higher ranks)
        return currentRank < 1
    }
}
