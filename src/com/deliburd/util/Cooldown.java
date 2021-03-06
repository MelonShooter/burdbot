package com.deliburd.util;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import net.dv8tion.jda.api.entities.User;

public class Cooldown {
	/**
	 * The time for the cooldown in milliseconds
	 */
	private long cooldown;
	
	private ConcurrentHashMap<Long, Long> userCooldownTimeTracker;

	/**
	 * Initializes the cooldown
	 * 
	 * @param cooldown Time in milliseconds
	 */
	public Cooldown(long cooldown) {
		this.cooldown = cooldown;
		userCooldownTimeTracker = new ConcurrentHashMap<Long, Long>(11);
	}

	/**
	 * Resets the cooldown for a user
	 * 
	 * @param user The user to reset the cooldown for
	 */
	public void resetCooldown(User user) {
		final long currentmilliSecond = Instant.now().toEpochMilli();

		userCooldownTimeTracker.put(user.getIdLong(), currentmilliSecond);

		if(userCooldownTimeTracker.size() == 11) {
			pruneUserCooldown();
		}
	}

	/**
	 * Goes through the user cooldown hashmap and takes out mappings that are outdated
	 */
	private void pruneUserCooldown() {
		var entries = userCooldownTimeTracker.entrySet().iterator();

		while(entries.hasNext()) {
			var entry = entries.next();
			
			if(isCooldownOver(entry.getKey())) {
				entries.remove();
			}
		}
	}

	/**
	 * Gets the current total cooldown value
	 * 
	 * @return The current cooldown
	 */
	public long getTotalCooldown() {
		return cooldown;
	}
	
	/**
	 * Changes the current total cooldown value
	 * 
	 * @param newCooldown The new cooldown
	 */
	public void changeTotalCooldown(long newCooldown) {
		cooldown = newCooldown;
	}
	
	/**
	 * Checks if the cooldown is over for a user
	 * 
	 * @param user The user to check
	 * @return Whether the cooldown is up
	 */
	public boolean isCooldownOver(User user) {
		return isCooldownOver(user.getIdLong());
	}
	
	/**
	 * Gets the amount of time remaining in the cooldown for a user
	 * 
	 * @param user The user to check
	 * @return The time remaining in the cooldown in milliseconds. Returns 0 if the cooldown is over or the user never had a cooldown.
	 */
	public long getCooldownTimeRemaining(User user) {
		return getCooldownTimeRemaining(user.getIdLong());
	}
	
	/**
	 * Gets the amount of time remaining in the cooldown for a user given their ID
	 * 
	 * @param userID The user ID to check
	 * @return The time remaining in the cooldown in milliseconds. Returns 0 if the cooldown is over or the user never had a cooldown.
	 */
	private long getCooldownTimeRemaining(long userID) {
		final Long lastAction = userCooldownTimeTracker.get(userID);
		
		// Never had a cooldown or cooldown is over
		if(lastAction == null || lastAction + cooldown * 1000 <= Instant.now().toEpochMilli()) {
			return 0;
		} else {
			return lastAction + cooldown * 1000 - Instant.now().toEpochMilli();
		}
	}
	
	/**
	 * Checks if the cooldown is over for a user given their ID
	 * 
	 * @param userID The user ID to check
	 * @return Whether the cooldown is up
	 */
	private boolean isCooldownOver(long userID) {
		return getCooldownTimeRemaining(userID) == 0;
	}
}
