package com.proyectodistribuidos.servidor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;
import org.zeromq.ZThread;

public class ServidorLB {

    private static final int NBR_WORKERS  = 10;
    private static byte[]    WORKER_READY = { '\001' };
    private static java.util.List<Semestre> semestres;


    public static void main(String[] args) {
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
        //  Prepare our context and sockets
        try (ZContext ctx = new ZContext()) {
            Socket facultades = ctx.createSocket(SocketType.ROUTER);
            Socket backend = ctx.createSocket(SocketType.ROUTER);
            facultades.bind("tcp://*:5555"); //  For clients
            backend.bind("inproc://workers"); //  For workers

            for (int workerNbr = 0; workerNbr < NBR_WORKERS; workerNbr++)
                new WorkerLB(ctx, semestres).start();

            //  Queue of available workers
            ArrayList<ZFrame> workers = new ArrayList<ZFrame>();

            Poller poller = ctx.createPoller(2);
            poller.register(backend, Poller.POLLIN);
            poller.register(facultades, Poller.POLLIN);

            //  The body of this example is exactly the same as lruqueue2.
            while (true) {
                boolean workersAvailable = workers.size() > 0;
                int rc = poller.poll(-1);

                //  Poll facultades only if we have available workers
                if (rc == -1)
                    break; //  Interrupted

                //  Handle worker activity on backend
                if (poller.pollin(0)) {
                    //  Use worker address for LRU routing
                    ZMsg msg = ZMsg.recvMsg(backend);
                    if (msg == null)
                        break; //  Interrupted
                    ZFrame address = msg.unwrap();
                    workers.add(address);

                    //  Forward message to client if it's not a READY
                    ZFrame frame = msg.getFirst();
                    if (Arrays.equals(frame.getData(), WORKER_READY))
                        msg.destroy();
                    else {
                        // Elimina la dirección del cliente (primer frame)
                        ZFrame clientAddr = msg.pop();
                        ZFrame empty = msg.pop();
                        ZFrame reply = msg.pop();

                        System.out.println("ServidorLB: Enviando respuesta a la facultad:");
                        System.out.println("  clientAddr: " + (clientAddr != null ? clientAddr.toString() : "null"));
                        System.out.println("  empty: " + (empty != null ? empty.toString() : "null"));
                        System.out.println("  reply: " + (reply != null ? reply.toString() : "null"));

                        ZMsg response = new ZMsg();
                        response.add(clientAddr);
                        response.add(empty);
                        response.add(reply);
                        response.send(facultades);

                        msg.destroy();
                    }
                }
                if (workersAvailable && poller.pollin(1)) {
                    //  Get client request, route to first available worker
                    ZMsg msg = ZMsg.recvMsg(facultades);
                    if (msg != null) {
                        msg.wrap(workers.remove(0));
                        msg.send(backend);
                    }
                }
            }

            //  When we're done, clean up properly
            while (workers.size() > 0) {
                ZFrame frame = workers.remove(0);
                frame.destroy();
            }

            workers.clear();
        }
    }
}
