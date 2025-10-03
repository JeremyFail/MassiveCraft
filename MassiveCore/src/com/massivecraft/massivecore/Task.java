package com.massivecraft.massivecore;

import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.util.Txt;

import java.util.List;
import java.util.function.Supplier;

/**
 * This class is for tasks that are only run infrequently such as once a day
 * and whose frequency must stay consistent between server restarts.
 * An example is Faction taxing which must happen exactly once a day.
 */
public abstract class Task extends Engine
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	private boolean mustBeTaskServer = false;
	public boolean mustBeTaskServer() { return mustBeTaskServer; }
	public void setMustBeTaskServer(boolean mustBeTaskServer) { this.mustBeTaskServer = mustBeTaskServer; }

	private boolean logTimeSpent = false;
	public boolean isLoggingTimeSpent() { return logTimeSpent; }
	public void setLoggingTimeSpent(boolean logTimeSpent) { this.logTimeSpent = logTimeSpent; }

	private List<Supplier<Boolean>> conditions = new MassiveList<>();
	public List<Supplier<Boolean>> getConditions() { return conditions; }
	public void setConditions(List<Supplier<Boolean>> conditions) { this.conditions = conditions; }
	public void addCondition(Supplier<Boolean> condition) { this.conditions.add(condition); }
	public boolean areConditionsMet()
	{
		return conditions.stream().map(Supplier::get).allMatch(b -> b == true);
	}

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public Task()
	{
		this.setPeriod(60 * 20L); // Once a minute
	}

	// -------------------------------------------- //
	// INVOKE
	// -------------------------------------------- //

	@Override
	public void run()
	{
		// Should it be the task server?
		if (this.mustBeTaskServer() && ! MassiveCore.isTaskServer()) return;

		// So the delay millis is lower than one? (could for example be zero)
		// This probably means the task should not be executed at all.
		if (this.getPeriodMillis() < 1) return;

		// Other conditions
		if (!this.areConditionsMet()) return;

		// INVOCATION
		long nowMillis = System.currentTimeMillis();
		long lastMillis = this.getPreviousMillis();

		long currentInvocation = this.getInvocationFromMillis(nowMillis);
		long lastInvocation = this.getInvocationFromMillis(lastMillis);

		if (currentInvocation == lastInvocation) return;

		// Log time spent and invoke
		if (this.isLoggingTimeSpent())
		{
			String message = Txt.parse("<h>Running %s.", this.getClass().getSimpleName());
			this.getPlugin().log(message);
		}

		long startNano = System.nanoTime();
		this.invoke(nowMillis);
		this.setPreviousMillis(nowMillis);
		long endNano = System.nanoTime();

		double elapsedSeconds = (endNano - startNano) / 1000_000_000D;
		if (this.isLoggingTimeSpent())
		{
			String msg = String.format("<i>Took <h>%.2f <i>seconds.", elapsedSeconds);
			msg = Txt.parse(msg);
			this.getPlugin().log(msg);
		}
	}

	public abstract void invoke(long now);

	// -------------------------------------------- //
	// TASK MILLIS AND INVOCATION
	// -------------------------------------------- //
	// The invocation is the amount of periods from UNIX time to now.
	// It will increment by one when a period has passed.

	public abstract long getPreviousMillis();
	public abstract void setPreviousMillis(long millis);

	public abstract long getPeriodMillis();
	public abstract long getOffsetMillis();

	// Here we accept millis from inside the period by rounding down.
	private long getInvocationFromMillis(long millis)
	{
		return (millis - this.getOffsetMillis()) / this.getPeriodMillis();
	}

}
