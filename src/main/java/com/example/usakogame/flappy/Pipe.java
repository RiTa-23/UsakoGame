package com.example.usakogame.flappy;

public class Pipe {
    public double x, topHeight;
    public boolean scored = false;
    
    public Pipe(double x, double h) {
        this.x = x;
        this.topHeight = h;
    }
}
