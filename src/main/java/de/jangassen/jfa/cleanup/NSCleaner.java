package de.jangassen.jfa.cleanup;

import de.jangassen.jfa.FoundationCallback;
import de.jangassen.jfa.FoundationCallbackRegistry;
import de.jangassen.jfa.ObjcToJava;
import de.jangassen.jfa.appkit.NSObject;
import de.jangassen.jfa.foundation.Foundation;
import de.jangassen.jfa.foundation.ID;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

@SuppressWarnings("unused")
public final class NSCleaner {

    private static final ReferenceQueue<Object> references = new ReferenceQueue<>();

    static {
        startCleaner();
    }

    private NSCleaner() {
    }

    public static void register(Object obj, NSObject nsObject) {
        new ReferenceWithCleanup(obj, () -> Foundation.cfRelease(ObjcToJava.toID(nsObject)));
    }

    public static void register(Object obj, ID id) {
        new ReferenceWithCleanup(obj, () -> Foundation.invoke(id, "dealloc"));
    }

    public static void register(Object obj, FoundationCallback callback) {
        new ReferenceWithCleanup(obj, () -> FoundationCallbackRegistry.unregister(callback));
    }

    public static void register(Object obj, Runnable runnable) {
        new ReferenceWithCleanup(obj, runnable);
    }

    private static void startCleaner() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    ReferenceWithCleanup ref = (ReferenceWithCleanup) references.remove();
                    ref.cleanUp();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "NSCleaner");
        cleanupThread.setDaemon(true);
        cleanupThread.setPriority(Thread.MIN_PRIORITY);
        cleanupThread.start();
    }

    private static class ReferenceWithCleanup extends WeakReference<Object> {

        private final Runnable callback;

        public ReferenceWithCleanup(Object obj, Runnable callback) {
            super(obj, references);
            this.callback = callback;
        }

        public void cleanUp() {
            callback.run();
        }
    }
}