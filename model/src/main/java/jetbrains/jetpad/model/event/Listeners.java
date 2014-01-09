/*
 * Copyright 2012-2014 JetBrains s.r.o
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.jetpad.model.event;


import java.util.ArrayList;
import java.util.List;

public class Listeners<ListenerT> {
  private List<ListenerT> myListeners;
  private FireData<ListenerT> myFireData;

  public boolean isEmpty() {
    if (myListeners == null) return true;
    return myListeners.isEmpty();
  }

  public Registration add(final ListenerT l) {
    if (myFireData != null) {
      if (myFireData.toAdd == null) {
        myFireData.toAdd = new ArrayList<ListenerT>(1);
      }
      myFireData.toAdd.add(l);
    } else {
      if (myListeners == null) {
        myListeners = new ArrayList<ListenerT>(1);
      }
      myListeners.add(l);
    }
    return new Registration() {
      @Override
      public void remove() {
        if (myFireData != null) {
          if (myFireData.toRemove == null) {
            myFireData.toRemove = new ArrayList<ListenerT>(1);
          }
          myFireData.toRemove.add(l);
        } else {
          myListeners.remove(l);
        }
      }
    };
  }

  public void fire(final ListenerCaller<ListenerT> h) {
    if (isEmpty()) return;
    beforeFire();
    try {
      for (ListenerT l : myListeners) {
        if (isRemoved(l)) continue;
        try {
          h.call(l);
        } catch (Throwable t) {
          Callbacks.handleException(t);
        }
      }
    } finally {
      afterFire();
    }
  }

  private boolean isRemoved(ListenerT l) {
    return myFireData.toRemove != null && myFireData.toRemove.contains(l);
  }

  private void beforeFire() {
    if (myFireData == null) {
      myFireData = new FireData<ListenerT>();
    }
    myFireData.depth++;
  }

  private void afterFire() {
    myFireData.depth--;
    if (myFireData.depth == 0) {
      if (myFireData.toRemove != null) {
        myListeners.removeAll(myFireData.toRemove);
        myFireData.toRemove = null;
      }
      if (myFireData.toAdd != null) {
        myListeners.addAll(myFireData.toAdd);
        myFireData.toAdd = null;
      }
      myFireData = null;
    }
  }

  private static class FireData<ListenerT> {
    private int depth;
    private List<ListenerT> toRemove;
    private List<ListenerT> toAdd;
  }
}