package com.proyectodistribuidos.programa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectodistribuidos.DTOs.*;

import org.zeromq.ZMQ;

public class CienciasNaturales {
    public static void main(String[] args) throws InterruptedException {
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket requester = context.socket(ZMQ.REQ);
        if (!requester.connect("tcp://172.23.172.13:5555"))
            System.out.println("No se pudo conectar al servidor.");
        else
            System.out.println("Conectado al servidor.");
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            for(int i = 0; i < 20; i++){
                
                int semestre =  1;
                String nomFacultad = "Facultad de Ciencias Naturales";
                String nomPrograma = switch ((int)(Math.random() * 3)) {
                    case 0 -> "Biología";
                    case 1 -> "Química";
                    case 2 -> "Física";
                    default -> "Matemáticas";
                };
                int numSalones = (int)(Math.random() * 4) + 7;
                int numLabo = (int)(Math.random() * 3) + 2;
                String respuesta;

                Solicitud solicitud = new Solicitud(semestre, nomFacultad, nomPrograma, numSalones, numLabo);
            
                System.out.println("Se mandara facultad: " + nomFacultad + ", programa: " + nomPrograma + ", semestre: " + semestre + ", numero salones: "+ numSalones +", numero laboratorios: " + numLabo);
                String mensajeFacultad = objectMapper.writeValueAsString(solicitud);
                System.out.println("Mandando: " + mensajeFacultad);
                requester.send(mensajeFacultad);
                respuesta = requester.recvStr();
                System.out.println(respuesta);

                Thread.sleep(1000); // Espera 1 segundo entre solicitudes
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error al procesar JSON: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Error al convertir un número: " + e.getMessage());
        } finally {
            requester.close();
            context.term();
        }
    }
    
}
