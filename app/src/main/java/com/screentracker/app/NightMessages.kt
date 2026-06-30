package com.screentracker.app

/**
 * Night message bank for gentle sleep interventions.
 *
 * Design principles:
 * - Ultra-short (half-asleep brain can't parse long sentences)
 * - Present-tense, permission-giving
 * - Never a command to sleep (creates performance pressure)
 * - Never a number, never false cheer
 * - One line only, designed to be closed not consumed
 */
object NightMessages {

    private val messages = listOf(
        // Normalizing the wake
        "Waking in the night is normal.",
        "Nothing is wrong. You just surfaced.",
        "Bodies wake. This is one of those times.",

        // Removing pressure to sleep
        "You don't have to fall asleep. Just rest.",
        "There's nothing you need to do right now.",
        "Rest counts, even without sleep.",

        // Down-regulating
        "Let your shoulders drop.",
        "Breathe out, slower than you breathed in.",
        "Heavy eyes. Loose hands. Quiet.",

        // Gentle exit
        "Set me down. I'll be here in the morning.",
        "Nothing here is worth waking up for.",
        "Eyes closed is enough.",

        // Additional calming messages
        "This will pass.",
        "You're okay.",
        "Just breathe."
    )

    /**
     * Returns a random message, avoiding repeats within the same night.
     * @param usedIndices Indices already shown this night
     * @return Pair of (message, index) for tracking
     */
    fun getRandomMessage(usedIndices: Set<Int> = emptySet()): Pair<String, Int> {
        val availableIndices = messages.indices.filter { it !in usedIndices }

        // If all messages used this night, reset and pick any
        val pool = if (availableIndices.isEmpty()) messages.indices.toList() else availableIndices

        val index = pool.random()
        return Pair(messages[index], index)
    }

    /**
     * Returns a specific message by index (for testing/debugging)
     */
    fun getMessage(index: Int): String = messages.getOrElse(index) { messages.first() }

    /**
     * Total message count
     */
    fun count(): Int = messages.size
}
