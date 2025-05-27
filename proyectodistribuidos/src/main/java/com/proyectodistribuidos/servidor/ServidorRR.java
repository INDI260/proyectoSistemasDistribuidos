package com.proyectodistribuidos.servidor;

import java.util.List;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;

public class ServidorRR {

    private static List<Semestre> semestres;


    public static void main(String[] args) throws Exception
    {

        if(args.length > 0){
            if(args.length != 2){
                System.out.println("Error: Debe ingresar 2 argumentos: salones laboratorios.");
                return;
            }
            else{
                semestres = new java.util.ArrayList<>();
                int salones = Integer.parseInt(args[0]);
                int laboratorios = Integer.parseInt(args[1]);
                for (int i = 1; i <= 10; i++) {
                    semestres.add(new Semestre(salones, laboratorios));
                }
            }
        }
        else{
            System.out.println("Error: Debe ingresar 2 argumentos: salones laboratorios.");
            return;
        }


        try (ZContext context = new ZContext()) {
            // Socket to talk to clients
            Socket facultades = context.createSocket(SocketType.ROUTER);
            facultades.bind("tcp://*:5555");

            Socket workers = context.createSocket(SocketType.DEALER);
            workers.bind("inproc://workers");
        
            for (int thread_nbr = 0; thread_nbr < 10; thread_nbr++) {
                Thread worker = new WorkerRR(context, semestres);
                worker.setName("Worker-" + thread_nbr);
                worker.start();
            }

            System.out.println("Workers iniciados");

            Poller items = context.createPoller(2);
            items.register(facultades, Poller.POLLIN);
            items.register(workers, Poller.POLLIN);

            byte[] message;
            boolean more;

            while (!Thread.currentThread().isInterrupted()) {
                //  poll and memorize multipart detection
                items.poll();

                if (items.pollin(0)) {
                    while (true) {
                        // receive message
                        message = facultades.recv(0);
                        more = facultades.hasReceiveMore();

                        // Broker it
                        workers.send(message, more ? ZMQ.SNDMORE : 0);
                        if (!more) {
                            break;
                        }
                    }
                    System.out.println("Facultad enviando mensaje a workers: " + new String(message));
                }

                if (items.pollin(1)) {
                    while (true) {
                        // receive message
                        message = workers.recv(0);
                        more = workers.hasReceiveMore();
                        // Broker it
                        facultades.send(message, more ? ZMQ.SNDMORE : 0);
                        if (!more) {
                            break;
                        }
                    }
                }
            }

            ZMQ.proxy(facultades, workers, null);
            
        }
    }
}
