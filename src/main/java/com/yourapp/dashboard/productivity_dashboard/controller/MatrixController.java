package com.yourapp.dashboard.productivity_dashboard.controller;

import com.yourapp.dashboard.productivity_dashboard.model.MatrixItem;
import com.yourapp.dashboard.productivity_dashboard.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
    public class MatrixController {

        @Autowired
        private TaskService taskService;

        @GetMapping("/matrix")
        public String showMatrix(Model model) {
            Map<String, List<MatrixItem>> matrix = taskService.getMatrixItems();

            model.addAttribute("doFirst", matrix.get("doFirst"));
            model.addAttribute("schedule", matrix.get("schedule"));
            model.addAttribute("delegate", matrix.get("delegate"));
            model.addAttribute("eliminate", matrix.get("eliminate"));

            return "matrix";
        }
    }

