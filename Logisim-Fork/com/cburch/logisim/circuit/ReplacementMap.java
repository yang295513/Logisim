/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.circuit;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.cburch.logisim.comp.Component;

public class ReplacementMap {
	private boolean frozen;
	private ConcurrentHashMap<Component, HashSet<Component>> map;
	private ConcurrentHashMap<Component, HashSet<Component>> inverse;

	public ReplacementMap() {
		this(new ConcurrentHashMap<Component, HashSet<Component>>(), new ConcurrentHashMap<Component, HashSet<Component>>());
	}

	public ReplacementMap(Component oldComp, Component newComp) {
		this(new ConcurrentHashMap<Component, HashSet<Component>>(), new ConcurrentHashMap<Component, HashSet<Component>>());
		HashSet<Component> oldSet = new HashSet<Component>(3);
		oldSet.add(oldComp);
		HashSet<Component> newSet = new HashSet<Component>(3);
		newSet.add(newComp);
		map.put(oldComp, newSet);
		inverse.put(newComp, oldSet);
	}

	private ReplacementMap(ConcurrentHashMap<Component, HashSet<Component>> map, ConcurrentHashMap<Component, HashSet<Component>> inverse) {
		this.map = map;
		this.inverse = inverse;
	}

	public void add(Component comp) {
		if (frozen) {
			throw new IllegalStateException("cannot change map after frozen");
		}
		inverse.put(comp, new HashSet<Component>(3));
	}

	void append(ReplacementMap next) {
		for (Map.Entry<Component, HashSet<Component>> e : next.map.entrySet()) {
			Component b = e.getKey();
			HashSet<Component> cs = e.getValue(); // what b is replaced by
			HashSet<Component> as = this.inverse.remove(b); // what was replaced
															// to get b
			if (as == null) { // b pre-existed replacements so
				as = new HashSet<Component>(3); // we say it replaces itself.
				as.add(b);
			}

			for (Component a : as) {
				HashSet<Component> aDst = this.map.get(a);
				if (aDst == null) { // should happen when b pre-existed only
					aDst = new HashSet<Component>(cs.size());
					this.map.put(a, aDst);
				}
				aDst.remove(b);
				aDst.addAll(cs);
			}

			for (Component c : cs) {
				HashSet<Component> cSrc = this.inverse.get(c); // should always
																// be null
				if (cSrc == null) {
					cSrc = new HashSet<Component>(as.size());
					this.inverse.put(c, cSrc);
				}
				cSrc.addAll(as);
			}
		}

		for (Map.Entry<Component, HashSet<Component>> e : next.inverse.entrySet()) {
			Component c = e.getKey();
			if (!inverse.containsKey(c)) {
				HashSet<Component> bs = e.getValue();
				if (!bs.isEmpty()) {
					System.err.println("internal error: component replaced but not represented"); // OK
				}
				inverse.put(c, new HashSet<Component>(3));
			}
		}
	}

	void freeze() {
		frozen = true;
	}

	public Collection<Component> get(Component prev) {
		return map.get(prev);
	}

	public Collection<? extends Component> getAdditions() {
		return inverse.keySet();
	}

	public Collection<Component> getComponentsReplacing(Component comp) {
		return map.get(comp);
	}

	ReplacementMap getInverseMap() {
		return new ReplacementMap(inverse, map);
	}

	public Collection<? extends Component> getRemovals() {
		return map.keySet();
	}

	public Collection<Component> getReplacedComponents() {
		return map.keySet();
	}

	public boolean isEmpty() {
		return map.isEmpty() && inverse.isEmpty();
	}

	public void print(PrintStream out) {
		boolean found = false;
		for (Component c : getRemovals()) {
			if (!found)
				out.println("  removals:");
			found = true;
			out.println("    " + c.toString());
		}
		if (!found)
			out.println("  removals: none");

		found = false;
		for (Component c : getAdditions()) {
			if (!found)
				out.println("  additions:");
			found = true;
			out.println("    " + c.toString());
		}
		if (!found)
			out.println("  additions: none");
	}

	public void put(Component prev, Collection<? extends Component> next) {
		if (frozen) {
			throw new IllegalStateException("cannot change map after frozen");
		}

		HashSet<Component> repl = map.get(prev);
		if (repl == null) {
			repl = new HashSet<Component>(next.size());
			map.put(prev, repl);
		}
		repl.addAll(next);

		for (Component n : next) {
			repl = inverse.get(n);
			if (repl == null) {
				repl = new HashSet<Component>(3);
				inverse.put(n, repl);
			}
			repl.add(prev);
		}
	}

	public void remove(Component comp) {
		if (frozen) {
			throw new IllegalStateException("cannot change map after frozen");
		}
		map.put(comp, new HashSet<Component>(3));
	}

	public void replace(Component prev, Component next) {
		put(prev, Collections.singleton(next));
	}

	public void reset() {
		map.clear();
		inverse.clear();
	}
}
