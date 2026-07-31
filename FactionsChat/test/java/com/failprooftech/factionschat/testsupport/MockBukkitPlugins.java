package com.failprooftech.factionschat.testsupport;

import com.failprooftech.factionschat.FactionsChat;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.plugin.MockBukkitConfiguredPluginClassLoader;
import org.mockbukkit.mockbukkit.plugin.PluginManagerMock;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.jar.JarFile;

/**
 * Loads {@link FactionsChat} under MockBukkit (JDK 17+/25 friendly).
 *
 * <p>MockBukkit's {@code load(Class)} injects a ByteBuddy proxy into an empty
 * {@link MockBukkitConfiguredPluginClassLoader}. On newer JDKs that injector is often
 * unavailable, so injection falls back to {@code findClass} and NPEs with
 * {@code No jar file selected}. This helper mirrors MockBukkit's load path but sets the
 * Maven-built plugin JAR on the classloader first, then enables the plugin.</p>
 *
 * <p>When {@code -Dfactionschat.test.pluginJar=} is unset or not a file, falls back to
 * {@link MockBukkit#load(Class)}.</p>
 */
public final class MockBukkitPlugins
{
    static
    {
        // Best-effort: ByteBuddy agent helps MockBukkit proxy injection on some JDKs.
        try
        {
            ByteBuddyAgent.install();
        }
        catch (Throwable ignored)
        {
        }
    }

    private MockBukkitPlugins()
    {
    }

    /**
     * Loads and enables {@link FactionsChat} on the current MockBukkit server.
     *
     * <p>Prefer calling {@link MockBukkit#mock()} first. When a plugin JAR path is configured
     * via system property, uses the reflection-based JAR-backed classloader path; otherwise
     * delegates to {@link MockBukkit#load(Class)}.</p>
     *
     * @return the enabled {@link FactionsChat} plugin instance
     * @throws IllegalStateException if JAR-backed loading fails, or (on the JAR path) if
     *         {@link MockBukkit#mock()} was not called / {@code plugin.yml} is missing
     */
    public static FactionsChat loadFactionsChat()
    {
        File jar = resolvePluginJar();
        if (jar != null)
        {
            try
            {
                return loadWithJarFile(jar);
            }
            catch (Exception e)
            {
                throw new IllegalStateException(
                    "Failed to load FactionsChat via MockBukkit with plugin JAR: " + jar, e);
            }
        }
        return MockBukkit.load(FactionsChat.class);
    }

    /**
     * Replicates MockBukkit's plugin load pipeline while attaching {@code jar} before proxy load.
     *
     * @param jar Maven-built plugin JAR (must exist)
     * @return enabled {@link FactionsChat} instance registered with the mock plugin manager
     * @throws Exception reflection, I/O, or plugin-construction failures from MockBukkit internals
     * @throws IllegalStateException if no mock server is active or {@code plugin.yml} is absent
     */
    private static FactionsChat loadWithJarFile(File jar) throws Exception
    {
        ServerMock server = MockBukkit.getMock();
        if (server == null)
        {
            throw new IllegalStateException("Call MockBukkit.mock() before loadFactionsChat()");
        }
        PluginManagerMock pluginManager = (PluginManagerMock) server.getPluginManager();

        PluginDescriptionFile description;
        try (InputStream in = FactionsChat.class.getClassLoader().getResourceAsStream("plugin.yml"))
        {
            if (in == null)
            {
                throw new IllegalStateException("plugin.yml missing from test classpath");
            }
            description = new PluginDescriptionFile(in);
        }

        // Package-private MockBukkit APIs — setAccessible required across JDK versions.
        Method createClassLoader = PluginManagerMock.class.getDeclaredMethod(
            "createClassLoader", PluginDescriptionFile.class);
        createClassLoader.setAccessible(true);
        MockBukkitConfiguredPluginClassLoader classLoader =
            (MockBukkitConfiguredPluginClassLoader) createClassLoader.invoke(pluginManager, description);

        // Avoid "No jar file selected" when ByteBuddy injection falls back to findClass.
        classLoader.setJarFile(new JarFile(jar));

        @SuppressWarnings("unchecked")
        Class<? extends Plugin> proxyClass =
            (Class<? extends Plugin>) classLoader.loadProxyClass(FactionsChat.class);

        Constructor<? extends Plugin> ctor = proxyClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Plugin plugin = ctor.newInstance();

        Method registerLoadedPlugin = PluginManagerMock.class.getDeclaredMethod(
            "registerLoadedPlugin", Plugin.class);
        registerLoadedPlugin.setAccessible(true);
        registerLoadedPlugin.invoke(pluginManager, plugin);

        pluginManager.enablePlugin(plugin);
        return (FactionsChat) plugin;
    }

    /**
     * Resolves the optional Maven surefire/failsafe plugin JAR path.
     *
     * @return the JAR {@link File} when {@code factionschat.test.pluginJar} points at an existing
     *         file; otherwise {@code null} to trigger the {@link MockBukkit#load(Class)} fallback
     */
    private static File resolvePluginJar()
    {
        String path = System.getProperty("factionschat.test.pluginJar");
        if (path == null || path.isBlank())
        {
            return null;
        }
        File jar = new File(path);
        return jar.isFile() ? jar : null;
    }
}
