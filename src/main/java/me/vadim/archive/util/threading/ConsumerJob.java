package me.vadim.archive.util.threading;

import java.util.function.Consumer;

/**
 * Wrapper for a {@link Consumer} and a precomputed value, to easily integrate
 * with {@link java.util.concurrent.Executor#execute(Runnable) Executor} and
 * {@link java.util.concurrent.ExecutorService#submit(Runnable) ExecutorService} methods.
 *
 * @author vadim
 */
public class ConsumerJob<T> implements Runnable {

	private final T t;
	private final Consumer<T> job;

	public ConsumerJob(T t, Consumer<T> job) {
		this.t   = t;
		this.job = job;
	}

	@Override
	public void run() {
		job.accept(t);
	}

}
