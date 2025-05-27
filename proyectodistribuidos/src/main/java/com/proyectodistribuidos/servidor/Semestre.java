package com.proyectodistribuidos.servidor;

public class Semestre {

    private int salones;
    private int laboratorios;
    
    public Semestre(int salones, int laboratorios) {
        this.salones = salones;
        this.laboratorios = laboratorios;
    }

    public synchronized int getSalones() {
        return salones;
    }

    public synchronized void setSalones(int salones) {
        this.salones = salones;
    }

    public synchronized int getLaboratorios() {
        return laboratorios;
    }

    public synchronized void setLaboratorios(int laboratorios) {
        this.laboratorios = laboratorios;
    }
}
