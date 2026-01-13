package com.example.usakogame.runner;

public class RunnerObstacle {
    public double x, y, w, h;
    public boolean isSky;
    
    public RunnerObstacle(double x, double y, double w, double h, boolean sky) {
        this.x = x; 
        this.y = y; 
        this.w = w; 
        this.h = h; 
        this.isSky = sky;
    }
}
