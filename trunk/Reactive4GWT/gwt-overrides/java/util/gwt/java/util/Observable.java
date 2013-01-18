/*
 * Copyright 2011-2013 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 package java.util;

import java.util.Observer;
import java.util.Vector;
 
 /**
  * The GWT version of the java.util.Observable class.
  * @author akarnokd, 2013.01.18.
  */
public class Observable {
	final Vector<Observer> observers = new Vector<Observer>();
	
	boolean changed;
	
	public void addObserver(Observer o) {
		if (o != null && !observers.contains(o)) {
			observers.add(o);
		}
	}
	public void deleteObserver(Observer o) {
		observers.remove(o);
	}
	public void deleteObservers() {
		observers.clear();
	}
	public void notifyObservers(Object value) {
		if (changed) {
			for (Observer o : new ArrayList<Observer>(observers)) {
				o.update(this, value);
			}
			changed = false;
		}
	}
	public void notifyObservers() {
		notifyObservers(null);
	}
	protected void setChanged() {
		changed = true;
	}
	public boolean hasChanged() {
		return changed;
	}
	protected void clearChanged() {
		changed = false;
	}
	public int countObservers() {
		return observers.size();
	}
}