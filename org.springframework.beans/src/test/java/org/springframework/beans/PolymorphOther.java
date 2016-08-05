package org.springframework.beans;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


/**
 * Let's say we have an apartment complex, and we're collecting
 * information on a potential or existing resident's pets.
 * All pets are allowed, but depending on type, we need to collect
 * some information.
 * We'd like to store these in the resident's pet collection regardless
 * of the type of the pet.
 * However, we'd also like our form, which dynamically changes depending
 * on the pet type being added, to be able to bind to the correct pet type
 * rather than the high level Pet (which is an interface)...
 * In this example:
 * If the resident has a cat, we need to know if it is fuzzy
 * 	(for purposes of replacing carpets?)
 * If the resident has a dog, we need to know the bark decibels
 * 	(to place it away from noise-sensitive neighbors)
 * If the resident's dog is a pitbull, we also need to know if it is aggressive
 *  (to keep it away from children where applicable)
 * Again, these questions will be shaped on the form... 
 * but we need to bind the type dynamically.
 * Let's look at how BeanWrapper would handle these cases.  
 */
public class PolymorphOther {
	

	@Test
	public void testBasicWorkingCats() {
		// Let's start with the overly specific resident.
		OverlySpecificResident osResident = new OverlySpecificResident();
		BeanWrapper beanWrapper = new BeanWrapperImpl(osResident);
		beanWrapper.setAutoGrowNestedPaths(true);
		
		// This will work fine...
		beanWrapper.setPropertyValue("cats[0].name", "Mr. Scruffles");
		beanWrapper.setPropertyValue("cats[0].fuzzy", true);
		assertEquals("Mr. Scruffles", osResident.getCats().get(0).getName());
		assertTrue(osResident.getCats().get(0).getFuzzy());
	}
	
	@Test
	public void testBasicWorkingDogs() {
		// Let's start with the overly specific resident.
		OverlySpecificResident osResident = new OverlySpecificResident();
		BeanWrapper beanWrapper = new BeanWrapperImpl(osResident);
		beanWrapper.setAutoGrowNestedPaths(true);
		
		// This will also work fine...
		beanWrapper.setPropertyValue("dogs[0].name", "The Fonz");
		beanWrapper.setPropertyValue("dogs[0].barkDecibels", 120);
		assertEquals("The Fonz", osResident.getDogs().get(0).getName());
		assertEquals(120, osResident.getDogs().get(0).getBarkDecibels());
	}
	
	@Test
	public void testUseDogSubclass() {
		// Let's start with the overly specific resident.
		OverlySpecificResident osResident = new OverlySpecificResident();
		BeanWrapper beanWrapper = new BeanWrapperImpl(osResident);
		beanWrapper.setAutoGrowNestedPaths(true);
		
		// We want this to work, where the dog is a pitbull.
		// We could theoretically indicate a canonical cast in the path
		//  that's read by the bean wrapper. It would be neat if the beanWrapper
		//  could read the cast and create the object based on the indicated type...
		try {
			beanWrapper.setPropertyValue("(org.springframework.beans.Pitbull)dogs[0].aggro", true);
		} catch (Exception nwpe) {
			nwpe.printStackTrace();
			fail("I couldn't assign a pitbull-specific field to my dog class :(");
		}
		assertTrue(((Pitbull) osResident.getDogs().get(0)).getAggro());
	}
	
	@Test
	public void testWouldBeNice() {
		
		// Would be great when working with interfaces as well!
		BeanWrapper beanWrapper = new BeanWrapperImpl(new Resident());
		beanWrapper.setAutoGrowNestedPaths(true);
		try {
			beanWrapper.setPropertyValue("(org.springframework.beans.Cat)pets[0].name", "Mr.Scruffles");
			beanWrapper.setPropertyValue("(org.springframework.beans.Cat)pets[0].fuzzy", true);
			beanWrapper.setPropertyValue("(org.springframework.beans.Pitbull)pets[1].name", "The Fonz");
			beanWrapper.setPropertyValue("(org.springframework.beans.Pitbull)pets[1].barkDecibels", 120);
			beanWrapper.setPropertyValue("(org.springframework.beans.Pitbull)pets[1].aggro", true);
		} catch (Exception nwpe) {
			nwpe.printStackTrace();
			fail("No can has interfaces :(");
		}
	}
	
