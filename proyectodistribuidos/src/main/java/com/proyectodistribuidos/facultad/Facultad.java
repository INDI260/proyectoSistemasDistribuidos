package com.proyectodistribuidos.facultad;

import org.zeromq.SocketType;
import org.zeromq.ZContext;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;

public class Facultad {

    private static String nomFacultad;
    private static int semestre;
    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Uso: java Cliente <semestre> <nomFacultad> <ip:puertoServidor> <puertoFacultad>");
            return;
        }
        //  Prepare our context and sockets
        try (ZContext context = new ZContext()) {
            Socket programa = context.createSocket(SocketType.ROUTER);
            Socket server = context.createSocket(SocketType.DEALER);
            programa.bind("tcp://*:" + args[3]); // Bind to the specified port for the faculty
            server.connect("tcp://" + args[2]);
            nomFacultad = args[1];
            semestre = Integer.parseInt(args[0]);

            System.out.println("launch and connect broker.");

            //  Initialize poll set
            Poller items = context.createPoller(2);
            items.register(programa, Poller.POLLIN);
            items.register(server, Poller.POLLIN);

            boolean more = false;
            byte[] message;

            //  Switch messages between sockets
            while (!Thread.currentThread().isInterrupted()) {
                //  poll and memorize multipart detection
                items.poll();

                if (items.pollin(0)) {
                    while (true) {
                        // receive message
                        message = programa.recv(0);
                        more = programa.hasReceiveMore();

                        // Broker it
                        server.send(message, more ? ZMQ.SNDMORE : 0);
                        if (!more) {
                            break;
                        }
                    }
                    System.out.println(nomFacultad + " enviando mensaje a servidor: " + new String(message));
                }

                if (items.pollin(1)) {
                    java.util.List<byte[]> frames = new java.util.ArrayList<>();
                    while (true) {
                        // receive message
                        message = server.recv(0);
                        frames.add(message);
                        more = server.hasReceiveMore();
                        if (!more) {
                            break;
                        }
                    }
                    for (int i = 0; i < frames.size(); i++) {
                        programa.send(frames.get(i), (i < frames.size() - 1) ? ZMQ.SNDMORE : 0);
                    }
                    String respuesta = (nomFacultad + " recibiendo mensaje del servidor: " + new String(frames.get(frames.size() - 1)));
                    System.out.println(respuesta);
                    try (java.io.FileWriter writer = new java.io.FileWriter(nomFacultad + ".txt", true)) {
                        writer.write(respuesta + System.lineSeparator());
                    } catch (java.io.IOException e) {
                        System.err.println("Error al escribir en el archivo: " + e.getMessage());
                    }
                }
            }
        }
    }
            
}

