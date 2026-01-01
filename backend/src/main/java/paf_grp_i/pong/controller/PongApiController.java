package paf_grp_i.pong.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PongApiController {

    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        return "<h1>Hello World!</h1><a href='/'>Return to Menu</a>";
    }

    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }
}