	@Test
	public void testNonCollectionPath()
	{
		Case aCase = new Case();
		BeanWrapper beanWrapper = new BeanWrapperImpl(aCase);
		beanWrapper.setAutoGrowNestedPaths(true);
		beanWrapper.setPropertyValue("party[0].(org.springframework.beans.Pitbull)pet.name", "Mr. Scruffles");
		beanWrapper.setPropertyValue("party[0].(org.springframework.beans.Pitbull)pet.aggro",true);
		assertTrue(((Pitbull)aCase.getParty().get(0).getPet()).getAggro());
	}
	
	@Test
	public void testDoublePolymorphicGet()
	{
		//This case tends to fail in cases where the user is preparing to conditionally bind
		//one of two or more sub-classes to a path which references their parent class/iterface.
		Resident resident = new Resident();
		BeanWrapper beanWrapper = new BeanWrapperImpl(resident);
		beanWrapper.setAutoGrowNestedPaths(true);
		beanWrapper.getPropertyValue("(org.springframework.beans.Pitbull)mainPet.aggro");
		beanWrapper.getPropertyValue("(org.springframework.beans.Cat)mainPet.fuzzy");
		beanWrapper.setPropertyValue("(org.springframework.beans.Pitbull)mainPet.aggro", true);
		assertTrue(((Pitbull)resident.getMainPet()).getAggro());
		
	}
	
	@Test
	public void testDoublePolymorphicGetOnList()
	{
		Resident resident = new Resident();
		BeanWrapper beanWrapper = new BeanWrapperImpl(resident);
		beanWrapper.setAutoGrowNestedPaths(true);
		beanWrapper.getPropertyValue("(org.springframework.beans.Pitbull)pets[0].aggro");
		beanWrapper.getPropertyValue("(org.springframework.beans.Cat)pets[0].fuzzy");
		beanWrapper.setPropertyValue("(org.springframework.beans.Pitbull)pets[0].aggro", true);
		assertTrue(((Pitbull)resident.getPets().get(0)).getAggro());
	}
}

class Case {
	private List<InvolvedParty> party;

	public void setParty(List<InvolvedParty> party) {
		this.party = party;
	}

	public List<InvolvedParty> getParty() {
		return party;
	}
}

class InvolvedParty {
	private Dog pet;

	public void setPet(Dog pet) {
		this.pet = pet;
	}

	public Dog getPet() {
		return pet;
	}


}

/**
 * Would like to use this class...
 */
class Resident {
	private Pet mainPet;
	
	public Pet getMainPet() {
		return mainPet;
	}
	public void setMainPet(Pet mainPet) {
		this.mainPet = mainPet;
	}
	private List<Pet> pets = new ArrayList<Pet>();
	public List<Pet> getPets() {
		return pets;
	}
	public void setPets(List<Pet> pets) {
		this.pets = pets;
	}
}

/**
 * End up using this one but it doesn't work either.
 * @author cduplichien
 *
 */
class OverlySpecificResident {
	private List<Cat> cats = new ArrayList<Cat>();
	private List<Dog> dogs = new ArrayList<Dog>();
	public List<Cat> getCats() {
		return cats;
	}
	public void setCats(List<Cat> cats) {
		this.cats = cats;
	}
	public List<Dog> getDogs() {
		return dogs;
	}
	public void setDogs(List<Dog> dogs) {
		this.dogs = dogs;
	}
}

interface Pet {
	public String getName();		
}

class Cat implements Pet {
	private String name;
	private boolean fuzzy;
	
	public String getName() {
		return this.name;
	}
	public void setName(String name) {
		this.name = name; 
	}
	public void setFuzzy(boolean fuzzy) {
		this.fuzzy = fuzzy;
	}
	public boolean getFuzzy() {
		return fuzzy;
	}

} 

class Dog implements Pet {
	private String name;
	private int barkDecibels;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getBarkDecibels() {
		return barkDecibels;
	}
	public void setBarkDecibels(int barkDecibels) {
		this.barkDecibels = barkDecibels;
	}
}

class Pitbull extends Dog {
	private boolean aggro;

	public boolean getAggro() {
		return aggro;
	}
	public void setAggro(boolean aggro) {
		this.aggro = aggro;
	}
}