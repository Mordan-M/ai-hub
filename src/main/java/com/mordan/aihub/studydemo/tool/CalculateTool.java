package com.mordan.aihub.studydemo.tool;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * @className: CalculateTool
 * @description: 计算工具
 * @author: 91002183
 * @date: 2026/3/13
 **/
@Component
public class CalculateTool {

    @Tool("Sums 2 given numbers")
    double sum(double a, double b) {
        return a + b;
    }

    @Tool("Returns a square root of a given number")
    double squareRoot(double x) {
        return Math.sqrt(x);
    }

}
