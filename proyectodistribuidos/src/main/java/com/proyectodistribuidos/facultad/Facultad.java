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
                        System.out.println(nomFacultad + " enviando mensaje a servidor: " + message);
                        server.send(message, more ? ZMQ.SNDMORE : 0);
                        if (!more) {
                            break;
                        }
                    }
                }

                if (items.pollin(1)) {
                    while (true) {
                        // receive message
                        message = server.recv(0);
                        more = server.hasReceiveMore();

                        System.out.println(nomFacultad + " recibiendo mensaje del servidor: " + message);
                        // Broker it
                        programa.send(message, more ? ZMQ.SNDMORE : 0);
                        if (!more) {
                            break;
                        }
                    }
                }
            }
        }
    }
            
}

