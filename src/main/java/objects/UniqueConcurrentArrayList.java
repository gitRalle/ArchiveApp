package objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UniqueConcurrentArrayList<E> extends ArrayList<E> {
    private Set<E> set = Collections.newSetFromMap(new ConcurrentHashMap<>(2500));

    @Override
    public synchronized boolean add(E e) {
        if (set.contains(e)) {
            return false;
        }
        set.add(e);
        return super.add(e);
    }

    @Override
    public synchronized boolean contains(Object o) {
        return set.contains(o);
    }

    @Override
    public synchronized int size() {
        return super.size();
    }
}
