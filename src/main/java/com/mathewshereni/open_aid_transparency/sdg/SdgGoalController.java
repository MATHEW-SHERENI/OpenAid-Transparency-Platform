package com.mathewshereni.open_aid_transparency.sdg;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

 // Web layer for SDG goals. Turns HTTP requests into service calls and returns

 // @RestController  = this class handles web requests and returns data (not views).

@RestController
@RequestMapping("/api/sdg-goals")
@RequiredArgsConstructor
public class SdgGoalController {

    private final SdgGoalService service;

    // GET /api/sdg-goals  -> list of all 17 goals.
    @GetMapping
    public List<SdgGoalDto> getAll() {
        return service.findAll();
    }

    // GET /api/sdg-goals/6  -> the goal whose number is 6.
    @GetMapping("/{goalNumber}")
    public SdgGoalDto getByNumber(@PathVariable Integer goalNumber) {
        return service.findByGoalNumber(goalNumber);
    }
}
