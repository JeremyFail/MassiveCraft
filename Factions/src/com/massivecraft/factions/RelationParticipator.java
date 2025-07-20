package com.massivecraft.factions;

import org.bukkit.ChatColor;


public interface RelationParticipator
{
	/**
	 * Describes the RelationParticipator to the observer.
	 *
	 * @param observer the object spectating.
	 * @return a description of the RelationParticipator to the observer.
	 * This assumes lowercase first.
	 * e.g your faction, you, etc.
	 */
	String describeTo(RelationParticipator observer);

	/**
	 * Describes the RelationParticipator to the observer.
	 *
	 * @param observer the object spectating.
	 * @param ucfirst  first character uppercase?
	 * @return a description of the RelationParticipator to the observer.
	 * e.g Your faction, You, Your kingdom etc.
	 */
	String describeTo(RelationParticipator observer, boolean ucfirst);
	
	/**
	 * Gets the relation to the RelationParticipator.
	 *
	 * @param observer to get the relation to.
	 * @return the relation between the 2 RelationParticipators.
	 */
	Rel getRelationTo(RelationParticipator observer);

	/**
	 * Gets the relation to the RelationParticipator.
	 * If one or both RelationParticipators are a faction,
	 * and if ignorePeaceful is true, will get true relation without
	 * returning truced if one of the factions have their peaceful
	 * flag set.
	 *
	 * @param observer       to get the relation to.
	 * @param ignorePeaceful if one or both are factions, get true relation.
	 * @return the relation between the 2 RelationParticipators.
	 */
	Rel getRelationTo(RelationParticipator observer, boolean ignorePeaceful);
	
	/**
	 * Gets the corresponding color to a relation status
	 * between 2 relation participators.
	 *
	 * @param observer the relation participator to get the color from.
	 * @return the corresponding chat color for the relation status
	 * between the 2 relation participators
	 */
	ChatColor getColorTo(RelationParticipator observer);
}
