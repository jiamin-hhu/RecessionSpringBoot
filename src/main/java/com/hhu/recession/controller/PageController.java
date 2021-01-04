package com.hhu.recession.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PageController {

    @RequestMapping("index")
    public ModelAndView Index(ModelAndView mv){
        mv.setViewName("index");
        return mv;
    }

}
