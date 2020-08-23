package com.deliburd.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.deliburd.bot.burdbot.Main;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;

public class ActivitySwitcher {
	private static List<Pair<String, Long>> states;
	private static int currentStateIndex;
	private static Instant instantToSwitch;
	private static Timer stateSwitcherTimer;
	
	static {
		states = new ArrayList<>();
		stateSwitcherTimer = new Timer(true);
		startActivitySwitching();
	}
	
	private ActivitySwitcher() {}
	
	/**
	 * Starts a timer to switch activities once the time for them is up.
	 */
	private static void startActivitySwitching() {
		stateSwitcherTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				JDA JDAInstance = Main.getJDAInstance();
				
				if(JDAInstance == null) {
					return;
				}
				
				Instant currentInstant = Instant.now();
				boolean stateHasExpired = instantToSwitch == null || currentInstant.isAfter(instantToSwitch);
				
				synchronized(ActivitySwitcher.class) {
					if(stateHasExpired && !states.isEmpty()) {
						if(currentStateIndex >= states.size()) {
							currentStateIndex = 0;
						}
						
						var newStateInfo = states.get(currentStateIndex);
						String stateText = newStateInfo.getKey();
						long stateDuration = newStateInfo.getValue();
						
						JDAInstance.getPresence().setActivity(Activity.playing(stateText));
						instantToSwitch = currentInstant.plus(stateDuration, ChronoUnit.MILLIS);
						
						currentStateIndex++;
					}
				}
			}
		}, 10000, 10000);
	}

	/**
	 * Adds an activity to rotate off with for the bot.
	 * 
	 * @param activity The activity's text
	 * @param time The time to keep the activity displayed for before moving on. The time internally is 
	 * converted to milliseconds and becomes Long.MAX_VALUE if it overflows. Must be more than 0.
	 * @param timeUnit The unit of time the time is in
	 */
	public static synchronized void addState(String activity, long time, TimeUnit timeUnit) {
		if(time <= 0) {
			throw new IllegalArgumentException("The time given must be over 0.");
		}
		
		long timeInMilliseconds = timeUnit.toMillis(time);
		var stateInfo = new Pair<String, Long>(activity, timeInMilliseconds);
		
		states.add(stateInfo);
	}
}
