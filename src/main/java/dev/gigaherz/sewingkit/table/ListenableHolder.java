package dev.gigaherz.sewingkit.table;

import com.google.common.collect.Lists;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

public class ListenableHolder
{
    private final List<Reference<? extends SewingTableContainer>> listeners = Lists.newArrayList();
    private final ReferenceQueue<SewingTableContainer> pendingRemovals = new ReferenceQueue<>();

    public void addWeakListener(SewingTableContainer e)
    {
        listeners.add(new WeakReference<>(e, pendingRemovals));
    }

    public void doCallbacks()
    {
        for (Reference<? extends SewingTableContainer>
             ref = pendingRemovals.poll();
             ref != null;
             ref = pendingRemovals.poll())
        {
            listeners.remove(ref);
        }

        for (Iterator<Reference<? extends SewingTableContainer>> iterator = listeners.iterator(); iterator.hasNext(); )
        {
            Reference<? extends SewingTableContainer> reference = iterator.next();
            SewingTableContainer listener = reference.get();
            if (listener == null)
                iterator.remove();
            else
                listener.onInventoryChanged();
        }
    }
}
