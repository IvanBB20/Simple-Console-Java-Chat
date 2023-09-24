import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements  Runnable{
    private ArrayList<ConnectionHandler> connectionsList;
    private ServerSocket server;

    private ExecutorService pool;
    private boolean done;
    public Server(){
         connectionsList = new ArrayList<>();
         done = false;
    }
    @Override
    public void run(){
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler current = new ConnectionHandler(client);
                connectionsList.add(current);
                pool.execute(current);
            }
        } catch (IOException e) {
            shutdown();
        }

    }

    public void broadcast(String messege){
         for(ConnectionHandler ch : connectionsList){
             if(ch != null){

                 ch.sendMessege(messege);

             }
         }
    }

    public void shutdown(){
       try {
           done = true;
           pool.shutdown();
           if (!server.isClosed()) {
               server.close();
           }
           for(ConnectionHandler ch : connectionsList){
                ch.shutdown();
           }
       } catch (IOException e){
           throw  new RuntimeException(e);
       }
    }
    class ConnectionHandler implements Runnable{
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;

        private String nickname;
        public ConnectionHandler(Socket client) {
            this.client=client;
        }
        @Override
        public void run() {
                try{
                        in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                        out = new PrintWriter(client.getOutputStream(),true);
                        out.println("Enter a nickname baby : ");
                        nickname = in.readLine();


                        System.out.println(nickname + " is connected!");
                        broadcast(nickname + " joined the chat");
                        String messege;

                        while ((messege = in.readLine()) != null){
                                if(messege.startsWith("/nick ")){
                                    //handle later
                                    String[] messegeSplit = messege.split("",2);
                                    if(messegeSplit.length == 2){
                                        broadcast(nickname + " renamed themselves to" + messegeSplit[1]);
                                        System.out.println(nickname + " renamed themselves to" + messegeSplit[1]);
                                        nickname = messegeSplit[1];
                                        out.println("Succesfully changed nickname to  " + nickname);
                                    }
                                    else{
                                        out.println("No nickname ");
                                    }
                                }
                                else if(messege.startsWith("/quit ")){

                                    broadcast(nickname + " left the chat");
                                    shutdown();
                                }
                                else{
                                    broadcast(nickname+ ": " + messege);
                                }
                        }
                }catch (IOException e){
                  shutdown();
                }
        }

        public void sendMessege(String messege){
            out.println(messege);
        }

        public void shutdown(){
            try {

                in.close();
                out.close();

                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e){
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
         Server server = new Server();
         server.run();
    }
}
