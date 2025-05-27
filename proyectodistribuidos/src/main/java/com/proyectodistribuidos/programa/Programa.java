package com.proyectodistribuidos.programa;

import com.proyectodistribuidos.DTOs.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;

public class Programa {
    public static void main(String[] args) {
        if (args.length != 6) {
            System.out.println("Uso: java Cliente <semestre> <facultad> <programa> <numSalones> <numLaboratorios> <ip:puertoFacultad>");
            return;
        }
        try (ZContext context = new ZContext()) {
            //  Socket to talk to server
            Socket requester = context.createSocket(SocketType.REQ);
            requester.connect("tcp://" + args[5]);

            System.out.println("Cliente conectado");

            ObjectMapper objectMapper = new ObjectMapper();

            int semestre = Integer.parseInt(args[0]);
            String nomFacultad = args[1];
            String nomPrograma = args[2];
            int numSalones = Integer.parseInt(args[3]);
            int numLabo = Integer.parseInt(args[4]);
            String respuesta;

            Solicitud solicitud = new Solicitud(semestre, nomFacultad, nomPrograma, numSalones, numLabo);
            
            System.out.println("Se mandara facultad: " + nomFacultad + ", programa: " + nomPrograma + ", semestre: " + semestre + ", numero salones: "+ numSalones +", numero laboratorios: " + numLabo);
            String mensajeFacultad = objectMapper.writeValueAsString(solicitud);
            System.out.println("Mandando: " + mensajeFacultad);
            requester.send(mensajeFacultad);
            respuesta = requester.recvStr();
            System.out.println(respuesta);
        } catch (JsonProcessingException e) {
            System.err.println("Error al procesar JSON: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Error al convertir un número: " + e.getMessage());
        }
    }
    
}
