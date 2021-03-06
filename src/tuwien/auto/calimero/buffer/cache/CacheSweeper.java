/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2011 B. Malinowsky

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package tuwien.auto.calimero.buffer.cache;

import tuwien.auto.calimero.exception.KNXIllegalArgumentException;

/**
 * Used to remove expired entries from a cache.
 * <p>
 * The cache sweeper is running in its own thread, waking up for work every sweep time
 * interval set by the user. Then {@link Cache#removeExpired()} is invoked on the
 * specified cache.<br>
 * Interruption policy: complete any ongoing sweeping, then cleanup and exit.
 * 
 * @author B. Malinowsky
 * @see Cache
 */
public final class CacheSweeper extends Thread
{
	// interval in seconds
	private int sweepInterval;
	private final Cache cache;
	private final Object lock = new Object();

	/**
	 * Creates a {@link CacheSweeper} for <code>cache</code> with the given
	 * <code>sweepInterval</code>.
	 * <p>
	 * 
	 * @param cache the cache for which {@link Cache#removeExpired()} should be invoked
	 * @param sweepInterval lapse of time between sweeping in seconds
	 */
	public CacheSweeper(final Cache cache, final int sweepInterval)
	{
		super("Cache sweeper");
		this.cache = cache;
		setSweepInterval(sweepInterval);
		// priority below normal
		setPriority(3);
		setDaemon(true);
	}

	/**
	 * Sets a new sweep interval.
	 * <p>
	 * If the cache sweeper is in waiting state for next sweep, the new interval is
	 * immediately applied and checked against elapsed time.
	 * 
	 * @param interval new time interval between sweeping in seconds
	 */
	public void setSweepInterval(final int interval)
	{
		if (interval <= 0)
			throw new KNXIllegalArgumentException("sweep interval has to be > 0");
		synchronized (lock) {
			sweepInterval = interval;
			lock.notify();
		}
	}

	/**
	 * Returns the time interval between {@link Cache#removeExpired()} calls used by this
	 * cache sweeper.
	 * <p>
	 * 
	 * @return the time in seconds
	 */
	public int getSweepInterval()
	{
		synchronized (lock) {
			return sweepInterval;
		}
	}

	/**
	 * Stops the sweeper and quits the thread.
	 * <p>
	 */
	public void stopSweeper()
	{
		interrupt();
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run()
	{
		try {
			while (true) {
				final long start = System.currentTimeMillis();
				synchronized (lock) {
					long remaining = sweepInterval * 1000;
					while (remaining > 0) {
						lock.wait(remaining);
						remaining = start + sweepInterval * 1000 - System.currentTimeMillis();
					}
				}
				cache.removeExpired();
			}
		}
		catch (final InterruptedException e) {
			// just let thread exit
		}
	}
}
