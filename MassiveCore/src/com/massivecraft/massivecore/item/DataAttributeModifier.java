package com.massivecraft.massivecore.item;

import com.massivecraft.massivecore.command.editor.annotation.EditorMethods;
import com.massivecraft.massivecore.comparator.ComparatorSmart;
import com.massivecraft.massivecore.util.MUtil;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Objects;
import java.util.UUID;

import static com.massivecraft.massivecore.item.DataItemStack.get;
import static com.massivecraft.massivecore.item.DataItemStack.set;

@EditorMethods(true)
public class DataAttributeModifier implements Comparable<DataAttributeModifier>
{
	// -------------------------------------------- //
	// DEFAULTS
	// -------------------------------------------- //
	
	public static final transient Attribute DEFAULT_ATTRIBUTE = Attribute.MAX_HEALTH;
	public static final transient String DEFAULT_NAME = "Modifier";
	public static final transient double DEFAULT_AMOUNT = 0;
	public static final transient Operation DEFAULT_OPERATION = Operation.ADD_NUMBER;
	public static final transient EquipmentSlot DEFAULT_SLOT = null;
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private Attribute attribute;
	public Attribute getAttribute() { return get(this.attribute, DEFAULT_ATTRIBUTE); }
	public DataAttributeModifier setAttribute(Attribute attribute) { this.attribute = set(attribute, DEFAULT_ATTRIBUTE); return this; }
	
	private UUID uniqueId;
	public UUID getUniqueId() { return get(this.uniqueId, UUID.randomUUID()); }
	public DataAttributeModifier setUniqueId(UUID uuid) { this.uniqueId = uuid; return this; }
	
	private String name;
	public String getName() { return get(this.name, DEFAULT_NAME); }
	public DataAttributeModifier setName(String name) { this.name = set(name, DEFAULT_NAME); return this; }
	
	private Double amount = null;
	public double getAmount() { return get(this.amount, DEFAULT_AMOUNT); }
	public DataAttributeModifier setAmount(double amount) { this.amount = set(amount, DEFAULT_AMOUNT); return this; }
	
	private Operation operation = null;
	public Operation getOperation() { return get(this.operation, DEFAULT_OPERATION); }
	public DataAttributeModifier setOperation(Operation operation) { this.operation = set(operation, DEFAULT_OPERATION); return this; }
	
	private EquipmentSlot slot = null;
	public EquipmentSlot getSlot() { return get(this.slot, DEFAULT_SLOT); }
	public DataAttributeModifier setSlot(EquipmentSlot slot) { this.slot = set(slot, DEFAULT_SLOT); return this; }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public DataAttributeModifier()
	{
	
	}
	
	public DataAttributeModifier(AttributeModifier attributeModifier)
	{
		this.write(attributeModifier, false);
	}
	
	// -------------------------------------------- //
	// WRITE
	// -------------------------------------------- //
	
	public void write(AttributeModifier attributeModifier, boolean a2b)
	{
		WriterAttributeModifier.get().write(this, attributeModifier, a2b);
	}
	
	// -------------------------------------------- //
	// TO BUKKIT
	// -------------------------------------------- //
	
	public AttributeModifier toBukkit()
	{
		// Create
		AttributeModifier ret = WriterAttributeModifier.get().createOB();
		
		// Fill
		this.write(ret, true);
		
		// Return
		return ret;
	}
	
	// -------------------------------------------- //
	// COMPARE & EQUALS & HASHCODE 
	// -------------------------------------------- //
	
	@Override
	public int compareTo(DataAttributeModifier that)
	{
		return ComparatorSmart.get().compare(
			this.getAttribute(), that.getAttribute(),
			this.getUniqueId(), that.getUniqueId(),
			this.getName(), that.getName(),
			this.getAmount(), that.getAmount(),
			this.getOperation(), that.getOperation(),
			this.getSlot(), that.getSlot()
		);
	}
	
	// TODO: Use compare instead to avoid bugs?
	@Override
	public boolean equals(Object object)
	{
		if (!(object instanceof DataAttributeModifier)) return false;
        DataAttributeModifier that = (DataAttributeModifier) object;
		
		return MUtil.equals(
			this.getAttribute(), that.getAttribute(),
			this.getUniqueId(), that.getUniqueId(),
			this.getName(), that.getName(),
			this.getAmount(), that.getAmount(),
			this.getOperation(), that.getOperation(),
			this.getSlot(), that.getSlot()
		);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(
			this.getAttribute(),
			this.getUniqueId(),
			this.getName(),
			this.getAmount(),
			this.getOperation(),
			this.getSlot()
		);
	}
	
}
