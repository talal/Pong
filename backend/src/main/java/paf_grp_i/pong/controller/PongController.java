package paf_grp_i.pong.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import paf_grp_i.pong.model.User;
import paf_grp_i.pong.repository.UserRepository;

@Controller
public class PongController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/")
    public String showHomepage() {
        return "index";
    }

    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        model.addAttribute("user", new User());
        return "signup_form";
    }

    @PostMapping("/process_signup")
    public String processSignup(User user) {

        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        userRepo.save(user);
        return "signup_success";
    }

    @GetMapping("/login")
    public String login() {
        //check if user is already logged in
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        //if user is already logged in redirect to menu
        if (authentication != null && authentication.isAuthenticated()
        		&& !(authentication instanceof AnonymousAuthenticationToken) ) {
            return "redirect:/?alreadyLoggedIn";
        }
//        return "login"; 	//if not authenticated, show custom login page
        return "login_jwt";	//now use new login page that also supports JWT
    }

    @ResponseBody
	@GetMapping("/hello")
	public String hello() {
		return "<h1>Hello World!</h1><a href='/'>Return to Menu</a>";
	}

	@GetMapping("/chat")
	public String chat() {
		return "chat";
	}


	@GetMapping("/hello-jwt")
	public String helloJwt() {
		return "hello-jwt";
	}

	@GetMapping("/chat-jwt")
	public String chatJwt() {
		return "chat-jwt";
	}

}
