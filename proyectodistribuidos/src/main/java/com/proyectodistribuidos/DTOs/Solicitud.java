package com.proyectodistribuidos.DTOs;

public class Solicitud {

    private int semestre;
    private String nomFacultad;
    private String nomPrograma;
    private int numSalones;
    private int numLaboratorios;


    public Solicitud(int semestre, String nomFacultad, String nomPrograma, int numSalones, int numLaboratorios) {
        this.semestre = semestre;
        this.nomFacultad = nomFacultad;
        this.nomPrograma = nomPrograma;
        this.numSalones = numSalones;
        this.numLaboratorios = numLaboratorios;
    }

    public Solicitud() {
    }

    
    public int getSemestre() {
        return semestre;
    }
    public void setSemestre(int semestre) {
        this.semestre = semestre;
    }
    public String getNomFacultad() {
        return nomFacultad;
    }
    public void setNomFacultad(String nomFacultad) {
        this.nomFacultad = nomFacultad;
    }
    public String getNomPrograma() {
        return nomPrograma;
    }
    public void setNomPrograma(String nomPrograma) {
        this.nomPrograma = nomPrograma;
    }
    public int getNumSalones() {
        return numSalones;
    }
    public void setNumSalones(int numSalones) {
        this.numSalones = numSalones;
    }
    public int getNumLaboratorios() {
        return numLaboratorios;
    }
    public void setNumLaboratorios(int numLaboratorios) {
        this.numLaboratorios = numLaboratorios;
    }

    
}
