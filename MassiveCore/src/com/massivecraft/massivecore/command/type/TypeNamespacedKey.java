package com.massivecraft.massivecore.command.type;

import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.item.ConverterToNamespacedKey;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Collections;

public class TypeNamespacedKey extends TypeAbstract<NamespacedKey>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final TypeNamespacedKey i = new TypeNamespacedKey();
	public static TypeNamespacedKey get() { return i; }
	public TypeNamespacedKey() { super(NamespacedKey.class); }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public String getVisualInner(NamespacedKey value, CommandSender sender)
	{
		String namespaceColour = "<blue>";
		if (value.getNamespace().equals(NamespacedKey.MINECRAFT)) namespaceColour = "<green>";
		else if (value.getNamespace().equals(NamespacedKey.BUKKIT)) namespaceColour = "<purple>";
		
		return Txt.parse(namespaceColour +"%s<n>:<v>%s",value.getNamespace(),value.getKey());
	}
	
	@Override
	public NamespacedKey read(String arg, CommandSender sender) throws MassiveException
	{
		return ConverterToNamespacedKey.get().convert(arg);
	}
	
	@Override
	public Collection<String> getTabList(CommandSender sender, String arg)
	{
		return Collections.emptySet();
	}

}
