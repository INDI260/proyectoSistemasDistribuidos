package com.proyectodistribuidos.servidor;

import java.util.List;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZThread;
import org.zeromq.ZMsg;
import org.zeromq.ZFrame;

import com.google.gson.Gson;
import com.proyectodistribuidos.DTOs.Solicitud;

public class WorkerLB extends Thread {
    
        private ZContext context;
        private List<Semestre> semestres;

        public WorkerLB(ZContext context, List<Semestre> semestres)
        {
            this.context = context;
            this.semestres = semestres;
        }

        @Override
        public void run() {
            //  Prepare our context and sockets
            try  {
                Socket worker = context.createSocket(SocketType.REQ);
                // Set a printable identity manually if needed
                worker.setIdentity(Thread.currentThread().getName().getBytes(ZMQ.CHARSET));
                worker.connect("inproc://workers");

                worker.send(new byte[]{1}, 0);

                String reply;

            while (true) {
                // Recibe todos los frames del mensaje
                ZMsg msg = ZMsg.recvMsg(worker);
                if (msg == null) break;

                // Normalmente: [empty][JSON]
                // Si hay más frames, el último suele ser el JSON
                ZFrame lastFrame = null;
                for (ZFrame frame : msg) {
                    lastFrame = frame;
                }
                String request = lastFrame.getString(ZMQ.CHARSET);

                System.out.println(Thread.currentThread().getName() + " Received request: [" + request + "]");
                reply = "Se asignaron: ";

                //  Do some 'work'
                try {
                    reply = procesarSolicitud(request, reply);
                } catch (InterruptedException e) {
                }

                // Responde con un solo frame (el reply)
                ZMsg response = new ZMsg();
                // Solo reenvía los dos primeros frames (dirección y vacío)
                int i = 0;
                for (ZFrame frame : msg) {
                    if (i < 2) {
                        response.add(frame.duplicate());
                    }
                    i++;
                }
                response.addString(reply);
                System.out.println(Thread.currentThread().getName() + " Sending reply: [" + reply + "]");
                System.out.println("Frames que se envían al backend:");
                for (ZFrame frame : response) {
                    System.out.println("  Frame: " + frame.getString(ZMQ.CHARSET));
                }
                response.send(worker);

                msg.destroy();
                }
            }
            catch (Exception e) {
                System.err.println("Error en Worker: " + e.getMessage());
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