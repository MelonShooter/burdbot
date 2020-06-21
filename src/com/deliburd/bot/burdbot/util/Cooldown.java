package com.deliburd.bot.burdbot.util;

import java.time.Instant;

public class Cooldown {
	/**
	 * The time for the cooldown in milliseconds
	 */
	private long cooldown;
	
	/**
	 * The time since the epoch when the cooldown was last reset
	 */
	private long lastAction;

	/**
	 * Initializes the cooldown
	 * 
	 * @param cooldown Time in milliseconds
	 */
	public Cooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	/**
	 * Resets the cooldown
	 */
	public void ResetCooldown() {
		lastAction = Instant.now().toEpochMilli();
	}

	/**
	 * Gets the current total cooldown value
	 * 
	 * @return The current cooldown
	 */
	public long GetTotalCooldown() {
		return cooldown;
	}
	
	/**
	 * Changes the current total cooldown value
	 * 
	 * @param newCooldown The new cooldown
	 */
	public void ChangeTotalCooldown(long newCooldown) {
		cooldown = newCooldown;
	}
	
	/**
	 * Checks if the cooldown is over
	 * 
	 * @return Whether the cooldown is up
	 */
	public boolean IsCooldownOver() {
		return GetCooldownTimeRemaining() == 0;
	}
	
	/**
	 * Gets the amount of time remaining in the cooldown.
	 * 
	 * @return The time remaining in the cooldown in milliseconds. Returns 0 if the cooldown is over.
	 */
	public long GetCooldownTimeRemaining() {
		long timeRemaining = lastAction + cooldown - Instant.now().toEpochMilli();
		
		if(timeRemaining > 0) {
			return timeRemaining;
		} else {
			return 0;
		}
	}
}
