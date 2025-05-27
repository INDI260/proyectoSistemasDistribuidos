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
        if (args.length != 3) {
            System.out.println("Uso: java Cliente <semestre> <nomFacultad> <ip:puertoServidor>");
            return;
        }
        //  Prepare our context and sockets
        try (ZContext context = new ZContext()) {
            Socket programa = context.createSocket(SocketType.ROUTER);
            Socket server = context.createSocket(SocketType.DEALER);
            programa.bind("tcp://*:5559");
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
            long startTime = 0;

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
                    try (java.io.FileWriter writer = new java.io.FileWriter(nomFacultad + ".txt", true)) {
                        writer.write(new String(message) + System.lineSeparator());
                    } catch (java.io.IOException e) {
                        System.err.println("Error escribiendo en el archivo: " + e.getMessage());
                    }
                    startTime = System.nanoTime();
                    
                    
                }
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
                    System.out.println(nomFacultad + " recibiendo mensaje del servidor: " + new String(frames.get(frames.size() - 1)));
                    long endTime = System.nanoTime();
                    long responseTime = endTime - startTime;
                    try (java.io.FileWriter csvWriter = new java.io.FileWriter(nomFacultad + ".csv", true)) {
                        csvWriter.write(responseTime + System.lineSeparator());
                    } catch (java.io.IOException e) {
                        System.err.println("Error escribiendo en el archivo CSV: " + e.getMessage());
                }
            }
        }
    }
            
}

