package me.vadim.archive.util.threading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author vadim
 */
public class LocalExecutors {

	public static ExecutorService newExtendedThreadPool() {
		return new SafeExecutorService(0, Integer.MAX_VALUE,
									   60L, TimeUnit.SECONDS,
									   new SynchronousQueue<Runnable>());
	}

	public static ExecutorService newExtendedThreadPool(ThreadFactory threadFactory) {
		return new SafeExecutorService(0, Integer.MAX_VALUE,
									   60L, TimeUnit.SECONDS,
									   new SynchronousQueue<Runnable>(),
									   threadFactory);
	}

	public static ExecutorService newExtendedThreadPool(int numThreads) {
		return new SafeExecutorService(numThreads, Integer.MAX_VALUE,
									   60L, TimeUnit.SECONDS,
									   new SynchronousQueue<Runnable>());
	}

	public static ExecutorService newExtendedThreadPool(int numThreads, int maxThreads) {
		return new SafeExecutorService(numThreads, maxThreads,
									   60L, TimeUnit.SECONDS,
									   new SynchronousQueue<Runnable>());
	}

}
