package com.failprooftech.factionschat.update;

/**
 * Factory for {@link FactionsChatUpdateBackend} implementations.
 * <p>
 * Main-line sources do not reference {@code FactionsChatUpdateBackendBundled} by name so the project compiles without
 * PluginUpdateChecker on the classpath. When that class exists in the JAR (profile {@code bundle-update-checker}),
 * it is loaded reflectively; otherwise {@link FactionsChatUpdateBackendNoop} is returned.
 */
public final class FactionsChatUpdateBackends
{

	private static final String BUNDLED_CLASS = "com.failprooftech.factionschat.update.FactionsChatUpdateBackendBundled";

	private FactionsChatUpdateBackends()
	{
	}

	/**
	 * @return {@code FactionsChatUpdateBackendBundled} via reflection if present and castable; otherwise {@link FactionsChatUpdateBackendNoop}
	 */
	public static FactionsChatUpdateBackend create()
	{
		try
		{
			Class<?> c = Class.forName(BUNDLED_CLASS);
			Object instance = c.getDeclaredConstructor().newInstance();
			return (FactionsChatUpdateBackend) instance;
		}
		catch (ReflectiveOperationException | ClassCastException | LinkageError e)
		{
			// Class missing, module issues, or incompatible class - behave as without bundle.
			return new FactionsChatUpdateBackendNoop();
		}
	}

}
