package me.vadim.archive.v2;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Trace what page a link originated from.
 *
 * @author vadim
 */
public class LinkAggregator {


	private LinkAggregator(String link, LinkAggregator parent) {
		this.link   = link;
		this.parent = parent;
	}

	public static LinkAggregator root(String root) {
		return new LinkAggregator(root, null);
	}

	private final String link;
	private final LinkAggregator parent;
	private final Stack<LinkAggregator> stack = new Stack<>();

	public LinkAggregator push(String link) {
		return stack.push(new LinkAggregator(link, this));
	}

	public void pushAll(Collection<String> links) {
		for (String l : links)
			push(l);
	}

	// searches only each level as it goes up
	public LinkAggregator select(String link) {
		if (link == null)
			throw new IllegalArgumentException("link");

		LinkAggregator node = this;
		do {
			if (node.link.equals(link))
				return node;
			for (LinkAggregator la : node.stack)
				if (link.equals(la.link))
					return la;
			node = node.parent;
		}
		while (node != null);

		return null;
	}

	// deep search
	public LinkAggregator search(String link) {
		if (link == null)
			throw new IllegalArgumentException("link");

		LinkAggregator root = this;
		do
			root = root.parent;
		while (root.parent != null);

		Stack<LinkAggregator> stack = new Stack<>();
		stack.push(root);
		while (!stack.isEmpty()) {
			LinkAggregator la = stack.pop();
			if (link.equals(la.link))
				return la;
			stack.addAll(la.stack);
		}

		return null;
	}

	public String[] trace() {
		List<String> trace = new ArrayList<>();

		LinkAggregator node = this;
		do {
			trace.add(node.link);
			node = node.parent;
		}
		while (node != null);

		return trace.toArray(String[]::new);
	}

	public void printTrace(PrintStream out) {
		String[] trace = trace();
		out.println("Link " + trace[0]);
		for (int i = 1; i < trace.length; i++)
			 out.println("\tfrom " + trace[i]);
		out.println();
	}

	public void printTrace(Logger log) {
		String[] trace = trace();
		log.warning("Link " + trace[0]);
		for (int i = 1; i < trace.length; i++)
			 log.warning("\tfrom " + trace[i]);
		log.warning("");
	}

	public void forEach(Consumer<LinkAggregator> action) {
		for (LinkAggregator la : stack)
			la.forEach(action);
		action.accept(this);
	}

	@Override
	public String toString() {
		return String.format("{ \"%s\": %s }", link, stack);
	}

}
