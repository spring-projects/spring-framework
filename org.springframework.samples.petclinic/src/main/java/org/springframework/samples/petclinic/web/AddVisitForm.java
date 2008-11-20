package org.springframework.samples.petclinic.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.Clinic;
import org.springframework.samples.petclinic.Pet;
import org.springframework.samples.petclinic.Visit;
import org.springframework.samples.petclinic.validation.VisitValidator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.bind.WebDataBinder;

/**
 * JavaBean form controller that is used to add a new <code>Visit</code> to
 * the system.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 */
@Controller
@RequestMapping("/addVisit.do")
@SessionAttributes("visit")
public class AddVisitForm {

	private final Clinic clinic;

	@Autowired
	public AddVisitForm(Clinic clinic) {
		this.clinic = clinic;
	}

    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields(new String[] {"id"});
    }

	@RequestMapping(method = RequestMethod.GET)
	public String setupForm(@RequestParam("petId") int petId, Model model) {
		Pet pet = this.clinic.loadPet(petId);
		Visit visit = new Visit();
		pet.addVisit(visit);
		model.addAttribute("visit", visit);
		return "visitForm";
	}

	@RequestMapping(method = RequestMethod.POST)
	public String processSubmit(@ModelAttribute("visit") Visit visit, BindingResult result, SessionStatus status) {
		new VisitValidator().validate(visit, result);
		if (result.hasErrors()) {
			return "visitForm";
		}
		else {
			this.clinic.storeVisit(visit);
			status.setComplete();
			return "redirect:owner.do?ownerId=" + visit.getPet().getOwner().getId();
		}
	}

}
