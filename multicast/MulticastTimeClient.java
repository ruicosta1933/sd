import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MulticastTimeClient {

    public static void main(String[] args) {
        // Configurações
        int serverPort = 5005;
        int bufferSize = 1024;

        try {
            // Inicia o cliente
            startClient(serverPort, bufferSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startClient(int serverPort, int bufferSize) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] buffer = new byte[bufferSize];
            DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);

            // Aguarda a solicitação do servidor
            socket.receive(requestPacket);

            // Processa a solicitação
            String requestTime = new String(requestPacket.getData(), 0, requestPacket.getLength());
            System.out.println("Pedido de tempo recebido do servidor: " + requestTime);

            // Obtém o tempo local
            long currentTime = System.currentTimeMillis();
            String responseTime = Long.toString(currentTime);

            // Envia a resposta ao servidor
            InetAddress serverAddress = requestPacket.getAddress();
            int serverPortReply = requestPacket.getPort();
            byte[] responseBuffer = responseTime.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length, serverAddress, serverPortReply);
            socket.send(responsePacket);

            System.out.println("Resposta de tempo enviada ao servidor: " + responseTime);
        }
    }
}
