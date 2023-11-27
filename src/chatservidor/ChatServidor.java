package chatservidor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.Semaphore;


public class ChatServidor {
    
    //Lista de clientes conectados
    static ArrayList<Cliente> clientes=new ArrayList();

    // Semáforo para sincronizar la emisión y recepción de mensajes
    static Semaphore semaforo = new Semaphore(1);

    public static void main(String[] args) {
        
        //Entrada por teclado para introducir el puerto en el que se va a levantar el servidor
        Scanner entradaEscaner = new Scanner(System.in);
        System.out.println("Inserte puerto:");
        String puerto = entradaEscaner.nextLine();
        
        try {
            ServerSocket serverSocket=new ServerSocket(Integer.parseInt(puerto));

            //El servidor se queda levantado hasta que se mata la aplicación
            while (true) {
                Thread.sleep(200);
                if (clientes.size() <2) {
                    if(clientes.isEmpty())
                        System.out.println("Ningún cliente conectado.");
                    
                    System.out.println("Conexión abierta.");
                    //Aceptamos conexiones
                    Socket newSocket = serverSocket.accept();
                    InputStream is = newSocket.getInputStream();
                    OutputStream os = newSocket.getOutputStream();

                    //Recibimos el nombre del cliente
                    byte[] recibido = new byte[250];
                    is.read(recibido);
                    String nickname = new String(recibido);
                    
                    //Añadimos un nuevo cliente a la lista y lo iniciamos
                    Cliente cliente=new Cliente(os,is,nickname);
                    cliente.start();
                    clientes.add(cliente);
                                        
                    //Informamos de la conexión de un nuevo usuario.
                    System.out.println("Nuevo cliente conectado (nickname:" + nickname + ",ip y puerto:" + newSocket.getRemoteSocketAddress() + ").");
                    System.out.println("Hay " + clientes.size() + " clientes conectados.");
                    
                    clientes.forEach((elemento) -> {
                        elemento.enviarMensaje("Servidor: "+nickname+" se ha unido.");
                    });
                                        
                    if(clientes.size()==2)
                        System.out.println("Servidor lleno hasta que se desconecte un usuario.");
                }   
            }
        } catch (IOException ex) {
            System.out.println("Error al recibir conexiones");
        } catch (InterruptedException ex) {
            Logger.getLogger(ChatServidor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

/**
 * Hilo para cada cliente del servidor.
 *
 * @author 
 */
class Cliente extends Thread {

    OutputStream os;
    InputStream is;
    String nickname;
    
    /**
     * Recibimos el socket de conexión con el cliente y abrimos las conexiones
     * de entrada y salida.
     *
     * @param socket socket de conexión con ek cliente
     * @throws IOException
     */
    public Cliente(OutputStream os, InputStream is, String nickname){
       this.os=os;
       this.is=is;
       this.nickname=nickname;
    }

    /**
     * Ejecución del hilo.
     *
     */
    @Override
    public void run() {
        while(true){
            try {
                //Recibimos mensajes
                byte[] recibido = new byte[250];
                is.read(recibido);
                String mensaje = new String(recibido);
                //Si llega "/bye" se cierra la conexión
                if(mensaje.contains("/bye")){
                    System.out.println("Usuario "+nickname+" se ha desconectado.");
                    ChatServidor.clientes.remove(this);
                    System.out.println("Hay "+ChatServidor.clientes.size()+" conectados");
                    ChatServidor.clientes.forEach((elemento) -> {
                        elemento.enviarMensaje("Usuario "+nickname+" se ha desconectado.");
                    });
                    if(ChatServidor.clientes.isEmpty())
                        System.out.println("Ningún cliente conectado.");
                    os.close();
                    is.close();
                    stop();                   
                }else{
                    ChatServidor.clientes.forEach((elemento) -> {
                        elemento.enviarMensaje(nickname+": "+mensaje);
                    });
                    System.out.println(nickname+": "+mensaje);
                }
                
            } catch (IOException ex) {
                try {
                    System.out.println("Error al recibir mensajes.");
                    os.close();
                    is.close();
                    stop();
                } catch (IOException ex1) {
                    System.out.println("Error al cerrar conexiones.");
                }
            }
        }
    }
    
    // Método para que al cliente le lleguen los mensajes de todos los usuarios.
    public void enviarMensaje(String mensaje){
        try {
            ChatServidor.semaforo.acquire();
            os.write(mensaje.getBytes());
            ChatServidor.semaforo.release();
        } catch (IOException ex) {
            System.out.println("Error al enviar mensaje.");
            try {
                os.close();
            } catch (IOException ex1) {
                System.out.println("Error. Envío de mensajes deshabilitado.");
            }
        } catch (InterruptedException ex) {
            System.out.println("Error al adquirir el semáforo.");
        }
    }
}
