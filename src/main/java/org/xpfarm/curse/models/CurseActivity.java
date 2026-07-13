package org.xpfarm.curse.models;

/**
 * Interface for curse activities that can be tracked by the leaderboard system.
 * This allows different curse mechanics to integrate with the leaderboard.
 */
public interface CurseActivity {
    /**
     * Get the current round/stage of the curse activity.
     * For mechanisms that complete in one stage, return 1 if successful, 0 if failed.
     * @return Current round/stage number
     */
    int getCurrentRound();

    /**
     * Get the total number of kills/eliminations during this curse activity.
     * @return Total kill count
     */
    int getTotalKills();

    /**
     * Get the start time of this curse activity in milliseconds.
     * @return Start time in milliseconds
     */
    long getStartTime();

    /**
     * Check if this curse activity was completed successfully.
     * @return true if completed successfully, false otherwise
     */
    boolean isSuccessful();
}
