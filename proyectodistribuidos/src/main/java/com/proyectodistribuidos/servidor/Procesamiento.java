package com.proyectodistribuidos.servidor;

import org.zeromq.ZMQ;

import com.proyectodistribuidos.DTOs.Solicitud;

public class Procesamiento implements Runnable {
    private int salones = 380;
    private int laboratorios = 60;
    private int aulasMoviles = 20;
    private Solicitud solicitud;
    private String response;
    private ZMQ.Socket socket;

    @Override
    public void run() {
        procesarSolicitud();
    }

    
    public void procesarSolicitud() {
       try{
        cambiarSalones();
        cambiarLaboratorios();
        }catch (IllegalStateException e){
            System.out.println("Error: " + e.getMessage());
            response = e.getMessage();
            socket.send(response.getBytes(ZMQ.CHARSET), 0);
            return;
        }
        response = "Solicitud procesada con éxito. Salones y laboratorios reservados.";
        socket.send(response.getBytes(ZMQ.CHARSET), 0);
        System.out.println("Salones disponibles: " + salones);
        System.out.println("Laboratorios disponibles: " + laboratorios);
    }

    public synchronized void cambiarSalones() {
        if (solicitud.getNumSalones() > salones) {
            throw new IllegalStateException("No hay suficientes salones disponibles.");
        } else {
            salones -= solicitud.getNumSalones();
        }
    }

    public synchronized void cambiarLaboratorios() {
        if (solicitud.getNumLaboratorios() > laboratorios) {
            if(salones >= solicitud.getNumLaboratorios() && aulasMoviles >= solicitud.getNumLaboratorios()){
                salones -= (solicitud.getNumLaboratorios()-laboratorios);
                aulasMoviles -= (solicitud.getNumLaboratorios() - laboratorios);
                laboratorios = 0;
            }
            else{
                salones -= aulasMoviles;
                aulasMoviles = 0;
                throw new IllegalStateException("No hay suficientes laboratorios disponibles.");
            }
        } else {
            laboratorios -= solicitud.getNumLaboratorios();
        }
    }

    public void setSolicitud(Solicitud solicitud) {
        this.solicitud = solicitud;
    }
    public int getSalones() {
        return salones;
    }
    public String getResponse() {
        return response;
    }

    public int getLaboratorios() {
        return laboratorios;
    }

    public int setSalones(int salones) {
        this.salones = salones;
        return salones;
    }
    public int setLaboratorios(int laboratorios) {
        this.laboratorios = laboratorios;
        return laboratorios;
    }
    public int setAulasMoviles(int aulasMoviles) {
        this.aulasMoviles = aulasMoviles;
        return aulasMoviles;
    }

    public void setSocket(ZMQ.Socket socket) {
        this.socket = socket;
    }

}