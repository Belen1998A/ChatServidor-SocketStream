package chatservidor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class ChatServidor {

    public static void main(String[] args) {
        
        //Lista de clientes conectados
        ArrayList<Cliente> clientes=new ArrayList();
        
        //Entrada por teclado para introducir el puerto en el que se va a levantar el servidor
        Scanner entradaEscaner = new Scanner(System.in);
        System.out.println("Inserte puerto:");
        String puerto = entradaEscaner.nextLine();
        
        try {
            ServerSocket serverSocket=new ServerSocket(Integer.parseInt(puerto));

            //El servidor se queda levantado hasta que se mata la aplicación
            while (true) {
                if (clientes.size() <10) {
                    if(clientes.isEmpty())
                        System.out.println("Ningún cliente conectado.");

                    //Aceptamos conexiones
                    Socket newSocket = serverSocket.accept();
                    InputStream is = newSocket.getInputStream();
                    OutputStream os = newSocket.getOutputStream();

                    //Recibimos el nombre del cliente
                    byte[] recibido = new byte[250];
                    is.read(recibido);
                    String nickname = new String(recibido);
                    
                    //Añadimos un nuevo cliente a la lista y lo iniciamos
                    Cliente cliente=new Cliente(os,is,nickname,clientes);
                    cliente.start();
                    clientes.add(cliente);
                                        
                    //Informamos de la conexión de un nuevo usuario.
                    System.out.println("Nuevo cliente conectado (nickname:" + nickname + ",ip y puerto:" + newSocket.getRemoteSocketAddress() + ").");
                    System.out.println("Hay " + clientes.size() + " clientes conectados.");
                    os.write(new String("Servidor: "+nickname+" se ha unido.").getBytes());       
    
                } else {
                    System.out.println("Conexión rechazada. Servidor lleno.");
                }   
            }
        } catch (IOException ex) {
            System.out.println("Error al recibir conexiones");
        }
    }
}

/**
 * Hilo para cada cliente del servidor.
 *
 * @author dani_
 */
class Cliente extends Thread {

    OutputStream os;
    InputStream is;
    String nickname;
    ArrayList<Cliente> clientes;
    
    /**
     * Recibimos el socket de conexión con el cliente y abrimos las conexiones
     * de entrada y salida.
     *
     * @param socket socket de conexión con ek cliente
     * @throws IOException
     */
    public Cliente(OutputStream os, InputStream is, String nickname, ArrayList<Cliente> clientes){
       this.os=os;
       this.is=is;
       this.nickname=nickname;
       this.clientes=clientes;
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
                
                System.out.println("sout mensaje: "+mensaje);
                if(mensaje.equals("/bye")){
                    System.out.println("Usuario "+nickname+" se ha desconectado.");
                    clientes.remove(this);
                    for(Cliente elemento: clientes){
                        elemento.enviarMensaje("Usuario "+nickname+" se ha desconectado.");
                    }
                    os.close();
                    is.close();
                    stop();                   
                }else{
                    for(Cliente elemento: clientes){
                        elemento.enviarMensaje(nickname+": "+mensaje);
                    }
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
    
    public void enviarMensaje(String mensaje){
        try {
            os.write(mensaje.getBytes());
        } catch (IOException ex) {
            System.out.println("Error al enviar mensaje.");
            try {
                os.close();
            } catch (IOException ex1) {
                System.out.println("Error. Envío de mensajes deshabilitado.");
            }
        }
    }
}
