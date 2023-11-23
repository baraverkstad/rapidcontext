package org.rapidcontext.app.ui;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * A Mac application wrapper. This class is used to avoid a direct
 * dependency on the Apple-specific AWT classes used on the Mac.
 * Instead, all calls to this class are forwarded via reflection to
 * the real application handler. This avoids ClassNotFoundException
 * when loading this class on other environments.
 *
 * @author   Per Cederberg
 * @version  1.0
 */
public class MacApplication {

    /**
     * The Apple application class name.
     */
    private static final String APP_CLASS = "com.apple.eawt.Application";

    /**
     * The Apple about handler class name.
     */
    private static final String ABOUT_CLASS = "com.apple.eawt.AboutHandler";

    /**
     * The Apple preferences handler class name.
     */
    private static final String PREFS_CLASS = "com.apple.eawt.PreferencesHandler";

    /**
     * The one and only instance.
     */
    private static MacApplication instance = null;

    /**
     * The com.apple.eawt.Application instance object, or null if not
     * found (i.e. not running on Mac).
     */
    private Object app = null;

    /**
     * Returns the Mac application wrapper instance.
     *
     * @return the Mac application wrapper instance
     *
     * @throws Exception if the Apple-specific classes weren't found
     */
    public static MacApplication get() throws Exception {
        if (instance == null) {
            instance = new MacApplication();
        }
        return instance;
    }

    /**
     * Creates a new Mac app helper.
     *
     * @throws Exception if the Apple-specific classes couldn't be found
     */
    private MacApplication() throws Exception {
        Class<?> cls = Class.forName(APP_CLASS);
        app = call(cls, "getApplication");
    }

    /**
     * Sets the dock icon image for the application.
     *
     * @param image          the image to use
     *
     * @throws Exception if the image couldn't be added
     */
    public void setDockIconImage(Image image) throws Exception {
        call(app, "setDockIconImage", image);
    }

    /**
     * Sets an about handler for the application.
     *
     * @param listener       the handler to use, or null for default
     *
     * @throws Exception if the handler couldn't be added
     */
    public void setAboutHandler(ActionListener listener) throws Exception {
        Handler  handler = null;

        if (listener != null) {
            handler = new Handler(this, "handleAbout", listener);
        }
        call(app, "setAboutHandler", proxy(ABOUT_CLASS, handler));
    }

    /**
     * Sets a preferences handler for the application.
     *
     * @param listener       the handler to use, or null for none
     *
     * @throws Exception if the handler couldn't be added
     */
    public void setPreferencesHandler(ActionListener listener) throws Exception {
        Handler  handler = null;

        if (listener != null) {
            handler = new Handler(this, "handlePreferences", listener);
        }
        call(app, "setPreferencesHandler", proxy(PREFS_CLASS, handler));
    }

    /**
     * Finds a method with a specified name.
     *
     * @param cls            the class to search
     * @param name           the method name (must be unique)
     *
     * @return the method found, or
     *         null if not found
     */
    private Method find(Class<?> cls, String name) {
        for (Method m : cls.getMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }

    /**
     * Calls a static class method without arguments.
     *
     * @param cls            the class object
     * @param name           the method name (must be unique)
     *
     * @return the call result, or
     *         null if none was provided
     *
     * @throws RuntimeException if a runtime error occurred
     * @throws ReflectiveOperationException if a reflection error occurred
     */
    private Object call(Class<?> cls, String name)
    throws ReflectiveOperationException, RuntimeException {
        return find(cls, name).invoke(null, new Object[] {});
    }

    /**
     * Calls a method with a single argument.
     *
     * @param obj            the object instance
     * @param name           the method name (must be unique)
     * @param arg            the argument value
     *
     * @return the call result, or
     *         null if none was provided
     *
     * @throws RuntimeException if a runtime error occurred
     * @throws ReflectiveOperationException if a reflection error occurred
     */
    private Object call(Object obj, String name, Object arg)
    throws ReflectiveOperationException, RuntimeException {
        return find(obj.getClass(), name).invoke(obj, new Object[] { arg });
    }

    /**
     * Creates a proxy object for the specified class. The calls
     * will be delegated to the local invoke() method.
     *
     * @param className      the interface class name
     * @param handler        the handler to receive calls
     *
     * @return the proxy object to use in reflection calls
     *
     * @throws ClassNotFoundException if the class couldn't be found
     */
    private Object proxy(String className, Handler handler)
        throws ClassNotFoundException {

        ClassLoader loader = getClass().getClassLoader();
        Class<?> cls = Class.forName(className);
        return Proxy.newProxyInstance(loader, new Class<?>[] { cls }, handler);
    }


    /**
     * A simple handler wrapper.
     */
    private static class Handler implements InvocationHandler {

        /**
         * The reported event source.
         */
        private Object source;

        /**
         * The handler method name to accept, or null for any.
         */
        private String methodName;

        /**
         * The action listener to inform on calls.
         */
        private ActionListener listener;

        /**
         * Creates a new handler.
         *
         * @param source         the event source
         * @param methodName     the handler method name, or null
         * @param listener       the listener to call
         */
        public Handler(Object source, String methodName, ActionListener listener) {
            this.source = source;
            this.methodName = methodName;
            this.listener = listener;
        }

        /**
         * Handles calls on registered listener interfaces.
         *
         * @param p              the proxy object
         * @param m              the method being called
         * @param args           the call arguments
         *
         * @return the call response
         *
         * @throws Exception if an error occurred
         */
        @Override
        public Object invoke(Object p, Method m, Object[] args) throws Exception {
            if (methodName == null || m.getName().equals(methodName)) {
                listener.actionPerformed(new ActionEvent(source,
                                                         ActionEvent.ACTION_PERFORMED,
                                                         m.getName()));
            }
            return null;
        }
    }
}
