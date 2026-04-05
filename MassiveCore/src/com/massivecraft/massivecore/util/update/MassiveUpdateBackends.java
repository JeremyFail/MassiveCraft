package com.massivecraft.massivecore.util.update;

/**
 * Factory for {@link MassiveUpdateBackend} implementations.
 * <p>
 * Main-line sources do not reference {@code MassiveUpdateBackendBundled} by name so the project compiles without
 * PluginUpdateChecker on the classpath. When that class exists in the JAR (profile {@code bundle-update-checker}),
 * it is loaded reflectively; otherwise {@link MassiveUpdateBackendNoop} is returned.
 */
public final class MassiveUpdateBackends
{

	private static final String BUNDLED_CLASS = "com.massivecraft.massivecore.util.update.MassiveUpdateBackendBundled";

	private MassiveUpdateBackends()
	{
	}

	/**
	 * @return {@code MassiveUpdateBackendBundled} via reflection if present and castable; otherwise {@link MassiveUpdateBackendNoop}
	 */
	public static MassiveUpdateBackend create()
	{
		try
		{
			Class<?> c = Class.forName(BUNDLED_CLASS);
			Object instance = c.getDeclaredConstructor().newInstance();
			return (MassiveUpdateBackend) instance;
		}
		catch (ReflectiveOperationException | ClassCastException | LinkageError e)
		{
			// Class missing, module issues, or incompatible class - behave as without bundle.
			return new MassiveUpdateBackendNoop();
		}
	}

}
