package com.proyectodistribuidos.servidor;

import java.util.List;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.proyectodistribuidos.DTOs.Solicitud;

public class Worker extends Thread {
    
        private ZContext context;
        private List<Semestre> semestres;

        public Worker(ZContext context, List<Semestre> semestres)
        {
            this.context = context;
            this.semestres = semestres;
        }

        @Override
        public void run()
        {
            ZMQ.Socket socket = context.createSocket(SocketType.REP);
            socket.connect("inproc://workers");
            String reply;

            while (true) {

                //  Wait for next request from client (C string)
                String request = socket.recvStr(0);
                System.out.println(Thread.currentThread().getName() + " Received request: [" + request + "]");
                reply = "Se asignaron: ";

                //  Do some 'work'
                try {
                   reply = procesarSolicitud(request, reply);
                }
                catch (InterruptedException e) {
                }

                //  Send reply back to client (C string)
                socket.send(reply, 0);
            }
        }

        public String procesarSolicitud(String request, String reply) throws InterruptedException
        {
            Solicitud solicitud = new Gson().fromJson(request, Solicitud.class);
            Semestre semestre = semestres.get(solicitud.getSemestre() - 1);

            synchronized (semestre) {
                try {
                    reply = cambiarSalones(semestre, solicitud.getNumSalones(),solicitud.getNumLaboratorios(), reply);
                } catch (IllegalStateException e) {
                    System.out.println("Error: " + e.getMessage());
                    reply = "Error: " + e.getMessage();
                    return reply;
                }
            }

            System.out.println("Solicitud procesada con éxito. Salones y laboratorios reservados.");
            return reply;
        }

        public String cambiarSalones(Semestre semestre, int numSalones, int laboratorios, String reply) {
            if (numSalones > semestre.getSalones()) {
                int salones = semestre.getSalones();
                semestre.setSalones(0);
                throw new IllegalStateException("No hay suficientes salones disponibles. Se asignaron " + salones + " salones.");
            } else {
                semestre.setSalones(semestre.getSalones() - numSalones);
                reply += numSalones + " salones";
                reply += cambiarLaboratorios(semestre, laboratorios, reply);
                return reply;
            }
        }

        public String cambiarLaboratorios(Semestre semestre, int numLaboratorios, String reply) {
            if (numLaboratorios > semestre.getLaboratorios()) {
                if (semestre.getSalones() >= numLaboratorios) {
                    reply += ", " + semestre.getLaboratorios() + " laboratorios y " + (numLaboratorios - semestre.getLaboratorios()) + " aulas móviles";
                    semestre.setSalones(semestre.getSalones() - (numLaboratorios - semestre.getLaboratorios()));
                    semestre.setLaboratorios(0);
                    return reply;
                } else {
                    int laboratorios = semestre.getLaboratorios();
                    int aulasMoviles = semestre.getSalones();
                    semestre.setLaboratorios(0);
                    semestre.setSalones(0);
                    throw new IllegalStateException("No hay suficientes laboratorios disponibles. Se asignaron " + laboratorios + " laboratorios y " + aulasMoviles + " aulas móviles.");
                }
            } else {
                reply += " y " + numLaboratorios + " laboratorios";
                semestre.setLaboratorios(semestre.getLaboratorios() - numLaboratorios);
                return reply;
            }
        }
    }